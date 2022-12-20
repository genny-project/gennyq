package life.genny.bootq.bootxport.xlsimport;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.json.JSONException;

import life.genny.bootq.bootxport.bootx.DEFBaseentityAttribute;
import life.genny.bootq.bootxport.bootx.QwandaRepository;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.CodedEntity;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.EntityEntity;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.utils.KeycloakUtils;

public class Optimization {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().getClass());

    private QwandaRepository service;

    private static final String LNK_INCLUDE = "LNK_INCLUDE";
    private static final String DEF_PREFIX= "DEF_";
    private static final String ATT_PREFIX= "ATT_";
    private static final String SER_PREFIX= "SER_";
    private static final String DFT_PREFIX= "DFT_";
    private static final String DEP_PREFIX= "DEP_";

    private static final Map<String, String> defPrefixDataTypeMapping =

    Map.of(ATT_PREFIX,"DTT_BOOLEAN",
    SER_PREFIX, "DTT_JSON",
    DFT_PREFIX, "DTT_TEXT",
    DEP_PREFIX, "DTT_TEXT");

    String debugStr = "Time profile";

    public Optimization(QwandaRepository repo) {
        this.service = repo;
    }

    private void printSummary(String tableName, int total, int invalid, int skipped, int updated, int newItem) {
        log.info(String.format("Table:%s: Total:%d, invalid:%d, skipped:%d, updated:%d, new item:%d.",
                tableName, total, invalid, skipped, updated, newItem));
    }

    private boolean isValid(CodedEntity t) {
        if (t == null) return false;

        ValidatorFactory factory = javax.validation.Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<CodedEntity>> constraints = validator.validate(t);
        for (ConstraintViolation<CodedEntity> constraint : constraints) {
            log.error(String.format("Validates constraints failure, Code:%s, PropertyPath:%s,Error:%s.",
                    t.getCode(), constraint.getPropertyPath(), constraint.getMessage()));
        }
        return constraints.isEmpty();
    }

    public void attributesOptimization(Map<String, Map<String, String>> project,
                                       Map<String, DataType> dataTypeMap, String realmName) {
        String tableName = "Attribute";
        List<Attribute> attributesFromDB = service.queryTableByRealm(tableName, realmName);

        HashMap<String, CodedEntity> codeAttributeMapping = new HashMap<>();

        for (Attribute attr : attributesFromDB) {
            codeAttributeMapping.put(attr.getCode(), attr);
        }

        ArrayList<CodedEntity> attributeInsertList = new ArrayList<>();
        ArrayList<CodedEntity> attributeUpdateList = new ArrayList<>();
        int invalid = 0;
        int total = 0;
        int skipped = 0;
        int newItem = 0;
        int updated = 0;

        for (Map.Entry<String, Map<String, String>> data : project.entrySet()) {
            total += 1;
            Map<String, String> attributes = data.getValue();
            String code = attributes.get("code");
            if(code == null) {
                log.error("Failed to get code for attribute: " + total + " in realm: " + realmName);
                continue;
            }
            code = code.replaceAll("^\"|\"$", "");

            Attribute attr = GoogleSheetBuilder.buildAttrribute(attributes, dataTypeMap, realmName, code);

            // validation check
            if (isValid(attr)) {
                if (codeAttributeMapping.containsKey(code.toUpperCase())) {
					attributeUpdateList.add(attr);
					updated++;
                } else {
                    // insert new item
                    attributeInsertList.add(attr);
                    newItem++;
                }
            } else {
                invalid++;
            }
        }

        service.bulkInsert(attributeInsertList);
        service.bulkUpdate(attributeUpdateList, codeAttributeMapping);
        printSummary(tableName, total, invalid, skipped, updated, newItem);
        attributesFromDB = null;
        codeAttributeMapping = null;
        attributeInsertList = null;
        attributeUpdateList = null;
    }

    public void baseEntityAttributesOptimization(Map<String, Map<String, String>> project, String realmName,
                                                 HashMap<String, String> userCodeUUIDMapping) {
        // Get all BaseEntity
        String tableName = "BaseEntity";
        List<BaseEntity> baseEntityFromDB = service.queryTableByRealm(tableName, realmName);
        HashMap<String, BaseEntity> beHashMap = new HashMap<>();
        for (BaseEntity be : baseEntityFromDB) {
            beHashMap.put(be.getCode(), be);
        }

        // Get all Attribute
        tableName = "Attribute";
        List<Attribute> attributeFromDB = service.queryTableByRealm(tableName, realmName);
        HashMap<String, Attribute> attrHashMap = new HashMap<>();
        for (Attribute attribute : attributeFromDB) {
            attrHashMap.put(attribute.getCode(), attribute);
        }

        int invalid = 0;
        int total = 0;
        int skipped = 0;
        int newItem = 0;
        int updated = 0;
        Set<String> entityCodes= new HashSet<>();

        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
            total++;
            Map<String, String> baseEntityAttr = entry.getValue();

            String baseEntityCode = GoogleSheetBuilder.getBaseEntityCodeFromBaseEntityAttribute(baseEntityAttr);
            if (baseEntityCode == null) {
                invalid++;
                continue;
            }
            String attributeCode = GoogleSheetBuilder.getAttributeCodeFromBaseEntityAttribute(baseEntityAttr);
            if (attributeCode == null) {
                invalid++;
                continue;
            }

            BaseEntity be = GoogleSheetBuilder.buildEntityAttribute(baseEntityAttr, realmName, attrHashMap, beHashMap,
                    userCodeUUIDMapping);
            if (be != null) {
                // update Baseentity in entity hash map
                beHashMap.put(be.getCode(), be);
                entityCodes.add(be.getCode());
                newItem++;
            } else {
                invalid++;
            }
        }

        for(String beCode: entityCodes) {
            service.updateWithAttributes(beHashMap.get(beCode));
        }
        printSummary("BaseEntityAttributes", total, invalid, skipped, updated, newItem);
        baseEntityFromDB = null;
        attributeFromDB = null;
        beHashMap = null;
        attrHashMap = null;
    }

    public void baseEntitysOptimization(Map<String, Map<String, String>> project, String realmName,
                                        HashMap<String, String> userCodeUUIDMapping) {
        String tableName = "BaseEntity";

        Instant start = Instant.now();
        List<BaseEntity> baseEntityFromDB = service.queryTableByRealm(tableName, realmName);
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info(debugStr + " Finished query table:" + tableName + ", cost:" + timeElapsed.toMillis() + " millSeconds.");

        HashMap<String, CodedEntity> codeBaseEntityMapping = new HashMap<>();

        for (BaseEntity be : baseEntityFromDB) {
            codeBaseEntityMapping.put(be.getCode(), be);
        }

        ArrayList<CodedEntity> baseEntityInsertList = new ArrayList<>();
        ArrayList<CodedEntity> baseEntityUpdateList = new ArrayList<>();
        int invalid = 0;
        int total = 0;
        int skipped = 0;
        int newItem = 0;
        int updated = 0;

        start = Instant.now();
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
            total += 1;
            Map<String, String> baseEntitys = entry.getValue();
            BaseEntity baseEntity = GoogleSheetBuilder.buildBaseEntity(baseEntitys, realmName);
            // validation check
            String baseentityCode ;
            if (isValid(baseEntity)) {
                // get keycloak uuid from keycloak, update code
                baseentityCode = baseEntity.getCode();
                if (baseentityCode.startsWith("PER_")) {
                    String keycloakUUID = KeycloakUtils.getKeycloakUUIDByUserCode(baseentityCode, userCodeUUIDMapping);
                    baseEntity.setCode(keycloakUUID);
                    // assign new value
                    baseentityCode = keycloakUUID;
                }

                if (codeBaseEntityMapping.containsKey(baseentityCode)) {
                    if (isChanged(baseEntity, codeBaseEntityMapping.get(baseentityCode))) {
                        baseEntityUpdateList.add(baseEntity);
                        updated++;
                    } else {
                        skipped++;
                    }
                } else {
                    // insert new item
                    baseEntityInsertList.add(baseEntity);
                    newItem++;
                }
            } else {
                invalid++;
            }
        }
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info(debugStr + " Finished for loop, type:" + tableName + ", cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        service.bulkInsert(baseEntityInsertList);
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info(debugStr + " Finished bulk insert, type:" + tableName + ", cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        service.bulkUpdate(baseEntityUpdateList, codeBaseEntityMapping);
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info(debugStr + " Finished bulk update, type:" + tableName + ", cost:" + timeElapsed.toMillis() + " millSeconds.");

        printSummary(tableName, total, invalid, skipped, updated, newItem);
        baseEntityFromDB = null;
        codeBaseEntityMapping = null;
        baseEntityInsertList = null;
        baseEntityUpdateList = null;
    }

    public void entityEntitysOptimization(Map<String, Map<String, String>> project, String realmName,
                                          boolean isSynchronise, HashMap<String, String> userCodeUUIDMapping) {
        // Get all BaseEntity
        String tableName = "BaseEntity";
        List<BaseEntity> baseEntityFromDB = service.queryTableByRealm(tableName, realmName);
        HashMap<String, BaseEntity> beHashMap = new HashMap<>();
        for (BaseEntity be : baseEntityFromDB) {
            beHashMap.put(be.getCode(), be);
        }

        // Get all Attribute
        tableName = "Attribute";
        List<Attribute> attributeFromDB = service.queryTableByRealm(tableName, realmName);
        HashMap<String, Attribute> attrHashMap = new HashMap<>();
        for (Attribute attribute : attributeFromDB) {
            attrHashMap.put(attribute.getCode(), attribute);
        }

        tableName = "EntityEntity";
        List<EntityEntity> entityEntityFromDB = service.queryTableByRealm(tableName, realmName);

        HashMap<String, EntityEntity> codeBaseEntityEntityMapping = new HashMap<>();
        for (EntityEntity entityEntity : entityEntityFromDB) {
            String beCode = entityEntity.getPk().getSource().getCode();
            String attrCode = entityEntity.getPk().getAttribute().getCode();
            String targetCode = entityEntity.getPk().getTargetCode();
            if (targetCode.toUpperCase().startsWith("PER_")) {
               targetCode = KeycloakUtils.getKeycloakUUIDByUserCode(targetCode.toUpperCase(), userCodeUUIDMapping);
            }
            String uniqueCode = beCode + "-" + attrCode + "-" + targetCode;
            codeBaseEntityEntityMapping.put(uniqueCode, entityEntity);
        }

        int invalid = 0;
        int total = 0;
        int skipped = 0;
        int newItem = 0;
        int updated = 0;

        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
            total++;
            Map<String, String> entEnts = entry.getValue();
            String linkCode = entEnts.get("linkCode".toLowerCase().replaceAll("^\"|\"$|_|-", ""));

            if (linkCode == null)
                linkCode = entEnts.get("code".toLowerCase().replaceAll("^\"|\"$|_|-", ""));

            String parentCode = entEnts.get("parentCode".toLowerCase().replaceAll("^\"|\"$|_|-", ""));
            if (parentCode == null)
                parentCode = entEnts.get("sourceCode".toLowerCase().replaceAll("^\"|\"$|_|-", ""));

            String targetCode = entEnts.get("targetCode".toLowerCase().replaceAll("^\"|\"$|_|-", ""));
            if (targetCode.toUpperCase().startsWith("PER_")) {
                targetCode = KeycloakUtils.getKeycloakUUIDByUserCode(targetCode.toUpperCase(), userCodeUUIDMapping);
            }

            String weightStr = entEnts.get("weight");
            String valueString = entEnts.get("valueString".toLowerCase().replaceAll("^\"|\"$|_|-", "").replaceAll("\n", ""));
            Optional<String> weightStrOpt = Optional.ofNullable(weightStr);
            final Double weight = weightStrOpt.filter(d -> !d.equals(" ")).map(Double::valueOf).orElse(0.0);

            Attribute linkAttribute = attrHashMap.get(linkCode.toUpperCase());
            BaseEntity sbe = beHashMap.get(parentCode.toUpperCase());
            BaseEntity tbe = beHashMap.get(targetCode.toUpperCase());
            if (linkAttribute == null) {
                log.error("EntityEntity Link code:" + linkCode + " doesn't exist in Attribute table.");
                invalid++;
                continue;
            } else if (sbe == null) {
                log.error("EntityEntity parent code:" + parentCode + " doesn't exist in BaseEntity table.");
                invalid++;
                continue;
            } else if (tbe == null) {
                log.error("EntityEntity target Code:" + targetCode + " doesn't exist in BaseEntity table.");
                invalid++;
                continue;
            }

            String code = parentCode + "-" + linkCode + "-" + targetCode;
            if (isSynchronise) {
                if (codeBaseEntityEntityMapping.containsKey(code.toUpperCase())) {
                    EntityEntity ee = codeBaseEntityEntityMapping.get(code.toUpperCase());
                    ee.setWeight(weight);
                    ee.setValueString(valueString);
                    service.updateEntityEntity(ee);
                    updated++;
                } else {
                    EntityEntity ee = new EntityEntity(sbe, tbe, linkAttribute, weight);
                    ee.setValueString(valueString);
                    service.insertEntityEntity(ee);
                    newItem++;
                }
            } else {
                try {
                    sbe.addTarget(tbe, linkAttribute, weight, valueString);
                    service.updateWithAttributes(sbe);
                    newItem++;
                } catch (BadDataException be) {
                    log.error(String.format("Should never reach here!, BaseEntity:%s, Attribute:%s ", tbe.getCode(), linkAttribute.getCode()));
                }
            }
        }
        printSummary("EntityEntity", total, invalid, skipped, updated, newItem);
        baseEntityFromDB = null;
        beHashMap = null;
        attributeFromDB = null;
        attrHashMap = null;
        entityEntityFromDB = null;
        codeBaseEntityEntityMapping = null;
    }

    public void messageTemplatesOptimization(Map<String, Map<String, String>> project, String realmName) {
        String tableName = "QBaseMSGMessageTemplate";
        List<QBaseMSGMessageTemplate> qBaseMSGMessageTemplateFromDB = service.queryTableByRealm(tableName, realmName);

        HashMap<String, CodedEntity> codeMsgMapping = new HashMap<>();
        for (QBaseMSGMessageTemplate message : qBaseMSGMessageTemplateFromDB) {
            codeMsgMapping.put(message.getCode(), message);
        }

        ArrayList<CodedEntity> messageInsertList = new ArrayList<>();
        ArrayList<CodedEntity> messageUpdateList = new ArrayList<>();
        int invalid = 0;
        int total = 0;
        int skipped = 0;
        int newItem = 0;
        int updated = 0;

        for (Map.Entry<String, Map<String, String>> data : project.entrySet()) {
            total += 1;
            Map<String, String> template = data.getValue();
            String code = template.get("code");
            String name = template.get("name");
            if (StringUtils.isBlank(name)) {
                log.error("Templates:" + code + "has EMPTY name.");
                invalid += 1;
                continue;
            }

            QBaseMSGMessageTemplate msg = GoogleSheetBuilder.buildQBaseMSGMessageTemplate(template, realmName);
            if (codeMsgMapping.containsKey(code.toUpperCase())) {
                if (isChanged(msg, codeMsgMapping.get(code.toUpperCase()))) {
                    messageUpdateList.add(msg);
                    updated++;
                } else {
                    skipped++;
                }
            } else {
                // insert new item
                messageInsertList.add(msg);
                newItem++;
            }
        }
        service.bulkInsert(messageInsertList);
        service.bulkUpdate(messageUpdateList, codeMsgMapping);
        printSummary(tableName, total, invalid, skipped, updated, newItem);
        qBaseMSGMessageTemplateFromDB = null;
        codeMsgMapping = null;
    }

    public void questionQuestionsOptimization(Map<String, Map<String, String>> project, String realmName) {
        String tableName = "Question";
        List<Question> questionFromDB = service.queryTableByRealm(tableName, realmName);
        HashSet<String> questionCodeSet = new HashSet<>();
        HashMap<String, Question> questionHashMap = new HashMap<>();

        for (Question question : questionFromDB) {
            questionCodeSet.add(question.getCode());
            questionHashMap.put(question.getCode(), question);
        }

        tableName = "QuestionQuestion";
        List<QuestionQuestion> questionQuestionFromDB = service.queryTableByRealm(tableName, realmName);

        HashMap<String, QuestionQuestion> codeQuestionMapping = new HashMap<>();

        for (QuestionQuestion qq : questionQuestionFromDB) {
            String sourceCode = qq.getSourceCode();
            String targetCode = qq.getTarketCode();
            String uniqCode = sourceCode + "-" + targetCode;
            codeQuestionMapping.put(uniqCode, qq);
        }

        ArrayList<QuestionQuestion> questionQuestionInsertList = new ArrayList<>();
        ArrayList<QuestionQuestion> questionQuestionUpdateList = new ArrayList<>();
        int invalid = 0;
        int total = 0;
        int skipped = 0;
        int newItem = 0;
        int updated = 0;

        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
            total += 1;
            Map<String, String> queQues = entry.getValue();

            QuestionQuestion qq = GoogleSheetBuilder.buildQuestionQuestion(queQues, realmName, questionHashMap);
            if (qq == null) {
                invalid++;
                continue;
            }

            String parentCode = queQues.get("parentCode".toLowerCase().replaceAll("^\"|\"$|_|-", ""));
            if (parentCode == null) {
                parentCode = queQues.get("sourceCode".toLowerCase().replaceAll("^\"|\"$|_|-", ""));
            }

            String targetCode = queQues.get("targetCode".toLowerCase().replaceAll("^\"|\"$|_|-", ""));

            String uniqueCode = parentCode + "-" + targetCode;
            if (codeQuestionMapping.containsKey(uniqueCode.toUpperCase())) {
                if (isChanged(qq, codeQuestionMapping.get(uniqueCode.toUpperCase()))) {
                    questionQuestionUpdateList.add(qq);
                    updated++;
                } else {
                    skipped++;
                }
            } else {
                // insert new item
                questionQuestionInsertList.add(qq);
                newItem++;
            }
        }
        service.bulkInsertQuestionQuestion(questionQuestionInsertList);
        service.bulkUpdateQuestionQuestion(questionQuestionUpdateList, codeQuestionMapping);
        printSummary("QuestionQuestion", total, invalid, skipped, updated, newItem);
        questionFromDB = null;
        questionHashMap = null;
        questionQuestionFromDB = null;
        codeQuestionMapping = null;
        questionQuestionInsertList = null;
        questionQuestionUpdateList = null;
    }

    public void questionsOptimization(Map<String, Map<String, String>> project, String realmName, boolean isSynchronise) {
        // Get all questions from database
        String tableName = "Question";
        String mainRealm = GennySettings.mainrealm;
        List<Question> questionsFromDBMainRealm = new ArrayList<>();
        HashMap<String, Question> codeQuestionMappingMainRealm = new HashMap<>();

        if (!realmName.equals(mainRealm)) {
            questionsFromDBMainRealm = service.queryTableByRealm(tableName, mainRealm);
            for (Question q : questionsFromDBMainRealm) {
                codeQuestionMappingMainRealm.put(q.getCode(), q);
            }
        }

        List<Question> questionsFromDB = service.queryTableByRealm(tableName, realmName);
        HashMap<String, Question> codeQuestionMapping = new HashMap<>();

        for (Question q : questionsFromDB) {
            codeQuestionMapping.put(q.getCode(), q);
        }

        // Get all Attributes from database
        tableName = "Attribute";
        List<Attribute> attributesFromDB = service.queryTableByRealm(tableName, realmName);
        HashMap<String, Attribute> attributeHashMap = new HashMap<>();

        for (Attribute attribute : attributesFromDB) {
            attributeHashMap.put(attribute.getCode(), attribute);
        }

        int invalid = 0;
        int total = 0;
        int skipped = 0;
        int newItem = 0;
        int updated = 0;

        for (Map.Entry<String, Map<String, String>> rawData : project.entrySet()) {
            total += 1;
            if (rawData.getKey().isEmpty()) {
                skipped += 1;
                continue;
            }

            Map<String, String> questions = rawData.getValue();
            String code = questions.get("code");

            Question question = GoogleSheetBuilder.buildQuestion(questions, attributeHashMap, realmName);
            if (question == null) {
                invalid++;
                continue;
            }

            Question existing = codeQuestionMapping.get(code.toUpperCase());
            if (existing == null) {
                if (isSynchronise) {
                    Question val = codeQuestionMappingMainRealm.get(code.toUpperCase());
                    if (val != null) {
                        val.setRealm(realmName);
                        service.updateRealm(val);
                        updated++;
                        continue;
                    }
                }
                log.info("Inserting Question :"+question);
                service.insert(question);
                newItem++;
            } else {
                String name = questions.get("name");
                String html = questions.get("html");
                String directions = questions.get("directions");
                String helper = questions.get("helper");
                String icon = question.getIcon();
                existing.setName(name);
                existing.setHtml(html);
                existing.setDirections(directions);
                existing.setHelper(helper);
                existing.setIcon(icon);
                existing.setAttributeCode(question.getAttributeCode());
                existing.setAttribute(question.getAttribute());

                String oneshotStr = questions.get("oneshot");
                String readonlyStr = questions.get(GoogleSheetBuilder.READONLY);
                String mandatoryStr = questions.get(GoogleSheetBuilder.MANDATORY);
                boolean oneshot = GoogleSheetBuilder.getBooleanFromString(oneshotStr);
                boolean readonly = GoogleSheetBuilder.getBooleanFromString(readonlyStr);
                boolean mandatory = GoogleSheetBuilder.getBooleanFromString(mandatoryStr);
                existing.setOneshot(oneshot);
                existing.setReadonly(readonly);
                existing.setMandatory(mandatory);
                service.upsert(existing, codeQuestionMapping);
                updated++;
            }
        }
        printSummary("Question", total, invalid, skipped, updated, newItem);
        questionsFromDBMainRealm = null;
        codeQuestionMappingMainRealm = null;
        questionsFromDB = null;
        codeQuestionMapping = null;
        attributesFromDB = null;
        attributeHashMap = null;
    }

    public void validationsOptimization(Map<String, Map<String, String>> project, String realmName) {
        String tableName = "Validation";
        // Get existing validation by realm from database
        List<Validation> validationsFromDB = service.queryTableByRealm(tableName, realmName);

        // Unique code set
        HashSet<String> codeSet = new HashSet<>();
        // Code to validation object mapping
        HashMap<String, CodedEntity> codeValidationMapping = new HashMap<>();

        for (Validation vld : validationsFromDB) {
            codeSet.add(vld.getCode());
            codeValidationMapping.put(vld.getCode(), vld);
        }

        ArrayList<CodedEntity> validationInsertList = new ArrayList<>();
        ArrayList<CodedEntity> validationUpdateList = new ArrayList<>();
        int invalid = 0;
        int total = 0;
        int skipped = 0;
        int newItem = 0;
        int updated = 0;

        for (Map<String, String> validations : project.values()) {
            total += 1;
            String code = validations.get("code");
            if(code == null) {
                log.error("Failed to get code for validation: " + total + " in realm: " + realmName);
                continue;
            }
            code = code.replaceAll("^\"|\"$", "");
            Validation val = GoogleSheetBuilder.buildValidation(validations, realmName, code);

            // validation check
            if (isValid(val)) {
                if (codeSet.contains(code.toUpperCase())) {
                    if (isChanged(val, codeValidationMapping.get(code.toUpperCase()))) {
                        validationUpdateList.add(val);
                        updated++;
                    } else {
                        skipped++;
                    }
                } else {
                    validationInsertList.add(val);
                    newItem++;
                }
            } else {
                invalid += 1;
            }
        }
        service.bulkInsert(validationInsertList);
        service.bulkUpdate(validationUpdateList, codeValidationMapping);
        printSummary(tableName, total, invalid, skipped, updated, newItem);
        validationsFromDB = null;
        codeValidationMapping = null;
        validationInsertList  = null;
        validationUpdateList = null;
    }

    private String getCodeBySearchKey(String searchKey, Map<String, String> baseEntityAttr) {
        if (baseEntityAttr.containsKey(searchKey.toLowerCase())) {
            return baseEntityAttr.get(searchKey.toLowerCase()).replaceAll("^\"|\"$", "");
        }
        return null;
    }

    private Map<String, Set<String>> getAttributeCodeFromLinkDefs(DEFBaseentityAttribute defBaseentityAttribute,
                                                     Map<String, DEFBaseentityAttribute> defBeAttrCodeObjMapping,
                                                     Set<String> scannedDefs) {
        // {DEF:(attr, attr)}
        Map<String, Set<String>> defAttributeMapping = new HashMap<>();
        for (String linkedDefBeCode: defBaseentityAttribute.getIncludeDefBaseentitys()) {
            DEFBaseentityAttribute defBeAttr = defBeAttrCodeObjMapping.get(linkedDefBeCode);
            if ( defBeAttr == null) {
                log.error("ATTENTION, Can not find " + linkedDefBeCode + " from DEF_BaseentityAttribute sheet.");
            } else  {
                if (scannedDefs.contains(linkedDefBeCode)) {
                    log.warn("Found scanned dependence DEFs, " + linkedDefBeCode + ", skip this code.");
                    continue;
                } else {
                    scannedDefs.add(linkedDefBeCode);
                }

                defAttributeMapping.putIfAbsent(linkedDefBeCode, defBeAttr.getAttributes());
                if (defBeAttr.isHasLnkInclude()) {
                    Map<String, Set<String>> tmpDefAttributes = getAttributeCodeFromLinkDefs(defBeAttr, defBeAttrCodeObjMapping, scannedDefs);
                    defAttributeMapping.putAll(tmpDefAttributes);
                }
            }
        }
        return defAttributeMapping;
    }

    private static<K, V> Map<K, V> clone(Map<K, V> original)
    {
        return original.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, Map<String, String>> generateNewValueSet(String defBeCode, Map<String, Set<String>> attrFromLinkDefs,
                                                                  Map<String, Map<String, String>> project) {
        /*
            1. ignore if attribute already had
            2. if attribute exist in 2 or more linked defs, check valueBoolean,
               pick if true, treat null, empty and false value as false
            3. pick first one if valueBoolean all false, and log warning
        */
        final String BASEENTITYCODE = "baseentitycode";
        Map<String, Map<String, String>> newValueSet = new HashMap<>();
        for(String lnkDefBeCode: attrFromLinkDefs.keySet()) {
            Set<String> attrs = attrFromLinkDefs.get(lnkDefBeCode);
            for (String attrCode:attrs) {
                // new Attribute
                String key = defBeCode+attrCode;
                if(!project.containsKey(key)) {
                    // copy attribute value from linked def
                    Map<String, String> tmpValue =  project.get(lnkDefBeCode + attrCode);
                    if (newValueSet.containsKey(key)) {
                        // check valueBoolean and copy if true
                        String valueboolean = tmpValue.get("valueboolean");
                        if (valueboolean != null && valueboolean.equalsIgnoreCase("TRUE")) {
                            String existing = newValueSet.get(key).get("valueboolean");
                             if (existing == null || !existing.equalsIgnoreCase("TRUE"))  {
                                 // replace when new valueBoolean is true and previous boolean is false or null
                                 Map<String, String> newValue  = clone(tmpValue);
                                 newValue.remove(BASEENTITYCODE);
                                 newValue.put(BASEENTITYCODE, defBeCode);
                                 newValueSet.put(key, newValue);
                             }
                        }
                    } else {
                        Map<String, String> newValue  = clone(tmpValue);
                        newValue.remove(BASEENTITYCODE);
                        newValue.put(BASEENTITYCODE, defBeCode);
                        newValueSet.put(key, newValue);
                    }
                }
            }
        }
        return newValueSet;
    }

    private Map<String, Map<String, String>> extendDefBaseentityAttribute(Map<String, Map<String, String>> project) {
        Map<String, Map<String, String>> newDefBeAttr = clone(project);
        Map<String, DEFBaseentityAttribute> DEFBeAttrCodeObjMapping = new HashMap<>();
        Set<String> defBaseentityHasLnkInclude = new HashSet<>();
        DEFBaseentityAttribute defBaseentityAttribute  = null;

        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
            Map<String, String> baseEntityAttr = entry.getValue();

            // get be_code
            String baseEntityCode = null;
            String searchKey = "baseEntityCode";
            baseEntityCode = getCodeBySearchKey(searchKey, baseEntityAttr);
            if (baseEntityCode == null) {
                log.error("Invalid" + searchKey + " record, BaseEntityCode not found in [" + baseEntityAttr + "]");
                continue;
            }

            // get attr_code
            String attributeCode = null;
            searchKey = "attributeCode";
            attributeCode =getCodeBySearchKey(searchKey, baseEntityAttr);
            if (attributeCode == null) {
                log.error("Invalid" + searchKey + " record, BaseEntityCode not found in [" + baseEntityAttr + "]");
                continue;
            }

            // Set defBaseentityAttribute value
            if (!DEFBeAttrCodeObjMapping.containsKey(baseEntityCode)) {
                defBaseentityAttribute = new DEFBaseentityAttribute(baseEntityCode);
                defBaseentityAttribute.getAttributes().add(attributeCode);
                DEFBeAttrCodeObjMapping.put(baseEntityCode, defBaseentityAttribute);
            } else {
                defBaseentityAttribute = DEFBeAttrCodeObjMapping.get(baseEntityCode);
                defBaseentityAttribute.getAttributes().add(attributeCode);
            }

            // save DEF_xx for further process
            if (attributeCode.equals(LNK_INCLUDE)) {
            	log.info("LNK_INCLUDE ->"+baseEntityCode);
                String[] defBaseentityArray = baseEntityAttr.get("valuestring").replace("[","")
                        .replace("]","")
                        .replace("\"", "")
                        .replaceAll("\\s", "") // remove all space
                        .split(",");
                defBaseentityAttribute.getIncludeDefBaseentitys().addAll(Arrays.asList(defBaseentityArray));
                defBaseentityAttribute.setHasLnkInclude(true);

                // add for further process
                defBaseentityHasLnkInclude.add(baseEntityCode);
            }
        }

        for (String defBeCode : defBaseentityHasLnkInclude) {
            Set<String> scannedDefs = new HashSet<>();
            scannedDefs.add(defBeCode);

            defBaseentityAttribute  = DEFBeAttrCodeObjMapping.get(defBeCode);
            Map<String, Set<String>> attrFromLinkDefs = getAttributeCodeFromLinkDefs(defBaseentityAttribute,
                    DEFBeAttrCodeObjMapping, scannedDefs);

            //Final process, cherry pick attribute from linked defs
            newDefBeAttr.putAll(generateNewValueSet(defBeCode, attrFromLinkDefs, project));
            // remove LNK_INCLUDE attr
            newDefBeAttr.remove(defBeCode + LNK_INCLUDE);
        }
        return newDefBeAttr;
    }


    private boolean isValidDEFAttribute(HashMap<String, Attribute> attrHashMap, String attributeCode, String prefix) {
        boolean isValid = true;
        String trimmedAttrCode = attributeCode.replaceFirst(prefix, "");
        if(attrHashMap.get(trimmedAttrCode.toUpperCase()) == null) {
            isValid = false;
            log.error("Found DEF attribute:" + attributeCode + ", but real attribute code:" + trimmedAttrCode + " does not exist");
        }
        return isValid;
    }

    private DataType getDataTypeFromRealAttribute(String attributeCode, String prefix, HashMap<String, Attribute> attrHashMap) {
        String trimmedAttrCode = attributeCode.replaceFirst(prefix, "");
        return attrHashMap.get(trimmedAttrCode.toUpperCase()).getDataType();
    }

    private Attribute createVirtualDefAttribute(String attributeCode, String realmName, DataType dataType) {
        // ATT_ doesn't exist in database, create and persist
        Attribute virtualAttr = new Attribute(attributeCode, attributeCode, dataType);
        virtualAttr.setRealm(realmName);
        return virtualAttr;
    }


    private boolean hasDefPrefix(String attributeCode) {
        for (String prefix: defPrefixDataTypeMapping.keySet()) {
            if(attributeCode.startsWith(prefix))
                return true;
        }
        return false;
    }

    private String getDefPrefix(String attributeCode) {
        for (String prefix: defPrefixDataTypeMapping.keySet()) {
            if(attributeCode.startsWith(prefix))
                return prefix;
        }
        return null;
    }

    public boolean isJSONValid(String jsonStr) {
        try {
            new JSONObject(jsonStr);
        } catch (JSONException ex) {
            try {
                new JSONArray(jsonStr);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    public void def_baseEntityAttributesOptimization(Map<String, Map<String, String>> project, String realmName,
                                                 HashMap<String, String> userCodeUUIDMapping,
                                                 Map<String, DataType> dataTypes) {
        log.info("Processing DEF_BaseEntityAttribute data");
        Map<String, Map<String, String>> newProject = extendDefBaseentityAttribute(project);

        // "DEF_XXX":"ATT_XXX, ATT_YYY"
        HashMap<String, String> def_basenetity_attributes_mapping = new HashMap<>();

        // Get all BaseEntity
        String tableName = "BaseEntity";
        List<BaseEntity> baseEntityFromDB = service.queryTableByRealm(tableName, realmName);
        HashMap<String, BaseEntity> beHashMap = new HashMap<>();
        for (BaseEntity be : baseEntityFromDB) {
            beHashMap.put(be.getCode(), be);
        }

        // Get all Attribute
        tableName = "Attribute";
        List<Attribute> attributeFromDB = service.queryTableByRealm(tableName, realmName);
        HashMap<String, Attribute> attrHashMap = new HashMap<>();
        for (Attribute attribute : attributeFromDB) {
            attrHashMap.put(attribute.getCode(), attribute);
        }

        int invalid = 0;
        int total = 0;
        int skipped = 0;
        int newItem = 0;
        int updated = 0;


        List<BaseEntity> baseEntities = new ArrayList<>();
        // Attribute code start with ATT_
        ArrayList<CodedEntity> virtualDefAttribute = new ArrayList<>();

        for (Map.Entry<String, Map<String, String>> entry : newProject.entrySet()) {
            total++;
            Map<String, String> baseEntityAttr = entry.getValue();

            String baseEntityCode = GoogleSheetBuilder.getBaseEntityCodeFromBaseEntityAttribute(baseEntityAttr,
                    userCodeUUIDMapping);
            if (baseEntityCode == null) {
                invalid++;
                continue;
            }

            String attributeCode = GoogleSheetBuilder.getAttributeCodeFromBaseEntityAttribute(baseEntityAttr);
            if (attributeCode == null) {
                invalid++;
                continue;
            }  else if (hasDefPrefix(attributeCode)) {
                String defPrefix = getDefPrefix(attributeCode);
                assert(defPrefix != null);
                if (!isValidDEFAttribute(attrHashMap, attributeCode, defPrefix)) {
                    invalid++;
                    continue;
                } else {
                    DataType dataType = dataTypes.get(defPrefixDataTypeMapping.get(defPrefix));
					// If default, then find the datatype of the actual attribute
					if (defPrefix.equals(DFT_PREFIX)) {
						dataType = attrHashMap.get(attributeCode.substring(DFT_PREFIX.length())).getDataType();
					}
                    // update datatype in case real attribute datatype changed
                    if (attrHashMap.containsKey(attributeCode)) {
                        attrHashMap.get(attributeCode).setDataType(dataType);
                    } else {
                        // ATT_ doesn't exist in database, create and persist
                        log.info("Create new virtual Attribute:" + attributeCode);
                        Attribute virtualAttr = createVirtualDefAttribute(attributeCode, realmName, dataType);
                        virtualDefAttribute.add(virtualAttr);
                        attrHashMap.put(attributeCode, virtualAttr);
                    }
                }
            }

            BaseEntity be = GoogleSheetBuilder.buildEntityAttribute(baseEntityAttr, realmName, attrHashMap, beHashMap,
                    userCodeUUIDMapping);
            if (be != null) {
                baseEntities.add(be);
                if (be.getCode().startsWith(DEF_PREFIX)) {
                    List<String> attributeCodeList= new ArrayList<>();
                    for(EntityAttribute ea : be.getBaseEntityAttributes()) {
                        // not LNK_
                       if (!ea.getAttributeCode().equals(LNK_INCLUDE)) {
                           if (ea.getAttributeCode().startsWith(ATT_PREFIX)) {
                               attributeCodeList.add(ea.getAttributeCode().replaceFirst(ATT_PREFIX, ""));
                           } else {
                               attributeCodeList.add(ea.getAttributeCode());
                           }
                           // check valueString if valid JSON
                           if(ea.getAttributeCode().startsWith(SER_PREFIX) && ea.getValueString() != null) {
                               if(!isJSONValid(ea.getValueString())) {
                                    log.error("Invalid JSON valueString, BaseentityCode:" + ea.getBaseEntityCode() + ", attributeCode:" + ea.getAttributeCode());
                               }
                           }
                       }
                    }
                    def_basenetity_attributes_mapping.put(be.getCode(), String.join(",", attributeCodeList));
                }
                newItem++;
            } else {
                invalid++;
            }
        }
        service.bulkInsert(virtualDefAttribute);

        for (BaseEntity be: baseEntities) {
            Set<EntityAttribute> entityAttributeList = be.getBaseEntityAttributes();
            for (EntityAttribute ea : entityAttributeList) {
                if (ea.getAttributeCode().equals(LNK_INCLUDE)) {
                    List<String> tmpList = new ArrayList<>();
                    String[] defBaseentityArray = ea.getValueString().replace("[","")
                            .replace("]","")
                            .replace("\"", "")
                            .replaceAll("\\s", "") // remove all space
                            .split(",");
                    for (String defBeCode: defBaseentityArray) {
                        if(def_basenetity_attributes_mapping.containsKey(defBeCode)) {
                            tmpList.add(def_basenetity_attributes_mapping.get(defBeCode));
                        }
                    }
                    if (!tmpList.isEmpty()) {
                        String target = "[";
                        String[] array = String.join(",", tmpList) .replaceAll("\\s", "")
                                .replace("\"", "")
                                .replaceAll("\\s", "") // remove all space
                                .split(",");
                        for (String str : array) {
                            target += "\"" + str + "\"" + ",";
                        }
                        if (target.endsWith(",") )
                            target = target.substring(0, target.length() -1) + "]";
                        ea.setValueString(target);
                    }
                }
            }
        }
        service.bulkUpdateWithAttributes(baseEntities);
        printSummary("DEF_BaseEntityAttributes", total, invalid, skipped, updated, newItem);
        baseEntityFromDB = null;
        beHashMap = null;
        attributeFromDB = null;
        attrHashMap = null;
        def_basenetity_attributes_mapping = null;
        newProject = null;
        virtualDefAttribute = null;
        baseEntities = null;
    }

    public void def_baseEntitysOptimization(Map<String, Map<String, String>> project, String realmName,
                                        HashMap<String, String> userCodeUUIDMapping) {

        log.info("Processing DEF_BaseEntity data");
        String tableName = "BaseEntity";

        Instant start = Instant.now();
        List<BaseEntity> baseEntityFromDB = service.queryTableByRealm(tableName, realmName);
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info(debugStr + " Finished query table:" + tableName + ", cost:" + timeElapsed.toMillis() + " millSeconds.");

        HashMap<String, CodedEntity> codeBaseEntityMapping = new HashMap<>();

        for (BaseEntity be : baseEntityFromDB) {
            codeBaseEntityMapping.put(be.getCode(), be);
        }

        ArrayList<CodedEntity> baseEntityInsertList = new ArrayList<>();
        ArrayList<CodedEntity> baseEntityUpdateList = new ArrayList<>();
        int invalid = 0;
        int total = 0;
        int skipped = 0;
        int newItem = 0;
        int updated = 0;

        start = Instant.now();
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
            total += 1;
            Map<String, String> baseEntitys = entry.getValue();
            BaseEntity baseEntity = GoogleSheetBuilder.buildBaseEntity(baseEntitys, realmName);
            // validation check
            if (isValid(baseEntity)) {
                // get keycloak uuid from keycloak, replace code and beasentity
                if (baseEntity.getCode().startsWith("PER_")) {
                    String keycloakUUID = KeycloakUtils.getKeycloakUUIDByUserCode(baseEntity.getCode(), userCodeUUIDMapping);
                    baseEntity.setCode(keycloakUUID);
                }

                if (codeBaseEntityMapping.containsKey(baseEntity.getCode())) {
                    if (isChanged(baseEntity, codeBaseEntityMapping.get(baseEntity.getCode()))) {
                        baseEntityUpdateList.add(baseEntity);
                        updated++;
                    } else {
                        skipped++;
                    }
                } else {
                    // insert new item
                    baseEntityInsertList.add(baseEntity);
                    newItem++;
                }
            } else {
                invalid++;
            }
        }
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info(debugStr + " Finished for loop, type:" + tableName + ", cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        service.bulkInsert(baseEntityInsertList);
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info(debugStr + " Finished bulk insert, type:" + tableName + ", cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        service.bulkUpdate(baseEntityUpdateList, codeBaseEntityMapping);
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info(debugStr + " Finished bulk update, type:" + tableName + ", cost:" + timeElapsed.toMillis() + " millSeconds.");
        printSummary("DEF_BaseEntity", total, invalid, skipped, updated, newItem);
        baseEntityFromDB = null;
        codeBaseEntityMapping = null;
        baseEntityInsertList = null;
        baseEntityUpdateList = null;
    }
}
