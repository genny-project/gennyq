package life.genny.gadaq.search;

import life.genny.kogito.common.service.FilterService.Options;
import life.genny.qwandaq.constants.FilterConst;
import life.genny.qwandaq.entity.search.trait.Operator;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventMessageUtils {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());
    static Jsonb jsonb = JsonbBuilder.create();
    public static final String NAME = "name";
    public static final String ITEMS = "items";
    public static final String DATA = "data";
    public static final String VALUE = "value";
    public static final String PARENT_CODE = "parentCode";
    public static final String SELECTED_OPTION = "selectedOption";
    public static final String SEARCH_NAME = "SEARCH_NAME";
    public static final String SEARCH_CODE = "SEARCH_CODE";
    public static final String DATA_TYPE = "data_type";

    /**
     * Get name attribute of message
     * @param msg Message object
     * @return name attribute of message
     */
    public static String getName(JsonObject msg) {
        return getValueByCode(msg, NAME);
    }

    /**
     * Get message token
     * @param msg Message object
     * @return Token
     */
    public static String getToken(JsonObject msg) {
        return getValueByCode(msg, FilterConst.TOKEN);
    }

    /**
     * Get message code
     * @param msg Message
     * @return Code
     */
    public static String getCode(JsonObject msg) {
        return getValueByCode(msg,FilterConst.CODE);
    }

    /**
     * Get message code
     * @param msg Message
     * @return Code
     */
    public static String getCode(String msg) {
        JsonObject json = jsonb.fromJson(msg, JsonObject.class);
        return getValueByCode(json,FilterConst.CODE);
    }

    /**
     * Get message target code
     * @param msg Message
     * @return Target code
     */
    public static String getTargetCode(JsonObject msg) {
        return getValueByCode(msg, FilterConst.TARGETCODE);
    }

    /**
     * Get message target codes
     * @param msg Message
     * @return Target code
     */
    public static List getTargetCodes(JsonObject msg) {
        if(msg.containsKey(FilterConst.TARGETCODES)) {
            return (List) msg.get(FilterConst.TARGETCODES);
        }
        return new ArrayList();
    }

    /**
     * Get message value
     * @param msg Message
     * @return Message Value
     */
    public static String getValue(JsonObject msg) {
        return getValueByCode(msg, FilterConst.VALUE);
    }

    /**
     * Get attribute code
     * @param msg Message
     * @return Attribute code
     */
    public static String getAttributeCode(JsonObject msg) {
        return getValueByCode(msg, FilterConst.ATTRIBUTECODE);
    }

    /**
     * Get name attribute of message
     * @param msg Message object
     * @return name attribute of message
     */
    public static String getSearchName(JsonObject msg) {
        return getValueByCode(msg, SEARCH_NAME);
    }

    /**
     * Get name attribute of message
     * @param msg Message object
     * @return name attribute of message
     */
    public static String getSearchCode(JsonObject msg) {
        return getValueByCode(msg, SEARCH_CODE);
    }

    /**
     * Get name attribute of message
     * @param msg Message object
     * @return name attribute of message
     */
    public static String getFilterCode(JsonObject msg) {
        return getValueByCode(msg, SELECTED_OPTION);
    }

    /**
     * Get Parent code
     * @param msg Message object
     * @return name attribute of message
     */
    public static String getParentCode(JsonObject msg) {
        return getValueByCode(msg, PARENT_CODE);
    }

    /**
     * Get data type of message
     * @param msg Message object
     * @return name attribute of message
     */
    public static String getDataType(JsonObject msg) {
        return getValueByCode(msg, DATA_TYPE);
    }

    /**
     * Get name attribute of message
     * @param json Json object
     * @return name attribute of message
     */
    public static String getValueByCode(JsonObject json,String code) {
        String result = "";
        try {
            result = getSafeValueByCode(json, code);
            if (result.isEmpty()) {
                /* check items */
                JsonArray items = json.getJsonArray(ITEMS);
                JsonObject dataJson = null;
                if (items!=null && items.size() > 0) {
                    dataJson = items.getJsonObject(0);
                }
                /* check data */
                if(dataJson == null) {
                    dataJson = json.getJsonObject(DATA);
                }
                /* Check value */
                if(!dataJson.containsKey(code)) {
                    if(dataJson.get(VALUE).getValueType().equals(JsonValue.ValueType.STRING)) {
                        String attValue = dataJson.getString(VALUE);
                        dataJson = jsonb.fromJson(attValue, JsonObject.class);
                    } else if(dataJson.get(VALUE).getValueType().equals(JsonValue.ValueType.OBJECT)) {
                        dataJson = dataJson.getJsonObject(VALUE);
                    }
                }

                if(dataJson.containsKey(code)) {
                    result = dataJson.getString(code);
                }
            }

        }catch(Exception ex) {}

        return result;
    }

    /**
     *
     * @param json Json Object
     * @param eventCode Event Code
     * @return Value
     */
    public static String getSafeValueByCode(JsonObject json, String eventCode) {
        String value = "";
        if(json == null) return value;
        if(json.containsKey(eventCode)) {
            value = json.get(eventCode).toString();
        }
        value = StringUtils.stripStart(value,"\"");
        value = StringUtils.stripEnd(value,"\"");
        return value;
    }

    /**
     * Strip search base entity code without jti
     * @param orgSbe Original search base entity code
     * @return Search base entity code without jti
     */
    public static String getCleanSBECode(String orgSbe) {
        String sbe = "";

        if (orgSbe.indexOf("-") > -1) {
            int index = orgSbe.lastIndexOf("_");
            sbe = orgSbe.substring(0, index);

            return sbe;
        }

        return orgSbe;
    }


    public static Map<String,Map<String, String>> parseFilterMessage(String event) {
        Map<String,Map<String, String>> map = jsonb.fromJson(event, Map.class);
        return map;
    }

    /**
     * Parse string to local date time
     * @param strDate Date String
     * @return Return local date time
     */
    public LocalDateTime parseStringToDate(String strDate){
        LocalDateTime localDateTime = null;
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(strDate);
            localDateTime = zdt.toLocalDateTime();
        }catch(Exception ex) {
            log.info(ex);
        }

        return localDateTime;
    }

    /**
     * Being whether date time is selected or not
     * @param questionCode Question code
     * @return Being whether date time is selected or not
     */
    public boolean isDateTimeSelected(String questionCode){
        boolean isDateTime = false;

        //date,time
        if(questionCode.equalsIgnoreCase(FilterConst.QUE_FILTER_VALUE_DATE)){
            return true;
        } else if(questionCode.equalsIgnoreCase(FilterConst.QUE_FILTER_VALUE_DATETIME)){
            return true;
        } else if(questionCode.equalsIgnoreCase(FilterConst.QUE_FILTER_VALUE_TIME)){
            return true;
        }

        return isDateTime;
    }

    /**
     * Return Whether filter tag or not
     * @param code
     * @return Whether filter tag or not
     */
    public boolean isRowFilterTag(String code) {
        if(code.startsWith(FilterConst.ROW_FILTER_PREF)) {
            return true;
        }

        return false;
    }

    /**
     * Return clean map  of filter parameters
     * @param value Event value
     * @return clean map  of filter parameters
     */
    public static Map<String, Map<String, String>> getCleanFilterParamsByMap(String value) {
        Map<String,Map<String, String>> params  = parseFilterMessage(value);
        params.remove(SEARCH_NAME);
        params.remove(SEARCH_CODE);
        return params;
    }


    /**
     * Return clean string  of filter parameters
     * @param value Event value
     * @return clean string  of filter parameters
     */
    public static String getCleanFilterParamsByString(String value) {
        Map<String,Map<String, String>> params  = parseFilterMessage(value);
        params.remove(SEARCH_NAME);
        params.remove(SEARCH_CODE);
        return jsonb.toJson(params);
    }

}
