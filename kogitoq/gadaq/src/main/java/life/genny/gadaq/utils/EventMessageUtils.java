package life.genny.gadaq.utils;

import life.genny.kogito.common.service.SearchService;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.entity.search.trait.Operator;
import org.jboss.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonObject;
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

    /**
     * Get name attribute of message
     * @param data Data
     * @return name attribute of message
     */
    public static String getName(String data) {
        JsonObject json = jsonb.fromJson(data, JsonObject.class);
        String result = getSafeValueByCode(json,NAME);
        if(result.isEmpty()) {
            //check items
            JsonArray items = json.getJsonArray(ITEMS);
            if(items.size() > 0) {
                JsonObject first = items.getJsonObject(0);
                String value  = first.getString(GennyConstants.VALUE);
                JsonObject valJson = jsonb.fromJson(value, JsonObject.class);
                result = valJson.getString(NAME);
                return result;
            }
        }
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
        return value;
    }


    /**
     *  parse string to map object
     * @param data json string
     * @param searchOptions PAGINATION or SORT or SEARCH
     * @return return map object
     */
    public static Map<String, Object> parseEventMessage(String data, SearchService.SearchOptions searchOptions){
        Map<String, Object> map = new HashMap<>();

        try {
            JsonObject eventJson = jsonb.fromJson(data, JsonObject.class);

            if(searchOptions.equals(SearchService.SearchOptions.PAGINATION)) {
                JsonObject jsonObject = eventJson.getJsonObject(DATA);

                map.put(GennyConstants.CODE,jsonObject.getString(GennyConstants.CODE));
                map.put(GennyConstants.TARGETCODE, jsonObject.getString(GennyConstants.TARGETCODE));
                map.put(GennyConstants.TOKEN, eventJson.getString(GennyConstants.TOKEN));

                return map;
            }else if(searchOptions.equals(SearchService.SearchOptions.SEARCH)) { //sorting, searching text
                map.put(GennyConstants.TOKEN, eventJson.getString(GennyConstants.TOKEN));

                JsonArray items = eventJson.getJsonArray(ITEMS);
                if(items.size() > 0){
                    JsonObject jsonObject = items.getJsonObject(0);
                    map.put(GennyConstants.CODE,jsonObject.getString(GennyConstants.CODE));
                    map.put(GennyConstants.ATTRIBUTECODE,jsonObject.getString(GennyConstants.ATTRIBUTECODE));
                    String strTargetcode = jsonObject.getString(GennyConstants.TARGETCODE);
                    String[] splitted = strTargetcode.split("\"*\"");
                    List<String> targetCodes = new ArrayList();
                    for(String str : splitted){
                        if(str.startsWith(GennyConstants.CACHING_SBE)) {
                            targetCodes.add(str);
                        }
                    }
                    map.put(GennyConstants.TARGETCODE,jsonObject.getString(GennyConstants.TARGETCODE));
                    //It is used for buckets
                    map.put(GennyConstants.TARGETCODES,targetCodes);

                    map.put(GennyConstants.VALUE, jsonObject.getString(GennyConstants.VALUE));

                    return map;
                }
            } else if(searchOptions.equals(SearchService.SearchOptions.FILTER)) {
                JsonObject jsonObject = eventJson.getJsonObject("data");

                map.put(GennyConstants.CODE,jsonObject.getString(GennyConstants.CODE));
                map.put(GennyConstants.TARGETCODE, jsonObject.getString(GennyConstants.TARGETCODE));
                map.put(GennyConstants.TOKEN, eventJson.getString(GennyConstants.TOKEN));
                if(jsonObject.containsKey(GennyConstants.VALUE)) {
                    map.put(GennyConstants.VALUE, jsonObject.getString(GennyConstants.VALUE));
                }
                return map;
            }
        } catch (Exception ex) {}
        return map;
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
     * Get Search filter by filter value
     * @param filterVal Filter value
     * @return Get Search filter by filter value
     */
    public Operator getOperatorByVal(String filterVal){
        if(filterVal.equalsIgnoreCase(GennyConstants.SEL_GREATER_THAN)){
            return Operator.GREATER_THAN;
        }

        if(filterVal.equalsIgnoreCase(GennyConstants.SEL_GREATER_THAN_OR_EQUAL_TO)){
            return Operator.GREATER_THAN_OR_EQUAL;
        }

        if(filterVal.equalsIgnoreCase(GennyConstants.SEL_LESS_THAN)){
            return Operator.LESS_THAN;
        }

        if(filterVal.equalsIgnoreCase(GennyConstants.SEL_LESS_THAN_OR_EQUAL_TO)){
            return Operator.LESS_THAN_OR_EQUAL;
        }

        if(filterVal.equalsIgnoreCase(GennyConstants.SEL_EQUAL_TO)){
            return Operator.EQUALS;
        }

        if(filterVal.equalsIgnoreCase(GennyConstants.SEL_NOT_EQUAL_TO)){
            return Operator.NOT_EQUALS;
        }

        if(filterVal.equalsIgnoreCase(GennyConstants.SEL_EQUAL_TO)){
            return Operator.EQUALS;
        }

        if(filterVal.equalsIgnoreCase(GennyConstants.SEL_NOT_EQUAL_TO)){
            return Operator.NOT_EQUALS;
        }

        if(filterVal.equalsIgnoreCase(GennyConstants.SEL_LIKE)){
            return Operator.LIKE;
        }

        if(filterVal.equalsIgnoreCase(GennyConstants.SEL_NOT_LIKE)){
            return Operator.NOT_LIKE;
        }

        return Operator.EQUALS;
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
        if(questionCode.equalsIgnoreCase(GennyConstants.QUE_FILTER_VALUE_DATE)){
            return true;
        } else if(questionCode.equalsIgnoreCase(GennyConstants.QUE_FILTER_VALUE_DATETIME)){
            return true;
        } else if(questionCode.equalsIgnoreCase(GennyConstants.QUE_FILTER_VALUE_TIME)){
            return true;
        }

        return isDateTime;
    }

}
