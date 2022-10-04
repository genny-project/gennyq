package life.genny.gadaq.utils;

import life.genny.gadaq.utils.EventMessageUtils;
import life.genny.kogito.common.service.SearchService;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.AnswerLinkId;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.AttributeLink;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.gadaq.cache.SearchCaching;

@ApplicationScoped
public class FilterUtils {
    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

    static Jsonb jsonb = JsonbBuilder.create();

    @Inject
    BaseEntityUtils beUtils;

    @Inject
    QwandaUtils qwandaUtils;

    @Inject
    UserToken userToken;

    @Inject
    SearchService searchService;

    @Inject
    DatabaseUtils databaseUtils;

//    @Inject
//    FilterParamsUtils filterParamsUtils;


    public static final String EVT_QUE_TREE_PREFIX = "QUE_TREE_ITEM_";
    public static final String EVT_QUE_TABLE_PREFIX = "QUE_TABLE_";

    public static final String DATE = "DATE";
    public static final String DATETIME = "DATETIME";
    public static final String TIME = "TIME";

    //select box option
    public static final String PRI_ADDRESS_COUNTRY = "PRI_ADDRESS_COUNTRY";
    public static final String PRI_ASSOC_COMP_INTERNSHIP = "PRI_ASSOC_COMP_INTERNSHIP";
    public static final String PRI_INTERNSHIP_TYPE = "PRI_INTERNSHIP_TYPE";
    public static final String PRI_DJP_AGREE = "PRI_DJP_AGREE";

    //Dropdown links
    public static final String LNK_CORE = "LNK_CORE";
    public static final String COUNTRY = "COUNTRY";
    public static final String COMPLETE_INTERNSHIP = "COMPLETE_INTERNSHIP";
    public static final String YES_NO = "YES_NO";
    public static final String INTERNSHIP_TYPE = "INTERNSHIP_TYPE";

    //bucket
    public static final String PREF_CPY = "CPY_";
    public static final String QUE_TAB_BUCKET_VIEW = "QUE_TAB_BUCKET_VIEW";
    public static final String QUE_PREF = "QUE_";

    public static final String FLT_PREF = "FLT_";
    public static final String PRI_PREFIX = "PRI_PREFIX";
    public static final String ATT_PREF = "LNK_";
//    public static final String SAVED_SEARCHES = "SAVED_SEARCHES";
    public static final String SAVED_SEARCHES = "S7_SEARCHES";
    public  DataType DataTypeStr = DataType.getInstance("life.genny.qwanda.entity.BaseEntity");


    /**
     * Return search base entity by message code
     * @param code Message Code
     * @return  Return search base entity
     */
    public String getSearchEntityCodeByMsgCode(String code) {
        String sbeCode = code.replaceFirst(EVT_QUE_TREE_PREFIX,"");
        sbeCode = GennyConstants.CACHING_SBE + sbeCode.replaceFirst(EVT_QUE_TABLE_PREFIX,"")
                                                .replaceFirst(QUE_PREF,"");

        return sbeCode;
    }

    /**
     * Check code whether is filter select question or not
     * @param filterValue Filter Value
     * @return Being filter option
     */
    public  boolean isFilterSelectQuestion(String filterValue) {
        boolean result = false;
        if(filterValue.indexOf(PRI_ADDRESS_COUNTRY) > -1
            || filterValue.indexOf(PRI_ASSOC_COMP_INTERNSHIP) > -1
            || filterValue.indexOf(PRI_DJP_AGREE) > -1
            || filterValue.indexOf(PRI_INTERNSHIP_TYPE) > -1)
            return true;

        return result;
    }

    /**
     * Check code whether is filter value or not
     * @param code Message Code
     * @return Being filter value
     */
    public  boolean isFilterValue(String code) {
        boolean result = false;
        if(code.startsWith(GennyConstants.QUE_FILTER_VALUE))
            return true;

        return result;
    }

    /**
     * Check code whether is filter submit or not
     * @param code Message Code
     * @return Being filter submit
     */
    public  boolean isFilterSubmit(String code) {
        boolean result = false;
        if(code.equalsIgnoreCase(GennyConstants.QUE_SUBMIT))
            return true;

        return result;
    }

    /**
     * Check code whether is quesion showing filter box or not
     * @param code Message Code
     * @return Being question showing filter box
     */
    public  boolean isValidFilterBox(String code) {
        boolean result = false;

        if(code.startsWith(EVT_QUE_TREE_PREFIX) || code.startsWith(EVT_QUE_TABLE_PREFIX))
            return true;

        return result;
    }

    /**
     * Return value by event code in safe way
     * @param attrMap Attribute Map
     * @param eventCode Event code
     * @return Return value by event code
     */
    public String getSafeValueByCode(Map<String, Object> attrMap, String eventCode) {
        String value = "";
        if(attrMap == null) return value;
        if(attrMap.containsKey(eventCode)) {
            value = attrMap.get(eventCode).toString();
        }
        return value;
    }

    /**
     * Being filter optio whether selected or not
     * @param eventCode Event Code
     * @param attributeCode Attribute Code
     * @return filter option selected
     */
    public boolean isFilerColumnSelected(String eventCode, String attributeCode) {
        boolean result = false;
        if(eventCode.startsWith(GennyConstants.QUE_FILTER_COLUMN)
                && attributeCode.startsWith(GennyConstants.LNK_FILTER_COLUMN))
            return true;

        return result;
    }

    /**
     * Being filter optio whether selected or not
     * @param eventCode Event Code
     * @param attributeCode Attribute Code
     * @return filter option selected
     */
    public boolean isFilerOptionSelected(String eventCode, String attributeCode) {
        boolean result = false;
        if(eventCode.startsWith(GennyConstants.QUE_FILTER_OPTION)
                && attributeCode.startsWith(GennyConstants.LNK_FILTER_OPTION))
            return true;

        return result;
    }

    /**
     * Return question code by filter code
     * @param filterVal Filter Code
     * @return Return question code by filter code
     */
    public String getQuestionCodeByFilterValue(String filterVal){
        String questionCode = "";
        String suffix = getLastSuffixCodeByFilterValue(filterVal);

        if(filterVal.indexOf(PRI_ADDRESS_COUNTRY) > -1){
            return GennyConstants.QUE_FILTER_VALUE_COUNTRY;
        } else if(filterVal.indexOf(PRI_ASSOC_COMP_INTERNSHIP) > -1){
            return GennyConstants.QUE_FILTER_VALUE_ACADEMY;
        } else if(filterVal.indexOf(PRI_DJP_AGREE) > -1){
            return GennyConstants.QUE_FILTER_VALUE_DJP_HC;
        } else if(filterVal.indexOf(PRI_INTERNSHIP_TYPE) > -1){
            return GennyConstants.QUE_FILTER_VALUE_INTERNSHIP_TYPE;
        }
        //date,time
        if(suffix.equalsIgnoreCase(DATE)){
            return GennyConstants.QUE_FILTER_VALUE_DATE;
        } else if(suffix.equalsIgnoreCase(DATETIME)){
            return GennyConstants.QUE_FILTER_VALUE_DATETIME;
        } else if(suffix.equalsIgnoreCase(TIME)){
            return GennyConstants.QUE_FILTER_VALUE_TIME;
        }

        //text
        if(!filterVal.isEmpty()) {
           return GennyConstants.QUE_FILTER_VALUE_TEXT;
        }
        return questionCode;
    }

    /**
     * Return link value based on event value
     * @param value Event Value
     * @return Return link value based on event value
     */
    public String getLinkVal(String value) {
        String lnkVal = "";
        if(value.indexOf(PRI_ADDRESS_COUNTRY) > -1){
            lnkVal = COUNTRY;
        } else if(value.indexOf(PRI_ASSOC_COMP_INTERNSHIP) > -1){
            lnkVal = COMPLETE_INTERNSHIP;
        } else if(value.indexOf(PRI_DJP_AGREE) > -1){
            lnkVal = YES_NO;
        } else if(value.indexOf(PRI_INTERNSHIP_TYPE) > -1){
            lnkVal = INTERNSHIP_TYPE;
        }

        return lnkVal;
    }

    /**
     * Return the last suffix code
     * @param filterVal Filter Value
     * @return Return the last suffix code
     */
    public String getLastSuffixCodeByFilterValue(String filterVal) {
        String lastSuffix = "";
        int lastIndex = filterVal.lastIndexOf("_");
        if(lastIndex > -1) {
            lastSuffix = filterVal.substring(lastIndex, filterVal.length());
            lastSuffix = lastSuffix.replaceFirst("_","");
            lastSuffix = lastSuffix.replaceFirst("\"]","");
        }
        return lastSuffix;
    }

    /**
     * Being whether event is pagination event or not
     * @param attrs Attribute Map
     * @return Being whether event is pagination event or not
     */
    public boolean isPaginationEvent(Map<String, Object> attrs) {
        if(attrs != null && (attrs.get(GennyConstants.CODE).toString().equalsIgnoreCase(GennyConstants.PAGINATION_NEXT)
                || attrs.get(GennyConstants.CODE).toString().equalsIgnoreCase(GennyConstants.PAGINATION_PREV))) {
            return true;
        }

        return false;
    }

    /**
     * Being whether bucket filter select options or not
     * @param eventCode Event code
     * @param targetCode Target code
     * @param value Event value
     * @return Being whether bucket filter select options or not
     */
    public boolean isQuickSearchSelectOptions(String eventCode, String targetCode, String value) {
        boolean result = false;
        if(eventCode.startsWith(GennyConstants.QUE_SELECT_INTERN)
                && targetCode.startsWith(GennyConstants.BKT_APPLICATIONS)
                && !value.isEmpty())
            return true;

        return result;
    }


    /**
     * Being whether bucket filter select options or not
     * @param eventCode Event code
     * @param attrCode Attribute code
     * @param targetCode Target code
     * @param value Event value
     * @return Being whether bucket filter select options or not
     */
    public boolean isQuickSearchSelectChanged(String eventCode, String attrCode, String targetCode, String value) {
        boolean result = false;
        String newVal =  getStripSelectValue(value);

        if(eventCode.startsWith(GennyConstants.QUE_SELECT_INTERN)
                && attrCode.startsWith(GennyConstants.LNK_PERSON)
                && targetCode.startsWith(GennyConstants.BKT_APPLICATIONS)
                && newVal.startsWith(PREF_CPY))
            return true;

        return result;
    }

    /**
     * Get stripped select value
     * @param value Select value chosen
     * @return Select value
     */
    public String getStripSelectValue(String value) {
        String finalVal = value.replace("\"","")
                .replace("[","").replace("]", "");

        return finalVal;
    }

    /**
     * Return base entity name
     * @param baseEntityCode Base entity code
     * @return Base entity name
     */
    public String getBaseNameByCode(String baseEntityCode) {
        return beUtils.getBaseEntityByCode(baseEntityCode).getName();
    }

    /**
     * Being whether sorting  or not
     * @param attrCode Attribute code
     * @param targetCode Target code
     * @return Being whether sorting  or not
     */
    public boolean isSorting(String attrCode, String targetCode) {
        if(attrCode.matches("SRT_.*") && targetCode.matches("SBE_.*")) {
            return true;
        }

        return false;
    }

    /**
     * Being whether search text  or not
     * @param attrCode Attribute code
     * @return Being whether search text  or not
     */
    public boolean isSearchText(String attrCode) {
        if(attrCode.equalsIgnoreCase(GennyConstants.SEARCH_TEXT)) {
            return true;
        }

        return false;
    }


    /**
     * Get message token
     * @param msg Message
     * @return Token
     */
    public String getToken(Map<String, Object> msg) {
        return getSafeValueByCode(msg, GennyConstants.TOKEN);
    }

    /**
     * Get message code
     * @param msg Message
     * @return Code
     */
    public String getCode(Map<String, Object> msg) {
        return getSafeValueByCode(msg,GennyConstants.CODE);
    }


    /**
     * Get message target code
     * @param msg Message
     * @return Target code
     */
    public String getTargetCode(Map<String, Object> msg) {
        return getSafeValueByCode(msg, GennyConstants.TARGETCODE);
    }

    /**
     * Get message target codes
     * @param msg Message
     * @return Target code
     */
    public List getTargetCodes(Map<String, Object> msg) {
        return (List) msg.get(GennyConstants.TARGETCODES);
    }

    /**
     * Get message value
     * @param msg Message
     * @return Message Value
     */
    public String getValue(Map<String, Object> msg) {
        return getSafeValueByCode(msg, GennyConstants.VALUE);
    }

    /**
     * Get attribute code
     * @param msg Message
     * @return Attribute code
     */
    public String getAttributeCode(Map<String, Object> msg) {
        return getSafeValueByCode(msg, GennyConstants.ATTRIBUTECODE);
    }

//    public Map<String, String> filterParams() {
//        return filterParamsUtils.getFilterParams();
//    }
//
//    public Map<String,Map<String, String>> listFilterParams() {
//        return filterParamsUtils.getListFilterParams();
//    }

    /**
     * Set question code to filter column
     * @param queCode question Code
     */
    public void setFilterColumn(String queCode) {
//        searchService.setFilterParamValByKey(GennyConstants.QUE_FILTER_COLUMN, queCode);
//        filterParams().put(GennyConstants.QUE_FILTER_COLUMN, queCode);
    }

    /**
     * Set value to filter option
     * @param value Filter option
     */
    public void setFilterOption(String value) {
//        searchService.setFilterParamValByKey(GennyConstants.QUE_FILTER_OPTION, value);
//        filterParams().put(GennyConstants.QUE_FILTER_OPTION, value);
    }

    /**
     * Set value to filter value
     * @param value Filter value
     */
    public void setFilterValue(String value) {
//        searchService.setFilterParamValByKey(GennyConstants.QUE_FILTER_VALUE, value);
//        filterParams().put(GennyConstants.QUE_FILTER_VALUE, value);
    }

    /**
     * Set value to filer attribute code
     * @param value Filter attribute code
     */
    public void setFilterAttributeCode(String value) {
//        searchService.setFilterParamValByKey(GennyConstants.ATTRIBUTECODE, value);
    }

    /**
     * Check code whether is quesion showing filter box or not
     * @param code Message Code
     * @return Being question showing filter box
     */
    public  boolean isValidBucketFilterBox(String code) {
        boolean result = false;

        if(code.startsWith(QUE_TAB_BUCKET_VIEW))
            return true;

        return result;
    }

    /**
     * Return filter attribute code
     * @return Filter attribute code
     */
    public String getFilterAttributeCode() {
//        return searchService.getFilterParamValByKey(GennyConstants.ATTRIBUTECODE);
        return "";
    }

    /**
     *  Send  quick search and filter group
     * @param code Event code
     * @return Being whether it was sent or not
     */

    public boolean isNeededFilterAndQuickSearch(String code) {
        boolean result = searchService.isFilterTag(code) ||
                isValidFilterBox(code) || isValidBucketFilterBox(code)
                || isFilterSubmit(code);

        return result;
    }

    /**
     * Search quick text
     * @param msg Parsed message
     * @param value Message value
     * @param targetCode Target code
     */
    public void searchQuickText(Map<String, Object> msg, String value, String targetCode) {
        String attrCode = Attribute.PRI_NAME;
        String attrName = Operator.LIKE.toString();
        String text =  "%" + value.replaceFirst("!","") + "%";
        List<String> targetCodes = getTargetCodes(msg);

            // Go to bucket
        if (targetCodes.size() > 1) {
            searchService.handleBucketSearch(attrCode, attrName, text, targetCodes);
            // Go to search text
        }else {
            searchService.handleSortAndSearch(attrCode, attrName, text, targetCode, SearchService.SearchOptions.SEARCH);
            searchService.sendQuickSearch(GennyConstants.QUE_TABLE_FILTER_GRP,GennyConstants.QUE_SELECT_INTERN,
                    GennyConstants.LNK_PERSON, GennyConstants.BKT_APPLICATIONS);
        }
    }

    /**
     * Handle event when filter columns is selected
     * @param targetCode Target code
     * @param value Message value
     */
    public void selectFilerColumn(String targetCode, String value) {
        String queCode = getQuestionCodeByFilterValue(value);
        setFilterAttributeCode(value);
        setFilterColumn(queCode);

        searchService.sendQuickSearch(GennyConstants.QUE_TABLE_FILTER_GRP,GennyConstants.QUE_SELECT_INTERN,
                GennyConstants.LNK_PERSON, GennyConstants.BKT_APPLICATIONS);

//        searchService.sendFilterGroup(targetCode, GennyConstants.QUE_FILTER_GRP,queCode,true);
        searchService.sendFilterOption(queCode, targetCode);

        boolean isSelectBox = isFilterSelectQuestion(value);
        if(isSelectBox) {
            String linkVal = getLinkVal(value);
            searchService.sendFilterValue(GennyConstants.QUE_ADD_FILTER_GRP,queCode,
                    FilterUtils.LNK_CORE,linkVal);
        } else {
            setFilterValue("");
        }
    }

    /**
     * Handle event if selecting value in quick search
     * @param token Message token
     * @param attrCode Attribute code
     * @param attrName Attribute name
     * @param value Message value
     */
    public void selectQuickSearch(String token, String attrCode, String attrName,String value) {
        userToken.init(token);
        List<String> bucketCodes = searchService.getBucketCodesBySBE(SearchCaching.SBE_TAB_BUCKET_VIEW);
        String baseCode = getStripSelectValue(value);
        String newVal = getBaseNameByCode(baseCode);

        searchService.handleBucketSearch(attrCode, attrName, newVal, bucketCodes);
    }

    /**
     * Being whether saved search or not
     * @param code
     * @return
     */
    public boolean isSavedSearch(String code) {
        boolean result = true;

        return result;
    }

    /**
     * Save searches
     * @param name Event name
     */
    public void saveSearches(String token, String prefix, String name, Map<String,Map<String, String>> params) {
        userToken.init(token);
        String userCode = userToken.getUserCode();

        String attCode = ATT_PREF + SAVED_SEARCHES;
        Attribute attrFound = saveAttribute(attCode);

        //get the list of filter parameter from base entity attribute
        Map<String, List<Map<String,Map<String, String>>>> filterParams = getFilterParamsByBaseCode(name);
        List<Map<String,Map<String, String>>> listMapParams = new ArrayList<>();
        listMapParams.add(params);
        filterParams.put(name, listMapParams);

        //add more params to the list
        String strParams =  jsonb.toJson(filterParams);
        saveBaseEntity(attrFound, prefix,name,strParams);
    }

    /**
     * Save searches
     * @param name Event name
     */
    public void saveSearches(String token, String prefix, String name) {
        userToken.init(token);
        String userCode = userToken.getUserCode();

        String attCode = ATT_PREF + SAVED_SEARCHES;
        Attribute attrFound = saveAttribute(attCode);
//        saveBaseEntity(attrFound, prefix,name);
    }

    /**
     * Save base entity
     * @param att Attribute
     * @param prefix Definition prefix
     * @param name Search name
     */
//    public void saveBaseEntity(Attribute att, String prefix,String name) {
//        BaseEntity defBE = new BaseEntity(prefix);
//        String baseCode = prefix + UUID.randomUUID().toString();
//
//        //first attribute
//        Attribute attr = new Attribute(PRI_PREFIX, prefix, DataTypeStr);
//        EntityAttribute ea = new EntityAttribute(defBE, attr, 1.0);
//        ea.setValue(prefix);
//        ea.setValueBoolean(true);
//        defBE.addAttribute(attr, 1.0, prefix);
//        try {
//            BaseEntity target = beUtils.create(defBE, name, baseCode);
//            att.setId(5872L); // TODO
//
//            //get current list of filter parameters and add new filter
////            EntityAttribute newEA = target.addAttribute(att, 1.0, getFilterParamsToJsonString());
////            beUtils.updateBaseEntity(target);
//
//            //update user
////            BaseEntity source = beUtils.getUserBaseEntity();
////            EntityEntity ee = source.addTarget(target, newEA.getAttribute(),1.0, getFilterListInJson());
////            beUtils.updateBaseEntity(source);
//
//        }catch (Exception ex) {
//            log.error(ex);
//        }
//    }

    /**
     * Save base entity
     * @param att Attribute
     * @param prefix Definition prefix
     * @param name Search name
     * @param params Filter parameters
     */
    public void saveBaseEntity(Attribute att, String prefix,String name,String params) {
        BaseEntity source = beUtils.getUserBaseEntity();

        //first attribute
        EntityAttribute ea = new EntityAttribute(source, att, 1.0);
        ea.setAttributeCode(att.getCode());
        ea.setBaseEntityCode(source.getCode());
        ea.setAttribute(att);
        ea.setBaseEntity(source);

        ea.setValue(prefix);
        ea.setValueBoolean(true);
        ea.setValueString(params);

        try {
//            source.getBaseEntityAttributes().add(ea);
            Attribute newAtt = new AttributeLink(att.getCode(), att.getName());
            newAtt.setId(att.getId());

//          source.addAttribute(newAtt,1.0,params);

            source.addAttribute(newAtt,1.0,params);
            source.getLinks().stream().forEach( e -> {
                e.getPk().setAttribute(newAtt);
                e.getPk().setSource(source);
                e.getPk().setTargetCode(source.getCode());
            });

            beUtils.updateBaseEntity(source);
        }catch (Exception ex) {
            ex.printStackTrace();
            log.error(ex);
        }
    }

    /**
     * Save attribute code
     * @param attCode Attribute code
     * @return Return attribute
     */
    public Attribute saveAttribute(String attCode) {
        //add list filter parameter
        String attShort = attCode;
        Attribute attrFound = null;
        try {
            attrFound = qwandaUtils.getAttribute(attShort);
        }catch (Exception ex) {
            AttributeLink newAtt = new AttributeLink(attShort, attShort);
            attrFound = qwandaUtils.saveAttribute(newAtt);
        }

        return attrFound;
    }

    /**
     * Delete base entity
     * @param code Base entity
     */
    public void deleteBaseEntity(String code) {
        databaseUtils.deleteBaseEntity(userToken.getRealm(), code);
    }

//    /**
//     * Return json string with filter parameters
//     * @return json string
//     */
//    public String getFilterParamsToJsonString() {
//        //get the list of filter parameters from attribute LNK_SAVED_SEARCHES
//        String result = "";
//
//        return result;
//    }

    /**
     * Get the table of filter parameters
     * @param sbeCode Search base entity
     * @param code Filter code
     * @return Get the table of filter parameters
     */
    public Map<String,Map<String, String>> getFilterParamByBaseCode(String sbeCode,String code) {
        Map<String,Map<String, String>>  result =  new HashMap<>();

        String value = "";
        try {
            //get the latest filter of current base entity
            if(code.isEmpty()) {
                value = getLatestFilterCode(sbeCode);
                BaseEntity base = beUtils.getBaseEntityByCode(value);
                Set<EntityAttribute> entityAttributes = base.getBaseEntityAttributes();
                String attCode = ATT_PREF + SAVED_SEARCHES;
                Optional<EntityAttribute> option = entityAttributes.stream()
                        .filter( e-> e.getAttributeCode() == attCode).findFirst();

                if (option.isPresent()) {
                    value = option.get().getValue();
                }
            } else {
                // get the filter by base entity code
                BaseEntity base = beUtils.getBaseEntityByCode(code);
                Optional<String> option = base.getValue(ATT_PREF + SAVED_SEARCHES);
                if (option.isPresent()) {
                    value = option.get();
                }
            }

            result = jsonb.fromJson(value, Map.class);
        }catch(Exception ex) {
            log.info(ex);
        }

        return result;
    }

    /**
     * Get the table of filter parameters
     * Map <Search code , List of filter parameters>
//     * @param sbeCode Search base entity
//     * @param code Filter code
     * @return Get the table of filter parameters
     */
    public Map<String, List<Map<String,Map<String, String>>>> getFilterParamsByBaseCode(String name) {
        Map<String,List<Map<String,Map<String, String>>>>  result =  new HashMap<>();

        String value = "";
        try {
            //get the latest filter of current base entity
            if(name.isEmpty()) {
//                value = getLatestFilterCode(sbeCode);
//                BaseEntity base = beUtils.getBaseEntityByCode(value);
//                Set<EntityAttribute> entityAttributes = base.getBaseEntityAttributes();
//                String attCode = ATT_PREF + SAVED_SEARCHES;
//                Optional<EntityAttribute> option = entityAttributes.stream()
//                        .filter( e-> e.getAttributeCode() == attCode).findFirst();
//
//                if (option.isPresent()) {
//                    value = option.get().getValue();
//                }
            } else {
                // get the filter by base entity code
                BaseEntity base = beUtils.getBaseEntityByCode(name);
                Optional<String> option = base.getValue(ATT_PREF + SAVED_SEARCHES);
                if (option.isPresent()) {
                    value = option.get();
                }
            }

            result = jsonb.fromJson(value, Map.class);
        }catch(Exception ex) {
            log.info(ex);
        }

        return result;
    }

    /**
     * Get the latest filter code
     * @param sbeCode Search base entity code
     * @return The latest filter code
     */
    public String getLatestFilterCode(String sbeCode) {
        String filterCode = "";
        String fltCond = FLT_PREF + "%";
        List<BaseEntity> bases = searchService.getListDropdownItems(sbeCode,GennyConstants.PRI_NAME,
                                                    GennyConstants.VALUE,fltCond,true);
        if(bases.size() > 0) {
            return bases.get(0).getCode();
        }
        return filterCode;
    }

    /**
     * Handle filter event
     * @param event Event
     */
    public void handleFilterEvent(String event) {
        try {
            Map<String, Object> msg = EventMessageUtils.parseEventMessage(event,SearchService.SearchOptions.FILTER);

            String token = getToken(msg);
            String code = getCode(msg);
            String targetCode = getTargetCode(msg);
            String value = getValue(msg);
            String sbeCode = "";
            String queGroup = "";

            //init user token
            if(!token.isEmpty()) { userToken.init(token);}

            if(searchService.isFilterTag(code)) {
                // TODO
                String filterCode = "FLT_3B536C98-DE86-49AA-A850-DFA5158ADA4E";
                Map<String,Map<String, String>> listFilterParams = getFilterParamByBaseCode(sbeCode,filterCode);

                searchService.handleFilter(code,  targetCode, false,listFilterParams);
//                searchService.removeFilterTag(code);
                sbeCode = EventMessageUtils.getCleanSBECode(targetCode);
                queGroup = GennyConstants.QUE_TABLE_FILTER_GRP;

                //see more in bucket page
            } else if(code.equalsIgnoreCase(GennyConstants.QUE_TABLE_LAZY_LOAD)) {
                searchService.handleSortAndSearch(code,code,"",targetCode, SearchService.SearchOptions.PAGINATION_BUCKET);
            } else if(isValidFilterBox(code)) {
                sbeCode =  getSearchEntityCodeByMsgCode(code);
                queGroup = GennyConstants.QUE_TABLE_FILTER_GRP;
            } else if(isValidBucketFilterBox(code))  {
                sbeCode =  SearchCaching.SBE_APPLIED_APPLICATIONS;
                queGroup = GennyConstants.QUE_BUCKET_INTERNS_GRP;
            } else if(isFilterSubmit(code)) {
                //TODO
                String strMap = "{\n" +
                        "   \"QUE_TAG_COUNTRY_0\":{\n" +
                        "      \"attributeCode\":\"[\\\"SEL_FILTER_COLUMN_FLC_PRI_ADDRESS_COUNTRY\\\"]\",\n" +
                        "      \"QUE_FILTER_COLUMN\":\"QUE_FILTER_VALUE_COUNTRY\",\n" +
                        "      \"QUE_FILTER_VALUE\":\"[\\\"SEL_AUSTRALIA\\\"]\",\n" +
                        "      \"QUE_FILTER_OPTION\":\"[\\\"SEL_EQUAL_TO\\\"]\"\n" +
                        "   },\n" +
                        "   \"QUE_TAG_COUNTRY_1\":{\n" +
                        "      \"attributeCode\":\"[\\\"SEL_FILTER_COLUMN_FLC_PRI_ADDRESS_COUNTRY\\\"]\",\n" +
                        "      \"QUE_FILTER_COLUMN\":\"QUE_FILTER_VALUE_COUNTRY\",\n" +
                        "      \"QUE_FILTER_VALUE\":\"[\\\"SEL_AUSTRALIA\\\"]\",\n" +
                        "      \"QUE_FILTER_OPTION\":\"[\\\"SEL_EQUAL_TO\\\"]\"\n" +
                        "   }\n" +
                        "}";

                Map<String,Map<String, String>> listParams = EventMessageUtils.parseFilterMessage(strMap);
                String attrCode = getFilterAttributeCode();
                sbeCode = EventMessageUtils.getCleanSBECode(targetCode);

//                searchService.addFilterParamToList();

                searchService.handleFilter(attrCode, sbeCode, true,listParams);
            } else if(isQuickSearchSelectOptions(code,targetCode, value)) {
                searchService.sendQuickSearchItems(GennyConstants.SBE_DROPDOWN,GennyConstants.QUE_BUCKET_INTERNS_GRP
                        ,GennyConstants.QUE_SELECT_INTERN,GennyConstants.PRI_NAME, value);
            }

            //send back quick search dropdown and filter group
            if(isNeededFilterAndQuickSearch(code)) {
                searchService.sendQuickSearch(queGroup,GennyConstants.QUE_SELECT_INTERN,
                        GennyConstants.LNK_PERSON, GennyConstants.BKT_APPLICATIONS);

                // TODO
//                String currentFilter = "FLT_3B536C98-DE86-49AA-A850-DFA5158ADA4E";
                String currentFilter = "";
                Map<String,Map<String, String>> listFilterParams = null;
                if (currentFilter.isEmpty()) {
                    //get the latest of filter
                    listFilterParams = getFilterParamByBaseCode(sbeCode,currentFilter);
                } else {
                    // get the current filter
                    listFilterParams = getFilterParamByBaseCode(sbeCode,currentFilter);
                }

                searchService.sendFilterGroup(sbeCode,GennyConstants.QUE_FILTER_GRP,code,true,listFilterParams);

                String fltCond = FLT_PREF + "%";
                String queCode = QUE_PREF + SAVED_SEARCHES;
                //send saved searches
                searchService.sendDropdownOptions(sbeCode,queGroup,queCode,GennyConstants.PRI_NAME,GennyConstants.VALUE,fltCond);
            }

        } catch(Exception ex) {}
    }


    /**
     * Handle filter data event
     * @param event
     */
    public void handleFilterEventData(String event) {
        try {
            Map<String, Object> msg = EventMessageUtils.parseEventMessage(event, SearchService.SearchOptions.SEARCH);

            String token = getToken(msg);;
            String code = getCode(msg);
            String attrCode = getAttributeCode(msg);
            String attrName = "";
            String value = getValue(msg);
            String targetCode = getTargetCode(msg);

            // Go to sorting
            if (isSorting(attrCode, targetCode)) {
                searchService.handleSortAndSearch(attrCode,attrName,value,targetCode, SearchService.SearchOptions.SEARCH);
                searchService.sendQuickSearch(GennyConstants.QUE_TABLE_FILTER_GRP,GennyConstants.QUE_SELECT_INTERN,
                        GennyConstants.LNK_PERSON, GennyConstants.BKT_APPLICATIONS);
            //Go searching text
            } else if (isSearchText(attrCode)) {
                searchQuickText(msg, value, targetCode);
                // Go to filter column selected
            } else if(isFilerColumnSelected(code, attrCode)) {
                selectFilerColumn(targetCode, value);
            } else if(isFilerOptionSelected(code, attrCode)) { // Go to filter option selected
                setFilterOption(value);
            } else if(isFilterValue(code)) {
                setFilterValue(value);
            } else if(isQuickSearchSelectChanged(code, attrCode, targetCode, value)) {
                selectQuickSearch(token, attrCode, attrName, value);
            } else if(isSavedSearch(code)) {
                String name =  EventMessageUtils.getName(event);
                //TODO
                String strMap = "{\n" +
                        "   \"QUE_TAG_COUNTRY_0\":{\n" +
                        "      \"attributeCode\":\"[\\\"SEL_FILTER_COLUMN_FLC_PRI_ADDRESS_COUNTRY\\\"]\",\n" +
                        "      \"QUE_FILTER_COLUMN\":\"QUE_FILTER_VALUE_COUNTRY\",\n" +
                        "      \"QUE_FILTER_VALUE\":\"[\\\"SEL_AUSTRALIA\\\"]\",\n" +
                        "      \"QUE_FILTER_OPTION\":\"[\\\"SEL_EQUAL_TO\\\"]\"\n" +
                        "   },\n" +
                        "   \"QUE_TAG_COUNTRY_1\":{\n" +
                        "      \"attributeCode\":\"[\\\"SEL_FILTER_COLUMN_FLC_PRI_ADDRESS_COUNTRY\\\"]\",\n" +
                        "      \"QUE_FILTER_COLUMN\":\"QUE_FILTER_VALUE_COUNTRY\",\n" +
                        "      \"QUE_FILTER_VALUE\":\"[\\\"SEL_AUSTRALIA\\\"]\",\n" +
                        "      \"QUE_FILTER_OPTION\":\"[\\\"SEL_EQUAL_TO\\\"]\"\n" +
                        "   }\n" +
                        "}";

                Map<String,Map<String, String>> paramMap = EventMessageUtils.parseFilterMessage(strMap);

                saveSearches(token, FLT_PREF,name,paramMap);

                String fltCond = FLT_PREF + "%";
                String queCode = QUE_PREF + SAVED_SEARCHES;
                searchService.sendDropdownOptions(GennyConstants.SBE_DROPDOWN,"queGroup",queCode,GennyConstants.PRI_NAME,GennyConstants.VALUE,fltCond);
            }

        } catch (Exception ex){}
    }


}