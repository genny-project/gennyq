package life.genny.qwandaq.utils;

import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.jboss.logging.Logger;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;

/**
 * A utiltity used in the MailMerge feature of Genny.
 * 
 * @author Jasper Robison
 */
@ApplicationScoped
public class MergeUtils {
	
	private static final Logger log = Logger.getLogger(MergeUtils.class);

	public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_RED = "\u001B[31m";
	
    /* [[VARIABLENAME.ATTRIBUTE]] pattern */
    /* Used for baseentity-attribute merging */
	public static final String REGEX_START = "[[";
	public static final String REGEX_END = "]]";
	public static final String REGEX_START_PATTERN = Pattern.quote(REGEX_START);
    public static final String REGEX_END_PATTERN = Pattern.quote(REGEX_END);
    public static final Pattern PATTERN = Pattern.compile(REGEX_START_PATTERN + "(?s)(.*?)" + REGEX_END_PATTERN);
    public static final String DEFAULT = "";
    public static final String PATTERN_BASEENTITY = REGEX_START_PATTERN + "(?s)(.*?)" + REGEX_END_PATTERN;
    public static final Pattern PATTERN_MATCHER = Pattern.compile(PATTERN_BASEENTITY);
    
    /* {{VARIABLE}} pattern */
    /* this is for direct merging */
    public static final String VARIABLE_REGEX_START = "{{";
    public static final String VARIABLE_REGEX_END = "}}";
    public static final Pattern PATTERN_VARIABLE = Pattern.compile(Pattern.quote(VARIABLE_REGEX_START) + "(?s)(.*?)" + Pattern.quote(VARIABLE_REGEX_END));  
    
    /* ((FORMAT)) pattern */
    /* this is for formatting data such as dates or strings */
    public static final String FORMAT_VARIABLE_REGEX_START = "((";
    public static final String FORMAT_VARIABLE_REGEX_END = "))";
    public static final Pattern FORMAT_PATTERN_VARIABLE = Pattern.compile(Pattern.quote(FORMAT_VARIABLE_REGEX_START) + "(.*)" + Pattern.quote(FORMAT_VARIABLE_REGEX_END));

	@Inject
	EntityAttributeUtils beaUtils;

	public void mergeBaseEntity(BaseEntity baseEntity, Map<String, Object> contexts) {
		beaUtils.getAllEntityAttributesForBaseEntity(baseEntity).forEach(ea -> {
			if (ea.getValueString() == null)
				return;
			String value = merge(ea.getValueString(), contexts);
			ea.setValueString(value);
			beaUtils.updateEntityAttribute(ea);
		});
	}
    
	/** 
	 * @param mergeStr the mergeStr to merge
	 * @param templateEntityMap the templateEntityMap to merge with
	 * @return String
	 */
	public String merge(String mergeStr, Map<String, Object> templateEntityMap) {
		
		if (mergeStr != null) {

		Matcher match = PATTERN_MATCHER.matcher(mergeStr);
		Matcher matchVariables = PATTERN_VARIABLE.matcher(mergeStr);

		if (templateEntityMap != null && templateEntityMap.size() > 0) {

			while (match.find()) {

				Object mergedObject = wordMerge(match.group(1), templateEntityMap);
				if (mergedObject != null)
					mergeStr = mergeStr.replace(REGEX_START + match.group(1) + REGEX_END, mergedObject.toString());
				else
					mergeStr = mergeStr.replace(REGEX_START + match.group(1) + REGEX_END, "");
			}

			// NOTE: duplicating this for now. ideally wordMerge should be a bit more flexible and allows all kind of data to be passed
			while (matchVariables.find()) {

				Object mergedText = templateEntityMap.get(matchVariables.group(1));
				if (mergedText != null)
					mergeStr = mergeStr.replace(VARIABLE_REGEX_START + matchVariables.group(1) + VARIABLE_REGEX_END, mergedText.toString());
				else
					mergeStr = mergeStr.replace(VARIABLE_REGEX_START + matchVariables.group(1) + VARIABLE_REGEX_END, "");
			}
		}

		} else {
			log.warn("mergeStr is NULL");
		}

		return mergeStr;
	}

	/** 
	 * @param mergeText the mergeText to merge
	 * @param entitymap the entitymap to merge with
	 * @return Object
	 */
	public Object wordMerge(String mergeText, Map<String, Object> entitymap) {

		if (mergeText == null || mergeText.isEmpty())
			return DEFAULT;

		// we split the text to merge into 2 components: BE.PRI... becomes [BE, PRI...]
		String[] entityArr = mergeText.split("\\.");
		String keyCode = entityArr[0];
		log.debug("looking for key in map: " + keyCode);

		if ((entityArr.length == 0))
			return DEFAULT;

		if (!entitymap.containsKey(keyCode))
			return DEFAULT;

		Object value = entitymap.get(keyCode);

		if (value == null) {
			log.info("value is NULL for key " + keyCode);
			return DEFAULT;
		}

		if (value.getClass().equals(BaseEntity.class)) {

			BaseEntity be = (BaseEntity) value;
			String attributeCode = entityArr[1];

			if (attributeCode.equals("PRI_CODE")) {
				log.debug("context: " + keyCode + ", attr: " + attributeCode + ", value: " + be.getCode());
				return be.getCode();
			}

			EntityAttribute ea = beaUtils.getEntityAttribute(be.getRealm(), be.getCode(), attributeCode, true, true);
			Object attributeValue = ea.getValue();
			log.debug("context: " + keyCode + ", attr: " + attributeCode + ", value: " + attributeValue);

			Matcher matchFormat = null;
			if (entityArr != null && entityArr.length > 2) {
				matchFormat = FORMAT_PATTERN_VARIABLE.matcher(entityArr[2]);
			}

			if (attributeValue instanceof org.javamoney.moneta.Money money) {

				log.debug("This is a Money attribute");
				DecimalFormat df = new DecimalFormat("#.00");

				return df.format(money.getNumber()) + " " + money.getCurrency();

			} else if (attributeValue instanceof java.time.LocalDateTime ldtValue) {
				/*
				   If the date-related mergeString needs to format to a particultar 
				   format -> we split the date-time related merge text to merge 
				   into 3 components: BE.PRI.TimeDateformat... becomes [BE, PRI...]
				   1st component -> BaseEntity code 
				   2nd component -> attribute code 
				   3rd component -> (date-Format)
				   */
				if (matchFormat != null && matchFormat.find()) {
					log.debug("Datetime attribute " + attributeCode + " needs formatting. Format is " + entityArr[2]);
					return TimeUtils.formatDateTime(ldtValue, matchFormat.group(1));
				} else {
					log.debug("DateTime attribute " + attributeCode + " does NOT need formatting");
					return ldtValue;
				}

			} else if (attributeValue instanceof java.time.LocalDate ldValue) {

				if (matchFormat != null && matchFormat.find()) {
					log.debug("Date attribute " + attributeCode + " needs formatting. Format is " + entityArr[2]);
					return TimeUtils.formatDate(ldValue, matchFormat.group(1));
				} else {
					log.debug("Date attribute " + attributeCode + " does NOT need formatting");
					return ldValue;
				}

			} else if (attributeValue instanceof java.lang.String sValue) {

				String result = null;
				if (matchFormat != null && matchFormat.find()) {
					result = getFormattedString(sValue, matchFormat.group(1));
					log.debug("String attribute " + attributeCode + " needs formatting. Format is " + entityArr[2] + ", Result is " + result);
				} else {
					result = beaUtils.getEntityAttribute(be.getRealm(), be.getCode(), attributeCode).getValueString();
					log.debug("String attribute " + attributeCode + " does NOT need formatting. Result is " + result);
				}
				return result;

			} else if (attributeValue instanceof java.lang.Boolean bValue) {
				return bValue;
			} else if (attributeValue instanceof java.lang.Integer iValue) {
				return iValue;
			} else if (attributeValue instanceof java.lang.Long lValue) {
				return lValue;
			} else if (attributeValue instanceof java.lang.Double dValue) {
				return dValue;
			} else {
				return be.findEntityAttribute(attributeCode).get().getValueString();
			}

		} else if (value.getClass().equals(String.class)) {
			return value;
		}

		return DEFAULT;	
	}

	/**
	* Check to see if all contexts are present.
	*
	* @param mergeStr the mergeStr to check contexts for
	* @param templateEntityMap the mergeStr to check contexts with
	* @return Boolean
	 */
	public Boolean contextsArePresent(String mergeStr, Map<String, Object> templateEntityMap) {
		
		if (mergeStr != null) {

		Matcher match = PATTERN_MATCHER.matcher(mergeStr);
		Matcher matchVariables = PATTERN_VARIABLE.matcher(mergeStr);

		if (templateEntityMap != null && templateEntityMap.size() > 0) {

			while (match.find()) {

				Object mergedObject = wordMerge(match.group(1), templateEntityMap);
				if (mergedObject == null || mergedObject.toString().isEmpty()) {
					return false;
				}
			}

			while(matchVariables.find()) {

				Object mergedText = templateEntityMap.get(matchVariables.group(1));
				if (mergedText == null) {
					return false;
				}
			}
		}

		} else {
			log.warn("mergeStr is NULL");
		}
		return true;
	}

	
	/** 
	 * @param mergeStr the mergeStr to check
	 * @return Boolean
	 */
	public Boolean requiresMerging(String mergeStr) {

		if (mergeStr == null) {
			log.warn("mergeStr is NULL");
			return null;
		}

		Matcher match = PATTERN_MATCHER.matcher(mergeStr);
		Matcher matchVariables = PATTERN_VARIABLE.matcher(mergeStr);

		return (match.find() || matchVariables.find());
	}

	/**
	 * This is a utility used to format strings during merging.
	 * Feel free to add some cool little string formatting tools below.
	 *
	 * Author - Jasper Robison (27/07/21)
	 *
	 * @param stringToBeFormatted		The string we want to format
	 * @param format					how it should be formatted (can be dot seperated string for multiple)
	 *
	 * @return String The formatted string.
	 */
	public String getFormattedString(String stringToBeFormatted, String format) {

		if (stringToBeFormatted != null && format != null) {
			String[] formatCommands = format.split("\\.");

			for (String cmd : formatCommands) {
				// A nice little clean up command for attribute values
				if (cmd.equals("CLEAN")) {
					stringToBeFormatted = stringToBeFormatted.replace("\"", "").replace("[", "").replace("]", "").replace(" ", "");
				}
				// A nice little substring command
				if (cmd.startsWith("SUBSTRING(")) {
					String[] subStringField = cmd.replace("SUBSTRING", "").replace("(", "").replace(")", "").replace(" ", "").split(",");
					Integer begin = Integer.valueOf(subStringField[0]);
					Integer end = subStringField.length > 1 ? Integer.valueOf(subStringField[1]) : null;
					if (end != null) {
						stringToBeFormatted = stringToBeFormatted.substring(begin, end);
					} else {
						stringToBeFormatted = stringToBeFormatted.substring(begin);
					}
				}
			}
			return stringToBeFormatted;
		}
		return null;
	}

	/**
	 * This method is used to find any associated contexts. 
	 * This allows us to provide a default set of context associations
	 * that the system can fetch for us in order to reduce code in rules 
	 * and other areas.
	 *
	 * Author - Jasper Robison (30/09/2021)
	 *
	 * @param	beUtils					Standard genny utility
	 * @param	ctxMap					the context map to add to, and fetch associations from.
	 * @param	contextAssociationJson	the json instructions for fetching associations.
	 * @param	overwrite				should the function overwrite any already existing contexts
	 */
	public void addAssociatedContexts(BaseEntityUtils beUtils, Map<String, Object> ctxMap, String contextAssociationJson, boolean overwrite) {

		// Enter Try-Catch for better error logging
		try {

			// convert to JsonObject for easier processing
			JsonReader reader = Json.createReader(new StringReader(contextAssociationJson));
			JsonObject ctxAssocJson = reader.readObject();
			reader.close();

			JsonArray ctxAssocArray = ctxAssocJson.getJsonArray("associations");

			for (Object obj : ctxAssocArray) {

				JsonObject ctxAssoc = (JsonObject) obj;

				// These are the required params in the json
				String code = ctxAssoc.getString("code");
				String sourceCode = ctxAssoc.getString("sourceCode");

				if (code == null) {
					log.error("Bad code field in " + contextAssociationJson);
					return;
				}
				if (sourceCode == null) {
					log.error("Bad sourceCode field in " + contextAssociationJson);
					return;
				}

				// Check to see if overwriting context is ok
				if (!ctxMap.containsKey(code) || (ctxMap.containsKey(code) && overwrite)) {

					String[] sourceArray = sourceCode.split("\\.");
					String parent = sourceArray[0];

					BaseEntity parentBE = (BaseEntity) ctxMap.get(parent);
					BaseEntity assocBE = null;

					// Iterate through attribute links to get BE
					for (int i = 1; i < sourceArray.length; i++) {

						String attributeCode = sourceCode.split("\\.")[i];

						// Grab Parent from map so we can fetch associated entity
						assocBE = beUtils.getBaseEntityFromLinkAttribute(parentBE, attributeCode);
						if (assocBE != null) {
							parentBE = assocBE;
						} else {
							log.error("Found a NULL BE for sourceCode = " + sourceCode + ", attributeCode = " +attributeCode);
						}
					}

					// Add the context map if associated be is found
					if (assocBE != null) {
						ctxMap.put(code, assocBE);
					} else {
						log.error("Associated BE not found for " + parent + "." + sourceCode);
					}
				}

			}
		} catch (Exception e) {
			log.error("Something is wrong with the context association JSON!!!!!");
		}
	}

}
