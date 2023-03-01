package life.genny.bootq.utils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.validation.Validation;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import javax.ws.rs.NotFoundException;

public class GoogleSheetBuilder {
    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
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

    private GoogleSheetBuilder() {
    }

    public static boolean toBoolean(final String booleanString) {
        if (booleanString == null) {
            return false;
        }
        return "TRUE".equalsIgnoreCase(booleanString.toUpperCase());
    }

    public static Double toDouble(final String doubleString) {
        if (doubleString == null) {
            return 0.0;
        }
        return Double.parseDouble(doubleString);
    }

    /**
	 * Build a Validation object from a row.
	 *
     * @param row The row from the sheets
     * @param realmName The realm
     * @return A Validation object
     */
    public static Validation buildValidation(Map<String, String> row, String realmName) {
		String code = row.get("code");
		String name = row.get("name");
		String regex = row.get("regex");
		Validation validation = new Validation(code, name, regex);
		validation.setErrormsg(row.get("error_message"));
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
	public static DataType buildDataType(Map<String, String> row, String realmName) {
		DataType dataType = new DataType();
		dataType.setDttCode(row.get("code"));
		dataType.setClassName(row.get("classname"));
		dataType.setInputmask(row.get("inputmask"));
		dataType.setComponent(row.get("component"));
		dataType.setValidationCodes(row.get("validations"));
		return dataType;
	}

    /**
	 * Build a Attribute object from a row.
	 *
     * @param row THe row from the sheets
     * @param realmName The realm
     * @return A Attribute object
     */
    public static Attribute buildAttribute(Map<String, String> row, String realmName) {

        String code = row.get("code");
        String name = row.get("name");
        String dttCode = row.get("datatype");

        Attribute attr = new Attribute();
		attr.setCode(code);
		attr.setName(name);
		attr.setDttCode(dttCode);
        attr.setDefaultPrivacyFlag(toBoolean(row.get("privacy")));
        attr.setDescription(row.get("description"));
        attr.setHelp(row.get("help"));
        attr.setPlaceholder(row.get("placeholder"));
        attr.setDefaultValue(row.get("defaultValue"));
        attr.setIcon(row.get("icon"));
        attr.setRealm(realmName);
        return attr;
    }

    /**
	 * Build a BaseEntity object from a row.
	 *
     * @param row THe row from the sheets
     * @param realmName The realm
     * @return A BaseEntity object
     */
    public static BaseEntity buildBaseEntity(Map<String, String> row, String realmName) {

        String code = row.get("code");
        String name = row.get("name");
        BaseEntity baseEntity = new BaseEntity(code, name);
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
    public static EntityAttribute buildEntityAttribute(Map<String, String> row, String realmName) {
		return buildEntityAttribute(row, realmName, row.get("attributeCode"));
	}

    public static EntityAttribute buildEntityAttribute(Map<String, String> row, String realmName, String attributeCode) {

		EntityAttribute entityAttribute = new EntityAttribute();
		entityAttribute.setBaseEntityCode(row.get("baseEntityCode"));
		entityAttribute.setAttributeCode(attributeCode);
		entityAttribute.setRealm(realmName);
        
        String valueString = row.get(VALUESTRING);
        Integer valueInt = Integer.valueOf(row.get(VALUEINTEGER));
        Long valueLong = Long.valueOf(row.get(VALUELONG));
        Double valueDouble = Double.valueOf(row.get(VALUEDOUBLE));
        Boolean valueBoolean = toBoolean(row.get(VALUEBOOLEAN));

        Double weight = toDouble(row.get(WEIGHT));

        boolean privacy = toBoolean(row.get(PRIVACY));
        boolean confirmation = toBoolean(row.get(CONFIRMATION));

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
    public static Question buildQuestion(Map<String, String> row, String realmName) {

        String code = row.get("code");
        String name = row.get("name");
        String attributeCode = row.get("attribute_code");
        String html = row.get("html");
        String placeholder = row.get("placeholder");
        boolean readonly = toBoolean(row.get(READONLY));
        boolean mandatory = toBoolean(row.get(MANDATORY));
        String icon = row.get("icon");

        Question q = new Question();
		q.setCode(code);
		q.setName(name);
		q.setAttributeCode(attributeCode);
        q.setHtml(html);
		q.setPlaceholder(placeholder);
        q.setReadonly(readonly);
        q.setMandatory(mandatory);
        q.setIcon(icon);
        q.setRealm(realmName);

        return q;
    }

    /**
	 * Build a QuestionQuestion object from a row.
	 *
     * @param row THe row from the sheets
     * @param realmName The realm
     * @return A QuestionQuestion object
     */
    public static QuestionQuestion buildQuestionQuestion(Map<String, String> row, String realmName) {

        String parentCode = row.get("parentCode");
        String targetCode = row.get("targetCode");

        Double weight = toDouble(row.get(WEIGHT));
        boolean mandatory = toBoolean(row.get(MANDATORY));
        boolean readonly = toBoolean(row.get(READONLY));

        String icon = row.get("icon");
        boolean disabled = toBoolean(row.get("disabled"));
        boolean hidden = toBoolean(row.get("hidden"));

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
