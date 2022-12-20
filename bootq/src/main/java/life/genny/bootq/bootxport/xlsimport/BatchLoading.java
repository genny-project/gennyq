package life.genny.bootq.bootxport.xlsimport;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.NoResultException;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.bootq.bootxport.bootx.QwandaRepository;
import life.genny.bootq.bootxport.bootx.RealmUnit;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.EntityEntity;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.utils.KeycloakUtils;
import life.genny.qwandaq.validation.Validation;
import life.genny.qwandaq.validation.ValidationList;

public class BatchLoading {

    private QwandaRepository service;

    private String mainRealm = "genny";

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().getClass());

    public BatchLoading(QwandaRepository repo) {
        this.service = repo;
    }

    private Boolean getBooleanFromString(final String booleanString) {
        if (booleanString == null) {
            return false;
        }

        return "TRUE".equalsIgnoreCase(booleanString.toUpperCase()) || "YES".equalsIgnoreCase(booleanString.toUpperCase())
                || "T".equalsIgnoreCase(booleanString.toUpperCase())
                || "Y".equalsIgnoreCase(booleanString.toUpperCase()) || "1".equalsIgnoreCase(booleanString);

    }

    public Map<String, DataType> dataType(Map<String, Map<String, String>> project) {
        final Map<String, DataType> dataTypeMap = new HashMap<>();
        project.entrySet().stream().filter(d -> !d.getKey().matches("\\s*")).forEach(data -> {
            Map<String, String> dataType = data.getValue();
            String validations = dataType.get("validations");
            String code = (dataType.get("code")).trim().replaceAll("^\"|\"$", "");
            String className = (dataType.get("classname")).replaceAll("^\"|\"$", "");
            String name = (dataType.get("name")).replaceAll("^\"|\"$", "");
            String inputmask = dataType.get("inputmask");
            String component = dataType.get("component");
            final ValidationList validationList = new ValidationList();
            validationList.setValidationList(new ArrayList<Validation>());
            if (validations != null) {
                final String[] validationListStr = validations.split(",");
                for (final String validationCode : validationListStr) {
                    try {
                        Validation validation = service.findValidationByCode(validationCode);
                        validationList.getValidationList().add(validation);
                    } catch (NoResultException e) {
                        log.error("Could not load Validation " + validationCode);
                    }
                }
            }
            if (!dataTypeMap.containsKey(code)) {
                DataType dataTypeRecord;
                if (component == null) {
                    dataTypeRecord = new DataType(className, validationList, name, inputmask);
                } else {
                    dataTypeRecord = new DataType(className, validationList, name, inputmask, component);
                }
                dataTypeRecord.setDttCode(code);
                dataTypeMap.put(code, dataTypeRecord);
            }
        });
        return dataTypeMap;
    }

    private String getNameFromMap(Map<String, String> baseEntitys, String key, String defaultString) {
        String ret = defaultString;
        if (baseEntitys.containsKey(key)) {
            if (baseEntitys.get("name") != null) {
                ret = ((String) baseEntitys.get("name")).replaceAll("^\"|\"$", "");
            }
        }
        return ret;
    }

    public void upsertKeycloakJson(String keycloakJson) {
        final String PROJECT_CODE = "PRJ_" + this.mainRealm.toUpperCase();
        BaseEntity be = service.findBaseEntityByCode(PROJECT_CODE);

        ValidatorFactory factory = javax.validation.Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Attribute attr = service.findAttributeByCode("ENV_KEYCLOAK_JSON");
        if (attr == null) {
            DataType dataType = new DataType("DTT_TEXT");
            dataType.setDttCode("DTT_TEXT");
            attr = new Attribute("ENV_KEYCLOAK_JSON", "Keycloak Json", dataType);
            attr.setRealm(mainRealm);
            Set<ConstraintViolation<Attribute>> constraints = validator.validate(attr);
            for (ConstraintViolation<Attribute> constraint : constraints) {
                log.info(String.format("[\"%s\"], %s, %s.", this.mainRealm,
                        constraint.getPropertyPath(), constraint.getMessage()));
            }
            service.upsert(attr);
        }
        try {
            be.addAttribute(attr, 0.0, keycloakJson);
        } catch (BadDataException e) {
            log.error(String.format("BadDataException:%s", e.getMessage()));
        }

        service.updateWithAttributes(be);

    }

    private Attribute getUrlListAttr() {
        String attrName = "Url List";
        String attrCode = "ENV_URL_LIST";
        String dttCode = "DTT_TEXT";

        ValidatorFactory factory = javax.validation.Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        Attribute attr = service.findAttributeByCode(attrCode);
        if (attr == null) {
            DataType dataType = new DataType(dttCode);
            dataType.setDttCode(dttCode);
            attr = new Attribute(attrCode, attrName, dataType);
        } else {
            attr.setName(attrName);
        }
        attr.setRealm(mainRealm);
        Set<ConstraintViolation<Attribute>> constraints = validator.validate(attr);
        for (ConstraintViolation<Attribute> constraint : constraints) {
            log.info(String.format("[\" %s\"] %s, %s.", this.mainRealm, constraint.getPropertyPath(), constraint.getMessage()));
        }
        return attr;
    }

    public void upsertProjectUrls(String urlList) {

        final String PROJECT_CODE = "PRJ_" + this.mainRealm.toUpperCase();
        BaseEntity be = service.findBaseEntityByCode(PROJECT_CODE);

        Attribute attr = getUrlListAttr();
        service.upsert(attr);

        try {
            be.addAttribute(attr, 0.0, urlList);
        } catch (BadDataException e) {
            log.error(String.format("BadDataException:%s", e.getMessage()));
        }
        service.updateWithAttributes(be);
    }

    public String constructKeycloakJson(final RealmUnit realm) {
        this.mainRealm = realm.getCode();
        String keycloakUrl = null;
        String keycloakSecret = null;
        String keycloakJson = null;

        keycloakUrl = realm.getKeycloakUrl();
        keycloakSecret = realm.getClientSecret();

        keycloakJson = "{\n" + "  \"realm\": \"" + this.mainRealm + "\",\n" + "  \"auth-server-url\": \"" + keycloakUrl
                + "/auth\",\n" + "  \"ssl-required\": \"external\",\n" + "  \"resource\": \"" + this.mainRealm + "\",\n"
                + "  \"credentials\": {\n" + "    \"secret\": \"" + keycloakSecret + "\" \n" + "  },\n"
                + "  \"policy-enforcer\": {}\n" + "}";

        log.info(String.format("[%s] Loaded keycloak.json:%s ", this.mainRealm, keycloakJson));
        return keycloakJson;

    }

    public void persistProject(RealmUnit rx) {
        persistProjectOptimization(rx);
    }

    private String decodePassword(String realm, String securityKey, String servicePass) {
        String initVector = "PRJ_" + realm.toUpperCase();
        initVector = StringUtils.rightPad(initVector, 16, '*');
        String decrypt = SecurityUtils.decrypt(securityKey, initVector, servicePass);
        return decrypt;
    }


    public void persistProjectOptimization(RealmUnit rx) {
        service.setRealm(rx.getCode());

        String decrypt = decodePassword(rx.getCode(), rx.getSecurityKey(), rx.getServicePassword());

        String debugStr = "Time profile";
        Instant start = Instant.now();
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info(debugStr + " Finished get user from keycloak, cost:" + timeElapsed.toMillis() + " millSeconds.");

        Optimization optimization = new Optimization(service);

        start = Instant.now();
        optimization.validationsOptimization(rx.getValidations(), rx.getCode());
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info(debugStr + " Finished validations, cost:" + timeElapsed.toMillis() + " millSeconds.");

        Map<String, DataType> dataTypes = dataType(rx.getDataTypes());

        start = Instant.now();
        optimization.attributesOptimization(rx.getAttributes(), dataTypes, rx.getCode());
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info(debugStr + " Finished attribute, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        optimization.def_baseEntitysOptimization(rx.getDef_baseEntitys(), rx.getCode());
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info(debugStr + " Finished def_baseentity, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        optimization.def_baseEntityAttributesOptimization(rx.getDef_entityAttributes(), rx.getCode());
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info(debugStr + " Finished def_baseentity_attribute, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        optimization.baseEntitysOptimization(rx.getBaseEntitys(), rx.getCode());
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info(debugStr + " Finished baseentity, cost:" + timeElapsed.toMillis() + " millSeconds.");

        optimization.attributeLinksOptimization(rx.getAttributeLinks(), dataTypes, rx.getCode());

        start = Instant.now();
        optimization.baseEntityAttributesOptimization(rx.getEntityAttributes(), rx.getCode());
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info(debugStr + " Finished baseentity_attribute, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        optimization.entityEntitysOptimization(rx.getEntityEntitys(), rx.getCode());
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info(debugStr + " Finished entity_entity, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        optimization.questionsOptimization(rx.getQuestions(), rx.getCode());
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info(debugStr + " Finished question, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        optimization.questionQuestionsOptimization(rx.getQuestionQuestions(), rx.getCode());
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info(debugStr + " Finished question_question, cost:" + timeElapsed.toMillis() + " millSeconds.");

        optimization.asksOptimization(rx.getAsks(), rx.getCode());

        optimization.messageTemplatesOptimization(rx.getNotifications(), rx.getCode());
        optimization.messageTemplatesOptimization(rx.getMessages(), rx.getCode());
    }

    public void deleteFromProject(life.genny.bootxport.bootx.RealmUnit rx) {
        service.setRealm(rx.getCode());
        deleteAttributes(rx.getAttributes());
        deleteBaseEntitys(rx.getBaseEntitys());
        deleteAttributeLinks(rx.getAttributeLinks());
        deleteEntityEntitys(rx.getEntityEntitys());
        deleteQuestions(rx.getQuestions());
        deleteQuestionQuestions(rx.getQuestionQuestions());
        deleteMessageTemplates(rx.getNotifications());
        deleteMessageTemplates(rx.getMessages());
    }

    public void deleteAttributes(Map<String, Map<String, String>> project) {
        project.entrySet().stream().forEach(d -> {
            Attribute attribute = service.findAttributeByCode(d.getKey());
            service.delete(attribute);
        });
    }

    public void deleteBaseEntitys(Map<String, Map<String, String>> project) {
        project.entrySet().stream().forEach(d -> {
            BaseEntity baseEntity = service.findBaseEntityByCode(d.getKey());
            service.delete(baseEntity);
        });

    }

    public void deleteAttributeLinks(Map<String, Map<String, String>> project) {
        project.entrySet().stream().forEach(d -> {
            Attribute attribute = service.findAttributeByCode(d.getKey());
            service.delete(attribute);
        });

    }

    public void deleteEntityEntitys(Map<String, Map<String, String>> project) {
        project.entrySet().stream().forEach(d -> {
            Map<String, String> entEnts = d.getValue();
            String parentCode = entEnts.get("parentCode".toLowerCase().replaceAll("^\"|\"$|_|-", ""));

            String linkCode = entEnts.get("linkCode".toLowerCase().replaceAll("^\"|\"$|_|-", ""));
            if (parentCode == null)
                parentCode = entEnts.get("sourceCode".toLowerCase().replaceAll("^\"|\"$|_|-", ""));

            String targetCode = entEnts.get("targetCode".toLowerCase().replaceAll("^\"|\"$|_|-", ""));

            EntityEntity entityEntity = service.findEntityEntity(parentCode, targetCode, linkCode);
            service.delete(entityEntity);

        });
    }

    public void deleteQuestions(Map<String, Map<String, String>> project) {
        project.entrySet().stream().forEach(d -> {
            Question question = service.findQuestionByCode(d.getKey());
            service.delete(question);
        });
    }

    public void deleteQuestionQuestions(Map<String, Map<String, String>> project) {
        project.entrySet().stream().forEach(d -> {
            Map<String, String> queQues = d.getValue();
            String parentCode = queQues.get("parentCode".toLowerCase().replaceAll("^\"|\"$|_|-", ""));
            String targetCode = queQues.get("targetCode".toLowerCase().replaceAll("^\"|\"$|_|-", ""));
            QuestionQuestion questionQuestion = service.findQuestionQuestionByCode(parentCode, targetCode);
            service.delete(questionQuestion);
        });

    }

    public void deleteMessageTemplates(Map<String, Map<String, String>> project) {
        project.entrySet().stream().forEach(d -> {
            QBaseMSGMessageTemplate template = service.findTemplateByCode(d.getKey());
            service.delete(template);
        });
    }

}
