package life.genny.bootq.bootxport.xlsimport;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.ws.rs.NotFoundException;

import org.jboss.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.utils.KeycloakUtils;
import life.genny.qwandaq.validation.Validation;

public class GoogleSheetBuilder {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().getClass());

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

    public static boolean getBooleanFromString(final String booleanString) {
        if (booleanString == null) {
            return false;
        }
        return "TRUE".equalsIgnoreCase(booleanString.toUpperCase()) || "YES".equalsIgnoreCase(booleanString.toUpperCase())
                || "T".equalsIgnoreCase(booleanString.toUpperCase())
                || "Y".equalsIgnoreCase(booleanString.toUpperCase()) || "1".equalsIgnoreCase(booleanString);

    }

    public static Validation buildValidation(Map<String, String> validations, String realmName, String code) {
        boolean hasValidOptions = false;
        Gson gsonObject = new Gson();
        String optionString = validations.get("options");
        if (optionString != null && (!optionString.equals(" "))) {
            try {
                gsonObject.fromJson(optionString, Options[].class);
                log.trace("FOUND VALID OPTIONS STRING:" + optionString);
                hasValidOptions = true;
            } catch (JsonSyntaxException ex) {
                log.error("FOUND INVALID OPTIONS STRING:" + optionString);
                throw new JsonSyntaxException(ex.getMessage());
            }
        }

        String regex = validations.get("regex");
        if (regex != null) {
            regex = regex.replaceAll(REGEX_1, "");
        }
        if ("VLD_AU_DRIVER_LICENCE_NO".equalsIgnoreCase(code)) {
            log.trace("detected VLD_AU_DRIVER_LICENCE_NO");
        }
        String name = validations.get("name").replaceAll(REGEX_1, "");
        String recursiveStr = validations.get("recursive");
        String multiAllowedStr = validations.get("multi_allowed".toLowerCase().replaceAll(REGEX_2, ""));
        String groupCodesStr = validations.get("group_codes".toLowerCase().replaceAll(REGEX_2, ""));
        Boolean recursive = getBooleanFromString(recursiveStr);
        Boolean multiAllowed = getBooleanFromString(multiAllowedStr);
        String errorMessage = validations.get("error_message".toLowerCase().replaceAll(REGEX_2, ""));
        Validation val = null;
        if (code.startsWith(Validation.getDefaultCodePrefix() + "SELECT_")) {
            if (hasValidOptions) {
                log.trace("Case 1, build Validation with OPTIONS String");
                val = new Validation(code, name, groupCodesStr, recursive, multiAllowed, optionString);
            } else {
                val = new Validation(code, name, groupCodesStr, recursive, multiAllowed);
            }
        } else {
            if (hasValidOptions) {
                log.trace("Case 2, build Validation with OPTIONS String");
                val = new Validation(code, name, regex, optionString);
            } else {
                val = new Validation(code, name, regex);
            }
        }
        val.setRealm(realmName);
        val.setErrormsg(errorMessage);
        log.trace("realm:" + realmName + ",code:" + code + ",name:" + name + ",val:" + val + ", grp="
                + (groupCodesStr != null ? groupCodesStr : "X"));
        return val;
    }

    public static Attribute buildAttrribute(Map<String, String> attributes, Map<String, DataType> dataTypeMap,
                                            String realmName, String code) {
        String dataType = null;
        if (!attributes.containsKey("datatype")) {
            log.error("DataType for " + code + " cannot be null");
            throw new NotFoundException("Bad DataType given for code " + code);
        }

        dataType = attributes.get("datatype").trim().replaceAll(REGEX_1, "");
        String name = attributes.get("name").replaceAll(REGEX_1, "");
        DataType dataTypeRecord = dataTypeMap.get(dataType);

        String privacyStr = attributes.get(PRIVACY);
        if (privacyStr != null) {
            privacyStr = privacyStr.toUpperCase();
        }

        boolean privacy = "TRUE".equalsIgnoreCase(privacyStr);
        if (privacy) {
            log.trace("Realm:" + realmName + ", Attribute " + code + " has default privacy");
        }
        String descriptionStr = attributes.get("description");
        String helpStr = attributes.get("help");
        String placeholderStr = attributes.get("placeholder");
        String defaultValueStr = attributes.get("defaultValue".toLowerCase().replaceAll(REGEX_2, ""));
        String icon = attributes.get("icon");

        Attribute attr = new Attribute(code, name, dataTypeRecord);
        attr.setDefaultPrivacyFlag(privacy);
        attr.setDescription(descriptionStr);
        attr.setHelp(helpStr);
        attr.setPlaceholder(placeholderStr);
        attr.setDefaultValue(defaultValueStr);
        attr.setRealm(realmName);
        attr.setIcon(icon);
        return attr;
    }

    private static QuestionQuestion hasChild(Question sourceQuestion, String targetCode) {
        for (QuestionQuestion qq : sourceQuestion.getChildQuestions()) {
            if (qq.getPk().getTargetCode().equals(targetCode)) {
            return qq;
            }
        }
        return null;
    }

    public static QuestionQuestion buildQuestionQuestion(Map<String, String> queQues, String realmName, Map<String, Question> questionHashMap) {

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

    public static Question buildQuestion(Map<String, String> questions, Map<String, Attribute> attributeHashMap, String realmName) {
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

        Boolean oneshot = getBooleanFromString(oneshotStr);
        Boolean readonly = getBooleanFromString(readonlyStr);
        Boolean mandatory = getBooleanFromString(mandatoryStr);

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


    public static String getAttributeCodeFromBaseEntityAttribute(Map<String, String> baseEntityAttr) {
        String attributeCode = null;
        String searchKey = "attributeCode".toLowerCase();
        if (baseEntityAttr.containsKey(searchKey)) {
            attributeCode = baseEntityAttr.get(searchKey).replaceAll("^\"|\"$", "");
        } else {
            log.error("Invalid record, AttributeCode not found in [" + baseEntityAttr + "]");
        }
        return attributeCode;
    }


    public static String getBaseEntityCodeFromBaseEntityAttribute(Map<String, String> baseEntityAttr) {
        String baseEntityCode = null;
        String searchKey = "baseEntityCode".toLowerCase();
        if (baseEntityAttr.containsKey(searchKey)) {
            baseEntityCode = baseEntityAttr.get(searchKey).replaceAll("^\"|\"$", "");
        } else {
            log.error("Invalid record, BaseEntityCode not found in [" + baseEntityAttr + "]");
        }
        return baseEntityCode;
    }

    public static BaseEntity buildEntityAttribute(Map<String, String> baseEntityAttr, String realmName, 
		Map<String, Attribute> attrHashMap, Map<String, BaseEntity> beHashMap, HashMap<String, String> userCodeUUIDMapping) {
        String attributeCode = getAttributeCodeFromBaseEntityAttribute(baseEntityAttr);
        if (attributeCode == null) return null;
        
        // Check if attribute code exist in Attribute table, foreign key restriction
        Attribute attribute = attrHashMap.get(attributeCode.toUpperCase());
        if (attribute == null) {
            log.error(String.format("Invalid EntityAttribute record, AttributeCode:%s is not in the Attribute Table!!!", attributeCode));
            return null;
        }


        List<String> asList = Collections.singletonList("valuestring");
        Optional<String> valueString = Optional.empty();
        try {
			valueString = asList.stream().map(baseEntityAttr::get).findFirst();
		} catch (Exception e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
			//log.error("NULL ERROR: "+baseEntityAttr.get("baseentitycode")+":"+attributeCode + " doesn't have column valuestring.");
		}
        Integer valueInt = null;
        Optional<String> ofNullable = Optional.ofNullable(baseEntityAttr.get(VALUEINTEGER));
        if (ofNullable.isPresent() && !baseEntityAttr.get(VALUEINTEGER).matches("\\s*") && (attribute.getDataType().getClassName().contains("Integer"))) {
            BigDecimal big = new BigDecimal(baseEntityAttr.get(VALUEINTEGER));
            Optional<String[]> nullableVal = Optional.of(big.toPlainString().split("[.]"));
            valueInt = nullableVal.filter(d -> d.length > 0).map(d -> Integer.valueOf(d[0])).get();
        }

        Long valueLong = null;
        Optional<String> ofNullableLong = Optional.ofNullable(baseEntityAttr.get(VALUELONG));
        if (ofNullableLong.isPresent() && !baseEntityAttr.get(VALUELONG).matches("\\s*") && (attribute.getDataType().getClassName().contains("Long"))) {
            BigDecimal big = new BigDecimal(baseEntityAttr.get(VALUELONG));
            Optional<String[]> nullableVal = Optional.of(big.toPlainString().split("[.]"));
            valueLong = nullableVal.filter(d -> d.length > 0).map(d -> Long.valueOf(d[0])).get();
        }

        Double valueDouble = null;
        Optional<String> ofNullableDouble = Optional.ofNullable(baseEntityAttr.get(VALUEDOUBLE));
        if (ofNullableDouble.isPresent() && !baseEntityAttr.get(VALUEDOUBLE).matches("\\s*") && (attribute.getDataType().getClassName().contains("Double"))) {
            BigDecimal big = null;
            try {
				big = new BigDecimal(baseEntityAttr.get(VALUEDOUBLE));
				Optional<String[]> nullableVal = Optional.of(big.toPlainString().split("[.]"));
				valueDouble = nullableVal.filter(d -> d.length > 0).map(d -> Double.valueOf(d[0])).get();
			} catch (Exception e) {
				log.error("Bad fDouble format "+attributeCode);
			}
        }

        Boolean valueBoolean = null;
        if(baseEntityAttr.containsKey(VALUEBOOLEAN) && attribute.getDataType().getClassName().contains("Boolean")) {
            Optional<Boolean> ofNullableBoolean = Optional.ofNullable("TRUE".equalsIgnoreCase(baseEntityAttr.get(VALUEBOOLEAN))
                                                               && (attribute.getDataType().getClassName().contains("Boolean")));
            valueBoolean = ofNullableBoolean.get();
        }

        String valueStr = null;
        if (valueString.isPresent()) {
            valueStr = valueString.get().replaceAll(REGEX_1, "");
        }

        String baseEntityCode = getBaseEntityCodeFromBaseEntityAttribute(baseEntityAttr);
        if (baseEntityCode == null) return null;

        String weight = baseEntityAttr.get(WEIGHT);
        String privacyStr = baseEntityAttr.get(PRIVACY);
        Boolean privacy = "TRUE".equalsIgnoreCase(privacyStr);

        String confirmationStr= baseEntityAttr.get(CONFIRMATION);
        Boolean confirmation = "TRUE".equalsIgnoreCase(confirmationStr);

        // Check if baseEntity code exist in BaseEntity table, foreign key restriction
        BaseEntity baseEntity = beHashMap.get(baseEntityCode.toUpperCase());
        if (baseEntity == null) {
            log.error(String.format("Invalid EntityAttribute record, BaseEntityCode:%s is not in the BaseEntity Table!!!", baseEntityCode));
            return null;
        }

        double weightField = 0.0;
        if (isDouble(weight)) {
            weightField = Double.parseDouble(weight);
        }

        EntityAttribute ea = null;
        if (valueString.isPresent()) {
        	 try {
                 ea = baseEntity.addAttribute(attribute, weightField, valueStr);
             } catch (BadDataException be) {
                 log.error(String.format("Should never reach here!, Error:%s", be.getMessage()));
             }
        	 valueBoolean = null; // force
        } else
        if (valueLong != null) {
            try {
                ea = baseEntity.addAttribute(attribute, weightField, valueLong);
            } catch (BadDataException be) {
                log.error(String.format("Should never reach here!, Error:%s", be.getMessage()));
            }
        } else if (valueDouble != null) {
            try {
                ea = baseEntity.addAttribute(attribute, weightField, valueDouble);
            } catch (BadDataException be) {
                log.error(String.format("Should never reach here!, Error:%s", be.getMessage()));
            }
        } else if (valueInt != null) {
            try {
                ea = baseEntity.addAttribute(attribute, weightField, valueInt);
            } catch (BadDataException be) {
                log.error(String.format("Should never reach here!, Error:%s", be.getMessage()));
            }
        } else if (Boolean.TRUE.equals(valueBoolean)) {
            try {
            	if (!attribute.getDataType().getClassName().equalsIgnoreCase("java.lang.Boolean")) {
            		attribute.setDataType(new DataType(Boolean.class));
                    log.error("Attribute dataType is not Boolean, updated to Boolean, "
                    + "attributeCode:" + attributeCode + ", dttType:" + attribute.getDataType().getClassName()
                    + ", baseentityCode:" + baseEntityCode);
            	}
                ea = baseEntity.addAttribute(attribute, weightField, valueBoolean);
            } catch (BadDataException be) {
                log.error(String.format("Should never reach here!, Error:%s", be.getMessage()));
            }
        } else {
            try {
                ea = baseEntity.addAttribute(attribute, weightField, valueStr);
            } catch (BadDataException be) {
                log.error(String.format("Should never reach here!, Error:%s", be.getMessage()));
            }
        }

        if (ea != null) {
            if (privacy || attribute.getDefaultPrivacyFlag()) {
                ea.setPrivacyFlag(true);
            }

            if (confirmation) {
                ea.setConfirmationFlag(true);
            }

            if (valueBoolean!= null) {
                ea.setValueBoolean(valueBoolean);
            }

            ea.setRealm(realmName);
        }

        baseEntity.setRealm(realmName);
        return baseEntity;
    }

    private static String getNameFromMap(Map<String, String> baseEntitys, String defaultString) {
        String key = "name";
        String ret = defaultString;
        if (baseEntitys.containsKey(key)) {
            if (baseEntitys.get(key) != null) {
                ret = baseEntitys.get(key).replaceAll("^\"|\"$", "");
            }
        }
        return ret;
    }

    public static BaseEntity buildBaseEntity(Map<String, String> baseEntitys, String realmName) {

        String code = baseEntitys.get("code").replaceAll("^\"|\"$", "");
        String name = getNameFromMap(baseEntitys, code);
        BaseEntity be = new BaseEntity(code, name);
        be.setRealm(realmName);
        return be;
    }
}
