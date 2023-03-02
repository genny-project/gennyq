package life.genny.bootq.utils;

import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.utils.AttributeUtils;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.EntityAttributeUtils;
import life.genny.qwandaq.utils.QuestionUtils;
import life.genny.qwandaq.validation.Validation;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

@ApplicationScoped
public class GoogleSheetBuilder {

    private static final String WEIGHT = "weight";
    private static final String REGEX_1 = "^\"|\"$";
    private static final String REGEX_2 = "^\"|\"$|_|-";
    private static final String PRIVACY = "privacy";
    private static final String CONFIRMATION = "confirmation";
    private static final String VALUEINTEGER = "valueinteger";
    private static final String VALUEBOOLEAN = "valueboolean";
    private static final String VALUEDOUBLE = "valuedouble";
    private static final String VALUESTRING = "valuestring";
    private static final String VALUEBDATETIME = "valuedatetime";
    private static final String VALUEDATE = "valuedate";
    private static final String VALUETIME = "valuetime";
    private static final String VALUELONG = "valuelong";

    public static final String MANDATORY = "mandatory";
    public static final String READONLY = "readonly";

    Jsonb jsonb = JsonbBuilder.create();

	@Inject
	Logger log;

	@Inject
	CacheManager cm;

	@Inject
	AttributeUtils attributeUtils;

	@Inject
	EntityAttributeUtils beaUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	QuestionUtils questionUtils;

    public GoogleSheetBuilder() { }

    public static Boolean toBoolean(final String booleanString) {
        if (StringUtils.isBlank(booleanString)) {
            return null;
        }
        return "TRUE".equalsIgnoreCase(booleanString.toUpperCase());
    }

    public static Double toDouble(final String doubleString) {
        if (StringUtils.isBlank(doubleString)) {
            return null;
        }
        return Double.parseDouble(doubleString);
    }

    public static Integer toInt(final String intString) {
        if (StringUtils.isBlank(intString)) {
			return null;
		}
        return Integer.valueOf(intString);
	}

    public static Long toLong(final String longString) {
        if (StringUtils.isBlank(longString)) {
			return null;
		}
        return Long.valueOf(longString);
	}

    /**
	 * Build a Validation object from a row.
	 *
     * @param row The row from the sheets
     * @param realmName The realm
     * @return A Validation object
     */
    public Validation buildValidation(Map<String, String> row, String realmName) {

		String code = row.get("code");
		Validation validation;
		try {
			validation = attributeUtils.getValidation(realmName, code);
		} catch (ItemNotFoundException e) {
			// create new validation if not found
			validation = new Validation();
			validation.setCode(code);
			Long id = cm.getMaxValidationId();
			validation.setId(id+1);
		}

		String name = row.get("name");
		String regex = row.get("regex");
		String error = row.get("errormessage");
		validation.setName(name);
		validation.setRegex(regex);
		validation.setErrormsg(error);
		
		// handle the group codes
		String group_codes = row.get("group_codes");
		List<String> groups = StringUtils.isBlank(group_codes) ? new ArrayList<>() : Arrays.asList(group_codes.replaceAll(" ", "").split(","));
		validation.setSelectionBaseEntityGroupList(groups);

		validation.setRealm(realmName);
        return validation;
    }

    /**
	 * Build a DataType object from a row.
	 *
     * @param row THe row from the sheets
     * @param realmName The realm
     * @return A DataType object
     */
	public DataType buildDataType(Map<String, String> row, String realmName) {

		String code = row.get("code");
		DataType dataType = attributeUtils.getDataType(realmName, code, false);
		// create new datatype if not found
		if (dataType == null) {
			dataType = new DataType();
			dataType.setDttCode(code);
		}

		dataType.setClassName(row.get("classname"));
		dataType.setInputmask(row.get("inputmask"));
		dataType.setComponent(row.get("component"));
		dataType.setValidationCodes(row.get("validations"));
		dataType.setRealm(realmName);
		return dataType;
	}

    /**
	 * Build a Attribute object from a row.
	 *
     * @param row THe row from the sheets
     * @param realmName The realm
     * @return An Attribute object
     */
    public Attribute buildAttribute(Map<String, String> row, String realmName) {

        String code = row.get("code");
        String name = row.get("name");

		Attribute attribute = attributeUtils.getAttribute(realmName, code, false);
		// create new attribute if not found
		if (attribute == null) {
			attribute = new Attribute();
			attribute.setCode(code);
			attribute.setName(name);
			Long id = cm.getMaxAttributeId();
			attribute.setId(id+1);
		}
		attribute.setDttCode(row.get("datatype"));
        attribute.setDefaultPrivacyFlag(toBoolean(row.get("privacy")));
        attribute.setDescription(row.get("description"));
        attribute.setHelp(row.get("help"));
        attribute.setPlaceholder(row.get("placeholder"));
        attribute.setDefaultValue(row.get("defaultValue"));
        attribute.setIcon(row.get("icon"));
        attribute.setRealm(realmName);
        return attribute;
    }

    /**
	 * Build a BaseEntity object from a row.
	 *
     * @param row THe row from the sheets
     * @param realmName The realm
     * @return A BaseEntity object
     */
    public BaseEntity buildBaseEntity(Map<String, String> row, String realmName) {

        String code = row.get("code");
        BaseEntity baseEntity = beUtils.getBaseEntity(realmName, code, false);
		if (baseEntity == null) {
			baseEntity = new BaseEntity();
			baseEntity.setCode(code);
			Long id = cm.getMaxBaseEntityId();
			baseEntity.setId(id+1);
		}
        String name = row.get("name");
		baseEntity.setName(name);
        baseEntity.setRealm(realmName);
        return baseEntity;
    }

    /**
	 * Build a EntityAttribute object from a row.
	 *
     * @param row THe row from the sheets
     * @param realmName The realm
     * @return A EntityAttribute object
     */
    public EntityAttribute buildEntityAttribute(Map<String, String> row, String realmName) {
		return buildEntityAttribute(row, realmName, row.get("attributecode"));
	}

    /**
	 * Build a EntityAttribute object from a row.
	 *
     * @param row THe row from the sheets
     * @param realmName The realm
     * @param attributeCode The attributeCode to set
     * @return A EntityAttribute object
     */
    public EntityAttribute buildEntityAttribute(Map<String, String> row, String realmName, String attributeCode) {

		EntityAttribute entityAttribute = new EntityAttribute();
		String baseEntityCode = row.get("baseentitycode");
        BaseEntity baseEntity = beUtils.getBaseEntity(realmName, baseEntityCode, false);
		if (baseEntity == null) {
			log.error("BaseEntity " + baseEntityCode + " does NOT exist!");
		}
		Attribute attribute = attributeUtils.getAttribute(realmName, attributeCode, false);
		if (attribute == null) {
			log.error("Attribute " + attributeCode + " does NOT exist!");
		}

		entityAttribute.setBaseEntityCode(baseEntityCode);
		entityAttribute.setBaseEntityId(baseEntity.getId());
		entityAttribute.setAttributeCode(attributeCode);
		entityAttribute.setAttributeId(attribute.getId());
		entityAttribute.setRealm(realmName);
        
        String valueString = row.get(VALUESTRING);
        Integer valueInt = toInt(row.get(VALUEINTEGER));
        Long valueLong = toLong(row.get(VALUELONG));
        Double valueDouble = toDouble(row.get(VALUEDOUBLE));
        Boolean valueBoolean = toBoolean(row.get(VALUEBOOLEAN));

        Double weight = toDouble(row.get(WEIGHT));

        Boolean privacy = toBoolean(row.get(PRIVACY));
        Boolean confirmation = toBoolean(row.get(CONFIRMATION));

		entityAttribute.setValueString(valueString);
		entityAttribute.setValueInteger(valueInt);
		entityAttribute.setValueLong(valueLong);
		entityAttribute.setValueDouble(valueDouble);
		entityAttribute.setValueBoolean(valueBoolean);

		entityAttribute.setWeight(weight);
		entityAttribute.setPrivacyFlag(privacy);
		entityAttribute.setConfirmationFlag(confirmation);

        return entityAttribute;
    }

    /**
	 * Build a Question object from a row.
	 *
     * @param row The row from the sheet
     * @param realmName the realm
     * @return A Question
     */
    public Question buildQuestion(Map<String, String> row, String realmName) {

        String code = row.get("code");
		Question question = questionUtils.getQuestionFromQuestionCode(realmName, code);
		if (question == null) {
			question = new Question();
			question.setCode(code);
			Long id = cm.getMaxQuestionId();
			question.setId(id+1);
		}
        String attributeCode = row.get("attributecode");

		Attribute attribute = attributeUtils.getAttribute(realmName, attributeCode, false);
		if (attribute == null) {
			throw new ItemNotFoundException(realmName, attributeCode);
		}

        String name = row.get("name");
        String html = row.get("html");
        String placeholder = row.get("placeholder");
        Boolean readonly = toBoolean(row.get(READONLY));
        Boolean mandatory = toBoolean(row.get(MANDATORY));
        String icon = row.get("icon");

		question.setName(name);
		question.setAttributeCode(attributeCode);
        question.setHtml(html);
		question.setPlaceholder(placeholder);
        question.setReadonly(readonly);
        question.setMandatory(mandatory);
        question.setIcon(icon);
        question.setRealm(realmName);

        return question;
    }

    /**
	 * Build a QuestionQuestion object from a row.
	 *
     * @param row THe row from the sheets
     * @param realmName The realm
     * @return A QuestionQuestion object
     */
    public QuestionQuestion buildQuestionQuestion(Map<String, String> row, String realmName) {

        String parentCode = row.get("parentcode");
		Question source = questionUtils.getQuestionFromQuestionCode(realmName, parentCode);
		if (source == null) {
			throw new ItemNotFoundException(realmName, parentCode);
		}
        String targetCode = row.get("targetcode");
		Question target = questionUtils.getQuestionFromQuestionCode(realmName, targetCode);
		if (target == null) {
			throw new ItemNotFoundException(realmName, parentCode);
		}

        Double weight = toDouble(row.get(WEIGHT));
        Boolean mandatory = toBoolean(row.get(MANDATORY));
        Boolean readonly = toBoolean(row.get(READONLY));

        String icon = row.get("icon");
        Boolean disabled = toBoolean(row.get("disabled"));
        Boolean hidden = toBoolean(row.get("hidden"));

		QuestionQuestion questionQuestion = new QuestionQuestion();
		questionQuestion.setParentCode(parentCode);
		questionQuestion.setChildCode(targetCode);
		questionQuestion.setWeight(weight);
		questionQuestion.setMandatory(mandatory);
		questionQuestion.setReadonly(readonly);
		questionQuestion.setDisabled(disabled);
		questionQuestion.setHidden(hidden);
		questionQuestion.setIcon(icon);
		questionQuestion.setRealm(realmName);

		return questionQuestion;
    }

}
