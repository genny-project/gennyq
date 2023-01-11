package life.genny.bootq.imprt;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.NoResultException;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Base64;

import life.genny.bootq.models.ModuleUnit;
import life.genny.bootq.models.RealmUnit;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.EntityEntity;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
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

    public void persistProject(RealmUnit realmUnit) {
        Instant start = Instant.now();
		// persist each module
		List<ModuleUnit> modules = realmUnit.getModules();
		for (ModuleUnit moduleUnit : realmUnit.getModules()) {
			persistModuleUnit(moduleUnit);
		}
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished get user from keycloak, cost:" + timeElapsed.toMillis() + " millSeconds.");
    }

	/**
	 * @param moduleUnit
	 */
	public void persistModuleUnit(ModuleUnit moduleUnit) {

        Instant start = Instant.now();
        buildValidations(moduleUnit);
        Duration timeElapsed = Duration.between(start, Instant.now());
        log.info(debugStr + " Finished validations, cost:" + timeElapsed.toMillis() + " millSeconds.");

        Map<String, DataType> dataTypes = buildDataTypes(moduleUnit);

        start = Instant.now();
        buildAttributes(moduleUnit, dataTypes);
        timeElapsed = Duration.between(start, Instant.now());
        log.info(debugStr + " Finished attribute, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        buildDefBaseEntitys(moduleUnit);
        timeElapsed = Duration.between(start, Instant.now());
        log.info(debugStr + " Finished def_baseentity, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        buildDefBaseEntityAttributes(moduleUnit, dataTypes);
        timeElapsed = Duration.between(start, Instant.now());
        log.info(debugStr + " Finished def_baseentity_attribute, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        buildBaseEntitys(moduleUnit);
        timeElapsed = Duration.between(start, Instant.now());
        log.info(debugStr + " Finished baseentity, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        buildBaseEntityAttributes(moduleUnit);
        timeElapsed = Duration.between(start, Instant.now());
        log.info(debugStr + " Finished baseentity_attribute, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        buildEntityEntitys(moduleUnit);
        timeElapsed = Duration.between(start, Instant.now());
        log.info(debugStr + " Finished entity_entity, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        buildQuestions(moduleUnit);
        timeElapsed = Duration.between(start, Instant.now());
        log.info(debugStr + " Finished question, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        buildQuestionQuestions(moduleUnit);
        timeElapsed = Duration.between(start, Instant.now());
        log.info(debugStr + " Finished question_question, cost:" + timeElapsed.toMillis() + " millSeconds.");
	}

    /**
     * @param table
     * @param realm
     */
    public void buildValidations(ModuleUnit moduleUnit) {
		// iterate validations table
        for (Map<String, String> validations : moduleUnit.getValidations().values()) {
			// init validation
            String code = validations.get("code");
			String name = validations.get("name");
			String regex = validations.get("regex");
			String errorMessage = validations.get("error_message");
			String options = validations.get("options");
			Validation validation = new Validation(code, name, regex, options);
			// set realm and err
			validation.setRealm(moduleUnit.getProductCode());
			validation.setErrormsg(errorMessage);
			// persist
			databaseUtils.saveValidation(validation);
        }
    }

    /**
     * @param moduleUnit
     * @return
     */
    public Map<String, DataType> buildDataTypes(ModuleUnit moduleUnit) {
		// iterate datatypes table
        final Map<String, DataType> dataTypeMap = new HashMap<>();
        moduleUnit.getDataTypes().entrySet().stream()
				.filter(d -> !d.getKey().matches("\\s*"))
				.forEach(data -> {
            Map<String, String> fields = data.getValue();
            String validations = fields.get("validations");
            String code = (fields.get("code"));
            String className = fields.get("classname");
            String name = fields.get("name");
            String inputmask = fields.get("inputmask");
            String component = fields.get("component");
			// build validation list
            ValidationList validationList = new ValidationList();
            validationList.setValidationList(new ArrayList<Validation>());
            if (validations != null) {
                final String[] validationListStr = validations.split(",");
                for (String validationCode : validationListStr) {
					Validation validation = databaseUtils.findValidationByCode(moduleUnit.getProductCode(), validationCode);
					validationList.getValidationList().add(validation);
                }
            }
			DataType dataType = new DataType(className, validationList, name, inputmask, component);
			dataType.setDttCode(code);
			dataTypeMap.put(code, dataType);
        });
        return dataTypeMap;
    }

    /**
     * @param moduleUnit
     * @param dataTypeMap
     */
    public void buildAttributes(ModuleUnit moduleUnit, Map<String, DataType> dataTypeMap) {
		// iterate attributes table
        for (Map<String, String> fields : moduleUnit.getAttributes().values()) {
			String code = fields.get("code");
			String dataType = fields.get("datatype");
			String name = fields.get("name");
			// find datatype
			DataType dataTypeRecord = dataTypeMap.get(dataType);
			// build attribute
			Attribute attr = new Attribute(code, name, dataTypeRecord);
			attr.setRealm(moduleUnit.getProductCode());
			// persist attribute
			databaseUtils.saveAttribute(attr);
		}
    }

    /**
     * @param moduleUnit
     */
    public void buildBaseEntityAttributes(ModuleUnit moduleUnit) {
		String productCode = moduleUnit.getProductCode();
		// iterate EA table
        for (Map<String, String> fields : moduleUnit.getBaseEntitys().values()) {
            String baseEntityCode = fields.get("baseEntityCode");
            String attributeCode = fields.get("attributeCode");
			// fetch BE and Attribute
			BaseEntity baseEntity = databaseUtils.findBaseEntityByCode(productCode, baseEntityCode);
			Attribute attribute = databaseUtils.findAttributeByCode(productCode, attributeCode);
			// build EntityAttribute
			EntityAttribute ea = buildEntityAttribute(fields, productCode, baseEntity, attribute);
			baseEntity.addAttribute(ea);
			// persist
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
     * @param moduleUnit
     */
    public void buildBaseEntitys(ModuleUnit moduleUnit) {
		// iterate baseEntitys
        for (Map<String, String> fields : moduleUnit.getBaseEntitys().values()) {
			// build and persist
			BaseEntity baseEntity = buildBaseEntity(fields, moduleUnit.getProductCode());
			databaseUtils.saveBaseEntity(baseEntity);
        }
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
     * @param moduleUnit
     */
    public void buildEntityEntitys(ModuleUnit moduleUnit) {
		String productCode = moduleUnit.getProductCode();
		// iterate table
        for (Map<String, String> fields : moduleUnit.getEntityEntitys().values()) {
            String linkCode = fields.get("linkCode");
            String parentCode = fields.get("parentCode");
            String targetCode = fields.get("targetCode");
            double weight = Double.valueOf(fields.get("weight"));
            String valueString = fields.get("valueString".toLowerCase().replaceAll("^\"|\"$|_|-", "").replaceAll("\n", ""));
			// fetch link attribute
			Attribute linkAttribute = databaseUtils.findAttributeByCode(productCode, linkCode);
			// find source and target
            BaseEntity source = databaseUtils.findBaseEntityByCode(productCode, parentCode);
            BaseEntity target = databaseUtils.findBaseEntityByCode(productCode, targetCode);
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
     * @param moduleUnit
     */
    public void buildQuestionQuestions(ModuleUnit moduleUnit) {
		String productCode = moduleUnit.getProductCode();
        for (Map<String, String> fields : moduleUnit.getQuestionQuestions().values()) {
			// get parent and child
            String parentCode = fields.get("parentCode");
            String targetCode = fields.get("targetCode");
			Question parent = databaseUtils.findQuestionByCode(productCode, parentCode);
			Question child = databaseUtils.findQuestionByCode(productCode, targetCode);
			// find fields
			Double weight = Double.parseDouble(fields.get(WEIGHT));
			Boolean mandatory = "TRUE".equalsIgnoreCase(fields.get(MANDATORY));
			Boolean readonly = "TRUE".equalsIgnoreCase(fields.get(READONLY));
			Boolean formTrigger = "TRUE".equalsIgnoreCase(fields.get("formtrigger"));
			Boolean createOnTrigger = "TRUE".equalsIgnoreCase(fields.get("createontrigger"));
			String dependency = fields.get("dependency");
			String icon = fields.get("icon");
			Boolean disabled = "TRUE".equalsIgnoreCase(fields.get("disabled"));
			Boolean hidden = "TRUE".equalsIgnoreCase(fields.get("hidden"));
			Boolean oneshot = "TRUE".equalsIgnoreCase(fields.get(ONESHOT));
			// init QQ
			QuestionQuestion qq = new QuestionQuestion(parent, child, weight);
			qq.setOneshot(oneshot);
			qq.setReadonly(readonly);
			qq.setCreateOnTrigger(createOnTrigger);
			qq.setFormTrigger(formTrigger);
			qq.setRealm(productCode);
			qq.setDependency(dependency);
			qq.setIcon(icon);
			qq.setDisabled(disabled);
			qq.setHidden(hidden);
			qq.setMandatory(mandatory);
        }
    }

    public void buildQuestions(ModuleUnit moduleUnit) {
		String productCode = moduleUnit.getProductCode();
        for (Map<String, String> fields : moduleUnit.getQuestions().values()) {
            String code = fields.get("code");
			String name = fields.get("name");
			String placeholder = fields.get("placeholder");
			String directions = fields.get("directions");
			String attributeCode = fields.get("attribute_code");
			String html = fields.get("html");
			String helper = fields.get("helper");
			String icon = fields.get("icon");
			Boolean oneshot = "TRUE".equalsIgnoreCase(fields.get("oneshot"));
			Boolean readonly = "TRUE".equalsIgnoreCase(fields.get(READONLY));
			Boolean mandatory = "TRUE".equalsIgnoreCase(fields.get(MANDATORY));
			// find attr
			Attribute attribute = databaseUtils.findAttributeByCode(productCode, attributeCode);
			// init Question
			Question q = new Question(code, name, attribute);
			q.setAttributeCode(attributeCode.toUpperCase());
			q.setOneshot(oneshot);
			q.setHtml(html);
			q.setReadonly(readonly);
			q.setMandatory(mandatory);
			q.setPlaceholder(placeholder);
			q.setRealm(productCode);
			q.setDirections(directions);
			q.setHelper(helper);
			q.setIcon(icon);
		}
	}

    /**
     * @param moduleUnit
     * @param dataTypes
     */
    public void buildDefBaseEntityAttributes(ModuleUnit moduleUnit, Map<String, DataType> dataTypes) {
		String productCode = moduleUnit.getProductCode();
        for (Map<String, String> fields : moduleUnit.getDef_entityAttributes().values()) {
            String baseEntityCode = fields.get("baseEntityCode");
            String defAttributeCode = fields.get("attributeCode");
			String prefix = defAttributeCode.substring(0, 3);
			String attributeCode = defAttributeCode.substring(4);
			// init BE
			BaseEntity baseEntity = databaseUtils.findBaseEntityByCode(productCode, baseEntityCode);

			DataType dataType = dataTypes.get(defPrefixDataTypeMapping.get(prefix));
			// If default, then find the datatype of the actual attribute
			if (prefix.equals(DFT_PREFIX)) {
				attributeCode = attributeCode.substring(DFT_PREFIX.length());
				Attribute attribute = databaseUtils.findAttributeByCode(productCode, attributeCode);
				dataType = attribute.getDataType();
			}

			Attribute attribute = databaseUtils.findAttributeByCode(productCode, attributeCode);
			// update datatype in case real attribute datatype changed
			if (attribute != null) {
				attribute.setDataType(dataType);
			} else {
				// ATT_ doesn't exist in database, create and persist
				log.info("Create new virtual Attribute:" + attributeCode);
				attribute = createVirtualDefAttribute(attributeCode, productCode, dataType);
			}
			databaseUtils.saveAttribute(attribute);

			EntityAttribute ea = buildEntityAttribute(fields, productCode, baseEntity, attribute);
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
     * @param moduleUnit
     */
    public void buildDefBaseEntitys(ModuleUnit moduleUnit) {
		String productCode = moduleUnit.getProductCode();
        for (Map<String, String> fields : moduleUnit.getDef_baseEntitys().values()) {
			BaseEntity baseEntity = buildBaseEntity(fields, productCode);
			databaseUtils.saveBaseEntity(baseEntity);
        }
    }

    /**
     * @param productCode
     * @param securityKey
     * @param servicePass
     * @return
     */
    private String decodePassword(String productCode, String securityKey, String servicePass) {
        String initVector = "PRJ_" + productCode.toUpperCase();
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
