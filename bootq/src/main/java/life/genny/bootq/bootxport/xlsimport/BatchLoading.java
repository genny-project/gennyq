package life.genny.bootq.bootxport.xlsimport;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Base64;

import life.genny.bootq.bootxport.bootx.QwandaRepository;
import life.genny.bootq.bootxport.bootx.RealmUnit;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.EntityEntity;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.KeycloakUtils;
import life.genny.qwandaq.validation.Validation;
import life.genny.qwandaq.validation.ValidationList;

@ApplicationScoped
public class BatchLoading {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().getClass());

    private String mainRealm = "genny";

    private static final String LNK_INCLUDE = "LNK_INCLUDE";
    private static final String DEF_PREFIX= "DEF_";
    private static final String ATT_PREFIX= "ATT_";
    private static final String SER_PREFIX= "SER_";
    private static final String DFT_PREFIX= "DFT_";
    private static final String DEP_PREFIX= "DEP_";

    private static final String WEIGHT = "weight";
    private static final String PRIVACY = "privacy";
    private static final String CONFIRMATION = "confirmation";

    public static final String MANDATORY = "mandatory";
    public static final String READONLY = "readonly";
    public static final String ONESHOT = "oneshot";

    private static final Map<String, String> defPrefixDataTypeMapping =

    Map.of(ATT_PREFIX,"DTT_BOOLEAN",
    SER_PREFIX, "DTT_JSON",
    DFT_PREFIX, "DTT_TEXT",
    DEP_PREFIX, "DTT_TEXT");

    String debugStr = "Time profile";

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	BaseEntityUtils beUtils;

    public BatchLoading() {
    }

    public void persistProjectOptimization(RealmUnit rx) {

        String realm = rx.getCode();
        String debugStr = "Time profile";
        Instant start = Instant.now();
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info(debugStr + " Finished get user from keycloak, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        buildValidations(rx.getValidations(), rx.getCode());
        timeElapsed = Duration.between(start, Instant.now());
        log.info(debugStr + " Finished validations, cost:" + timeElapsed.toMillis() + " millSeconds.");

        Map<String, DataType> dataTypes = buildDataTypes(rx.getDataTypes(), realm);

        start = Instant.now();
        buildAttributes(rx.getAttributes(), dataTypes, rx.getCode());
        timeElapsed = Duration.between(start, Instant.now());
        log.info(debugStr + " Finished attribute, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        buildDefBaseEntitys(rx.getDef_baseEntitys(), rx.getCode());
        timeElapsed = Duration.between(start, Instant.now());
        log.info(debugStr + " Finished def_baseentity, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        buildDefBaseEntityAttributes(rx.getDef_entityAttributes(), rx.getCode(), dataTypes);
        timeElapsed = Duration.between(start, Instant.now());
        log.info(debugStr + " Finished def_baseentity_attribute, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        buildBaseEntitys(rx.getBaseEntitys(), rx.getCode());
        timeElapsed = Duration.between(start, Instant.now());
        log.info(debugStr + " Finished baseentity, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        buildBaseEntityAttributes(rx.getEntityAttributes(), rx.getCode());
        timeElapsed = Duration.between(start, Instant.now());
        log.info(debugStr + " Finished baseentity_attribute, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        buildEntityEntitys(rx.getEntityEntitys(), rx.getCode());
        timeElapsed = Duration.between(start, Instant.now());
        log.info(debugStr + " Finished entity_entity, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        buildQuestions(rx.getQuestions(), rx.getCode());
        timeElapsed = Duration.between(start, Instant.now());
        log.info(debugStr + " Finished question, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        buildQuestionQuestions(rx.getQuestionQuestions(), rx.getCode());
        timeElapsed = Duration.between(start, Instant.now());
        log.info(debugStr + " Finished question_question, cost:" + timeElapsed.toMillis() + " millSeconds.");
    }

    /**
     * @param table
     * @param realm
     */
    public void buildValidations(Map<String, Map<String, String>> table, String realm) {

        for (Map<String, String> validations : table.values()) {
            String code = validations.get("code");
			String name = validations.get("name");
			String regex = validations.get("regex");
			String errorMessage = validations.get("error_message");
			String options = validations.get("options");

			Validation validation = new Validation(code, name, regex, options);

			validation.setRealm(realm);
			validation.setErrormsg(errorMessage);

			databaseUtils.saveValidation(validation);
        }
    }

    /**
     * @param table
     * @param realm
     * @return
     */
    public Map<String, DataType> buildDataTypes(Map<String, Map<String, String>> table, String realm) {
        final Map<String, DataType> dataTypeMap = new HashMap<>();
        table.entrySet().stream().filter(d -> !d.getKey().matches("\\s*")).forEach(data -> {
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
                        Validation validation = databaseUtils.findValidationByCode(realm, validationCode);
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


    /**
     * @param table
     * @param dataTypeMap
     * @param realm
     */
    public void buildAttributes(Map<String, Map<String, String>> table, Map<String, DataType> dataTypeMap, String realm) {

        Instant start = Instant.now();
        for (Map.Entry<String, Map<String, String>> data : table.entrySet()) {
            Map<String, String> fields = data.getValue();

			String code = fields.get("code");
			String dataType = fields.get("datatype");
			String name = fields.get("name");
			DataType dataTypeRecord = dataTypeMap.get(dataType);

			Attribute attr = new Attribute(code, name, dataTypeRecord);
			attr.setRealm(realm);

			databaseUtils.saveAttribute(attr);
		}
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished building Attributes: " + timeElapsed.toMillis() + " millSeconds.");
    }

    /**
     * @param table
     * @param realm
     */
    public void buildBaseEntityAttributes(Map<String, Map<String, String>> table, String realm) {

        for (Map.Entry<String, Map<String, String>> entry : table.entrySet()) {
            Map<String, String> fields = entry.getValue();

            String baseEntityCode = fields.get("baseEntityCode");
            String attributeCode = fields.get("attributeCode");

			BaseEntity baseEntity = databaseUtils.findBaseEntityByCode(realm, baseEntityCode);
			Attribute attribute = databaseUtils.findAttributeByCode(realm, attributeCode);

			EntityAttribute ea = buildEntityAttribute(fields, realm, baseEntity, attribute);

			baseEntity.addAttribute(ea);
			databaseUtils.saveBaseEntity(baseEntity);
        }
    }

	public EntityAttribute buildEntityAttribute(Map<String, String> fields, String realm, BaseEntity baseEntity, Attribute attribute) {

		String valueString = fields.get("valueString");
		Integer valueInt = Integer.valueOf(fields.get("valueInteger"));
		Long valueLong = Long.valueOf(fields.get("valueLong"));
		Double valueDouble = Double.valueOf(fields.get("valueDouble"));

		Boolean valueBoolean = Boolean.valueOf(fields.get("valueBoolean"));

		double weight = Double.parseDouble(fields.get(WEIGHT));

		String privacyStr = fields.get(PRIVACY);
		Boolean privacy = "TRUE".equalsIgnoreCase(privacyStr);

		String confirmationStr= fields.get(CONFIRMATION);
		Boolean confirmation = "TRUE".equalsIgnoreCase(confirmationStr);


		EntityAttribute ea = new EntityAttribute(baseEntity, attribute, weight);
		if (valueString != null) {
			ea.setValueString(valueString);
		} else if (valueLong != null) {
			ea.setValueLong(valueLong);
		} else if (valueDouble != null) {
			ea.setValueDouble(valueDouble);
		} else if (valueInt != null) {
			ea.setValueInteger(valueInt);
		} else if (valueBoolean != null) {
			ea.setValueBoolean(valueBoolean);
		} else {
			log.error("No value present for " + baseEntity.getCode() + ":" + attribute.getCode());
		}

		ea.setPrivacyFlag(privacy);
		ea.setConfirmationFlag(confirmation);
		ea.setRealm(realm);

		return ea;
	}

    /**
     * @param table
     * @param realm
     */
    public void buildBaseEntitys(Map<String, Map<String, String>> table, String realm) {
        Instant start = Instant.now();
        for (Map.Entry<String, Map<String, String>> entry : table.entrySet()) {
            Map<String, String> fields = entry.getValue();
			BaseEntity baseEntity = buildBaseEntity(fields, realm);
			databaseUtils.saveBaseEntity(baseEntity);
        }
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished building BaseEntity: cost:" + timeElapsed.toMillis() + " millSeconds.");
    }

	/**
	 * @param fields
	 * @param realm
	 * @return
	 */
	public BaseEntity buildBaseEntity(Map<String, String> fields, String realm) {
		String code = fields.get("code");
		String name = fields.get("name");
		BaseEntity baseEntity = new BaseEntity(code, name);
		baseEntity.setRealm(realm);
		return baseEntity;
	}

    /**
     * @param table
     * @param realm
     */
    public void buildEntityEntitys(Map<String, Map<String, String>> table, String realm) {

        for (Map.Entry<String, Map<String, String>> entry : table.entrySet()) {
            Map<String, String> fields = entry.getValue();
            String linkCode = fields.get("linkCode");
            String parentCode = fields.get("parentCode");
            String targetCode = fields.get("targetCode");

            double weight = Double.valueOf(fields.get("weight"));
            String valueString = fields.get("valueString".toLowerCase().replaceAll("^\"|\"$|_|-", "").replaceAll("\n", ""));

            Attribute linkAttribute = databaseUtils.findAttributeByCode(realm, linkCode);
            BaseEntity source = databaseUtils.findBaseEntityByCode(realm, parentCode);
            BaseEntity target = databaseUtils.findBaseEntityByCode(realm, targetCode);
            if (linkAttribute == null) {
                log.error("EntityEntity Link code:" + linkCode + " doesn't exist in Attribute table.");
                continue;
            } else if (source == null) {
                log.error("EntityEntity parent code:" + parentCode + " doesn't exist in BaseEntity table.");
                continue;
            } else if (target == null) {
                log.error("EntityEntity target Code:" + targetCode + " doesn't exist in BaseEntity table.");
                continue;
            }

			EntityEntity ee = new EntityEntity(source, target, linkAttribute, weight);
			ee.setValueString(valueString);
			ee.setWeight(weight);
			databaseUtils.saveEntityEntity(ee);
        }
    }

    /**
     * @param table
     * @param realm
     */
    public void buildQuestionQuestions(Map<String, Map<String, String>> table, String realm) {

        for (Map.Entry<String, Map<String, String>> entry : table.entrySet()) {
            Map<String, String> fields = entry.getValue();

            String parentCode = fields.get("parentCode");
            String targetCode = fields.get("targetCode");

			Question parent = databaseUtils.findQuestionByCode(realm, parentCode);
			Question child = databaseUtils.findQuestionByCode(realm, targetCode);

			Double weight = Double.parseDouble(fields.get(WEIGHT));

			Boolean mandatory = "TRUE".equalsIgnoreCase(fields.get(MANDATORY));
			Boolean readonly = "TRUE".equalsIgnoreCase(fields.get(READONLY));
			Boolean formTrigger = "TRUE".equalsIgnoreCase(fields.get("formtrigger"));
			Boolean createOnTrigger = "TRUE".equalsIgnoreCase(fields.get("createontrigger"));
			String dependency = fields.get("dependency");
			String icon = fields.get("icon");
			Boolean disabled = fields.get("disabled") != null && "TRUE".equalsIgnoreCase(fields.get("disabled"));
			Boolean hidden = fields.get("hidden") != null && "TRUE".equalsIgnoreCase(fields.get("hidden"));


			Boolean oneshot = "TRUE".equalsIgnoreCase(fields.get(ONESHOT));

			QuestionQuestion qq = new QuestionQuestion(parent, child, weight);
			qq.setOneshot(oneshot);
			qq.setReadonly(readonly);
			qq.setCreateOnTrigger(createOnTrigger);
			qq.setFormTrigger(formTrigger);
			qq.setRealm(realm);
			qq.setDependency(dependency);
			qq.setIcon(icon);
			qq.setDisabled(disabled);
			qq.setHidden(hidden);
			qq.setMandatory(mandatory);
        }
    }

    /**
     * @param table
     * @param realm
     */
    public void buildQuestions(Map<String, Map<String, String>> table, String realm) {

        for (Map.Entry<String, Map<String, String>> rawData : table.entrySet()) {

            Map<String, String> questions = rawData.getValue();
            String code = questions.get("code");
			String name = questions.get("name");
			String placeholder = questions.get("placeholder");
			String directions = questions.get("directions");
			String attributeCode = questions.get("attribute_code");
			String html = questions.get("html");
			String helper = questions.get("helper");
			String icon = questions.get("icon");

			Boolean oneshot = "TRUE".equalsIgnoreCase(questions.get("oneshot"));
			Boolean readonly = "TRUE".equalsIgnoreCase(questions.get(READONLY));
			Boolean mandatory = "TRUE".equalsIgnoreCase(questions.get(MANDATORY));

			Attribute attribute = databaseUtils.findAttributeByCode(realm, attributeCode);

			Question q = new Question(code, name, attribute);
			q.setAttributeCode(attributeCode.toUpperCase());
			q.setOneshot(oneshot);
			q.setHtml(html);
			q.setReadonly(readonly);
			q.setMandatory(mandatory);
			q.setPlaceholder(placeholder);
			q.setRealm(realm);
			q.setDirections(directions);
			q.setHelper(helper);
			q.setIcon(icon);
		}
	}

    /**
     * @param table
     * @param realm
     * @param dataTypes
     */
    public void buildDefBaseEntityAttributes(Map<String, Map<String, String>> table, String realm, Map<String, DataType> dataTypes) {

        for (Map.Entry<String, Map<String, String>> entry : table.entrySet()) {
            Map<String, String> fields = entry.getValue();

            String baseEntityCode = fields.get("baseEntityCode");
            String defAttributeCode = fields.get("attributeCode");

			String prefix = defAttributeCode.substring(0, 3);
			String attributeCode = defAttributeCode.substring(4);

			BaseEntity baseEntity = databaseUtils.findBaseEntityByCode(realm, baseEntityCode);

			DataType dataType = dataTypes.get(defPrefixDataTypeMapping.get(prefix));
			// If default, then find the datatype of the actual attribute
			if (prefix.equals(DFT_PREFIX)) {
				attributeCode = attributeCode.substring(DFT_PREFIX.length());
				Attribute attribute = databaseUtils.findAttributeByCode(realm, attributeCode);
				dataType = attribute.getDataType();
			}

			Attribute attribute = databaseUtils.findAttributeByCode(realm, attributeCode);
			// update datatype in case real attribute datatype changed
			if (attribute != null) {
				attribute.setDataType(dataType);
			} else {
				// ATT_ doesn't exist in database, create and persist
				log.info("Create new virtual Attribute:" + attributeCode);
				attribute = createVirtualDefAttribute(attributeCode, realm, dataType);
			}
			databaseUtils.saveAttribute(attribute);

			EntityAttribute ea = buildEntityAttribute(fields, realm, baseEntity, attribute);
			baseEntity.addAttribute(ea);
			databaseUtils.saveBaseEntity(baseEntity);

			if (ea.getAttributeCode().equals(LNK_INCLUDE)) {
				String[] defBaseentityArray = CommonUtils.cleanUpAttributeValue(ea.getValueString()).split(",");
				if (defBaseentityArray.length > 0) {
					String target = "[";
					for (String str : defBaseentityArray) {
						target += "\"" + str + "\"" + ",";
					}
					if (target.endsWith(",")) {
						target = target.substring(0, target.length() -1) + "]";
					}
					ea.setValueString(target);
				}
			}
        }
    }

    /**
     * @param table
     * @param realm
     */
    public void buildDefBaseEntitys(Map<String, Map<String, String>> table, String realm) {

        Instant start = Instant.now();
        for (Map.Entry<String, Map<String, String>> entry : table.entrySet()) {
            Map<String, String> fields = entry.getValue();
			BaseEntity baseEntity = buildBaseEntity(fields, realm);
			databaseUtils.saveBaseEntity(baseEntity);
        }

        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info(debugStr + " Finished for loop Def Entity, cost:" + timeElapsed.toMillis() + " millSeconds.");
    }

    /**
     * @param realm
     * @param securityKey
     * @param servicePass
     * @return
     */
    private String decodePassword(String realm, String securityKey, String servicePass) {
        String initVector = "PRJ_" + realm.toUpperCase();
        initVector = StringUtils.rightPad(initVector, 16, '*');
        String decrypt = decrypt(securityKey, initVector, servicePass);
        return decrypt;
    }

	/**
	 * @param key
	 * @param initVector
	 * @param encrypted
	 * @return
	 */
	public static String decrypt(String key, String initVector, String encrypted) {
		try {
			IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
			SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

			byte[] original = cipher.doFinal(Base64.decode(encrypted));

			return new String(original);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return null;
	}

    private Attribute createVirtualDefAttribute(String attributeCode, String realmName, DataType dataType) {
        // ATT_ doesn't exist in database, create and persist
        Attribute virtualAttr = new Attribute(attributeCode, attributeCode, dataType);
        virtualAttr.setRealm(realmName);
        return virtualAttr;
    }

}
