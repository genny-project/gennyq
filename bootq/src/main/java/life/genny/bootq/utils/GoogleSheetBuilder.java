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

    private static boolean isDouble(String doubleStr) {
        final String Digits = "(\\p{Digit}+)";
        final String HexDigits = "(\\p{XDigit}+)";
        // an exponent is 'e' or 'E' followed by an optionally
        // signed decimal integer.
        final String Exp = "[eE][+-]?" + Digits;
        final String fpRegex =
                ("[\\x00-\\x20]*" +  // Optional leading "whitespace"
                        "[+-]?(" + // Optional sign character
                        "NaN|" +           // "NaN" string
                        "Infinity|" +      // "Infinity" string

                        // A decimal floating-point string representing a finite positive
                        // number without a leading sign has at most five basic pieces:
                        // Digits . Digits ExponentPart FloatTypeSuffix
                        //
                        // Since this method allows integer-only strings as input
                        // in addition to strings of floating-point literals, the
                        // two sub-patterns below are simplifications of the grammar
                        // productions from section 3.10.2 of
                        // The Java Language Specification.

                        // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
                        "(((" + Digits + "(\\.)?(" + Digits + "?)(" + Exp + ")?)|" +

                        // . Digits ExponentPart_opt FloatTypeSuffix_opt
                        "(\\.(" + Digits + ")(" + Exp + ")?)|" +

                        // Hexadecimal strings
                        "((" +
                        // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
                        "(0[xX]" + HexDigits + "(\\.)?)|" +

                        // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
                        "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +

                        ")[pP][+-]?" + Digits + "))" +
                        "[fFdD]?))" +
                        "[\\x00-\\x20]*");// Optional trailing "whitespace"

        boolean result = false;
        
        try {
			result = Pattern.matches(fpRegex, doubleStr);
		} catch (Exception e) {
			log.error("Error in isDouble ->"+doubleStr);
		}
        
        return result;
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

    public static Validation buildValidation(Map<String, String> row, String realmName) {
		String code = row.get("code");
		String name = row.get("name");
		String regex = row.get("regex");
		Validation validation = new Validation(code, name, regex);
		validation.setErrormsg(row.get("error_message"));
		validation.setRealm(realmName);
        return validation;
    }

	public static DataType buildDataType(Map<String, String> row, String realmName) {
		DataType dataType = new DataType();
		dataType.setDttCode(row.get("code"));
		dataType.setClassName(row.get("classname"));
		dataType.setInputmask(row.get("inputmask"));
		dataType.setComponent(row.get("component"));
		dataType.setValidationCodes(row.get("validations"));
		return dataType;
	}

    public static Attribute buildAttribute(Map<String, String> row, String realmName) {

        String code = row.get("code");
        String name = row.get("name");
        String dttCode = row.get("datatype");

        Attribute attr = new Attribute(code, name);
        attr.setDefaultPrivacyFlag(toBoolean(row.get("privacy")));
        attr.setDescription(row.get("description"));
        attr.setHelp(row.get("help"));
        attr.setPlaceholder(row.get("placeholder"));
        attr.setDefaultValue(row.get("defaultValue"));
        attr.setIcon(row.get("icon"));
        attr.setRealm(realmName);
        return attr;
    }

    public static BaseEntity buildBaseEntity(Map<String, String> row, String realmName) {

        String code = row.get("code");
        String name = row.get("name");
        BaseEntity baseEntity = new BaseEntity(code, name);
        baseEntity.setRealm(realmName);
        return baseEntity;
    }

    public static EntityAttribute buildEntityAttribute(Map<String, String> row, String realmName) {

		EntityAttribute entityAttribute = new EntityAttribute();
		entityAttribute.setBaseEntityCode(row.get("baseEntityCode"));
		entityAttribute.setAttributeCode(row.get("attributeCode"));
		entityAttribute.setRealm(realmName);
        
        String valueString = row.get(VALUESTRING);
        Integer valueInt = row.get(VALUEINTEGER);
        Long valueLong = row.get(VALUELONG);
        Double valueDouble = row.get(VALUEDOUBLE);
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


    private static QuestionQuestion hasChild(Question sourceQuestion, String targetCode) {
        for (QuestionQuestion qq : sourceQuestion.getChildQuestions()) {
            if (qq.getPk().getTargetCode().equals(targetCode)) {
            return qq;
            }
        }
        return null;
    }

    public static QuestionQuestion buildQuestionQuestion(Map<String, String> queQues,
                                                         String realmName,
                                                         Map<String, Question> questionHashMap) {

        String parentCode = queQues.get("parentCode".toLowerCase().replaceAll("^\"|\"$|_|-", ""));
        if (parentCode == null) {
            parentCode = queQues.get("sourceCode".toLowerCase().replaceAll("^\"|\"$|_|-", ""));
        }

        String targetCode = queQues.get("targetCode".toLowerCase().replaceAll("^\"|\"$|_|-", ""));

        String weightStr = queQues.get(WEIGHT);
        String mandatoryStr = queQues.get(MANDATORY);
        String readonlyStr = queQues.get(READONLY);
        Boolean readonly = "TRUE".equalsIgnoreCase(readonlyStr);
        Boolean formTrigger = queQues.get("formtrigger") != null && "TRUE".equalsIgnoreCase(queQues.get("formtrigger"));
        Boolean createOnTrigger = queQues.get("createontrigger") != null && "TRUE".equalsIgnoreCase(queQues.get("createontrigger"));
        String dependency = queQues.get("dependency");
        String icon = queQues.get("icon");
        Boolean disabled = queQues.get("disabled") != null && "TRUE".equalsIgnoreCase(queQues.get("disabled"));
        Boolean hidden = queQues.get("hidden") != null && "TRUE".equalsIgnoreCase(queQues.get("hidden"));

        double weight = 0.0;
        if(weightStr == null || weightStr.isBlank())
            log.error("Weight for QuestionQuestion: " + parentCode + "//" + targetCode + " is missing!");

        if (isDouble(weightStr)) {
            weight = Double.parseDouble(weightStr);
        }

        Boolean mandatory = "TRUE".equalsIgnoreCase(mandatoryStr);

        Question sbe = questionHashMap.get(parentCode.toUpperCase());
        Question tbe = questionHashMap.get(targetCode.toUpperCase());
        if (sbe == null) {
            log.error("QuestionQuesiton parent code:" + parentCode + " doesn't exist in Question table.");
            return null;
        } else if (tbe == null) {
            log.error("QuestionQuesiton target Code:" + targetCode + " doesn't exist in Question table.");
            return null;
        }

        // Icon will default to Target Question's icon if null
        if (icon == null) {
            icon = tbe.getIcon();
        }

        String oneshotStr = queQues.get("oneshot");
        Boolean oneshot = false;
        if (oneshotStr == null) {
            // Set the oneshot to be that of the targetquestion
            oneshot = tbe.getOneshot();
        } else {
            oneshot = "TRUE".equalsIgnoreCase(oneshotStr);
        }

        try {
            QuestionQuestion qq  = hasChild(sbe, tbe.getCode()) ;
            if(qq == null) {
                qq = sbe.addChildQuestion(tbe.getCode(), weight, mandatory);
            }
            qq.setOneshot(oneshot);
            qq.setReadonly(readonly);
            qq.setCreateOnTrigger(createOnTrigger);
            qq.setFormTrigger(formTrigger);
            qq.setRealm(realmName);
            qq.setDependency(dependency);
            qq.setIcon(icon);
            qq.setDisabled(disabled);
            qq.setHidden(hidden);
            qq.setMandatory(mandatory);
            return qq;
        } catch (BadDataException be) {
            log.error("Should never reach here, got BadDataException when process sourceCode: " + sbe.getCode() + ", targetCode:" + tbe.getCode());
        }
        return null;
    }

    public static Question buildQuestion(Map<String, String> questions,
                                         Map<String, Attribute> attributeHashMap,
                                         String realmName) {
        String code = questions.get("code");
        String name = questions.get("name");
        String placeholder = questions.get("placeholder");
        String directions = questions.get("directions");
        String attrCode = questions.get("attribute_code".toLowerCase().replaceAll("^\"|\"$|_|-", ""));
        String html = questions.get("html");
        String oneshotStr = questions.get("oneshot");
        String readonlyStr = questions.get(READONLY);
        String mandatoryStr = questions.get(MANDATORY);
        String helper = questions.get("helper");
        String icon = questions.get("icon");

        Boolean oneshot = toBoolean(oneshotStr);
        Boolean readonly = toBoolean(readonlyStr);
        Boolean mandatory = toBoolean(mandatoryStr);

        Attribute attr = attributeHashMap.get(attrCode.toUpperCase());
        if (attr == null) {
			if (attrCode.contains(".")) {
				String[] attributeFields = attrCode.toUpperCase().split("\\.");
				attr = attributeHashMap.get(attributeFields[attributeFields.length-1]);
				if (attr == null) {
					log.error(String.format("Question: %s can not find Attribute:%s in database!", code, attrCode.toUpperCase()));
					return null;
				}
				log.info(String.format("Question: %s using linked Attribute:%s", code, attr.getCode()));
			} else {
				log.error(String.format("Question: %s can not find Attribute:%s in database!", code, attrCode.toUpperCase()));
				return null;
			}
        }

        // Icon will default to Attribute's icon if null
        if ( (icon == null) || (icon.equals("null")) ) {
            icon = attr.getIcon();
        }

        Question q = null;
        if (placeholder != null) {
            q = new Question(code, name, attr, placeholder);
        } else {
            q = new Question(code, name, attr);
        }
		q.setAttributeCode(attrCode.toUpperCase());
        q.setOneshot(oneshot);
        q.setHtml(html);
        q.setReadonly(readonly);
        q.setMandatory(mandatory);
        q.setRealm(realmName);
        q.setDirections(directions);
        q.setHelper(helper);
        q.setIcon(icon);
        return q;
    }
}
