package life.genny.gadaq.search;

import life.genny.kogito.common.service.FilterService;
import life.genny.kogito.common.service.FilterService.Options;
import life.genny.kogito.common.service.SearchService;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.FilterConst;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.message.QEventMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.lang.invoke.MethodHandles;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.Comparator;
import javax.inject.Inject;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.Question;

@ApplicationScoped
public class FilterGroupService {
    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

    static Jsonb jsonb = JsonbBuilder.create();

    @Inject
    BaseEntityUtils beUtils;

    @Inject
    QwandaUtils qwandaUtils;

    @Inject
    UserToken userToken;

    @Inject
    FilterService filterService;

    @Inject
    DatabaseUtils databaseUtils;

    @Inject
    SearchService search;

    public static final String EVT_QUE_TREE_PREFIX = "QUE_TREE_ITEM_";
    public static final String QUE_TABLE_PREF = "QUE_TABLE_";
    public static final String SBE_TABLE_PREF = "SBE_TABLE_";
    public static final String QUE_FILTER_PREF = "QUE_FILTER_";
    public static final String LNK_FILTER_PREF = "LNK_FILTER_";

    public static final String DATE = "DATE";
    public static final String DATETIME = "DATETIME";
    public static final String TIME = "TIME";

    /* select box option */
    public static final String PRI_ADDRESS_COUNTRY = "PRI_ADDRESS_COUNTRY";
    public static final String PRI_ASSOC_COMP_INTERNSHIP = "PRI_ASSOC_COMP_INTERNSHIP";
    public static final String PRI_INTERNSHIP_TYPE = "PRI_INTERNSHIP_TYPE";
    public static final String PRI_DJP_AGREE = "PRI_DJP_AGREE";

    /* Dropdown links */
    public static final String COUNTRY = "COUNTRY";
    public static final String COMPLETE_INTERNSHIP = "COMPLETE_INTERNSHIP";
    public static final String YES_NO = "YES_NO";
    public static final String INTERNSHIP_TYPE = "INTERNSHIP_TYPE";

    /* Bucket */
    public static final String PREF_CPY = "CPY_";
    public static final String QUE_TAB_BUCKET_VIEW = "QUE_TAB_BUCKET_VIEW";
    public static final String QUE_PREF = "QUE_";
    public static final String PRI_PREFIX = "PRI_PREFIX";
    public static final String LNK_SAVED_SEARCHES = "LNK_SAVED_SEARCHES";
    public  static final DataType DataTypeStr = DataType.getInstance("life.genny.qwanda.entity.BaseEntity");
    public static final String DELETE = "Delete";
    public static final String QUE_ADD_SEARCH = "QUE_ADD_SEARCH";

    /**
     * Return search base entity by message code
     * @param code Message Code
     * @return  Return search base entity
     */
    public String getSearchEntityCodeByMsgCode(String code) {
        String sbeCode = code.replaceFirst(EVT_QUE_TREE_PREFIX,"");
        sbeCode = FilterConst.CACHING_SBE + sbeCode.replaceFirst(QUE_PREF,"");

        return sbeCode;
    }

    /**
     * Being filter prefix or not
     * @param code Question Code
     * @return Being filter prefix or not
     */
    public boolean isFilterPref(String code) {
        if(code!=null && code.startsWith(QUE_FILTER_PREF)) {
            return true;
        }
        return false;
    }
    /**
     * Check code whether is filter select question or not
     * @param filterValue Filter Value
     * @return Being filter option
     */
    public  boolean isFilterSelectQuestion(String filterValue) {
        boolean result = false;
        if(filterValue.contains(PRI_ADDRESS_COUNTRY)
                || filterValue.contains(PRI_ASSOC_COMP_INTERNSHIP)
                || filterValue.contains(PRI_DJP_AGREE)
                || filterValue.contains(PRI_INTERNSHIP_TYPE))
            return true;

        return result;
    }

    /**
     * Check code whether is filter value or not
     * @param code Message Code
     * @return Being filter value
     */
    public  boolean isFilterValueSelected(String code) {
        boolean result = false;
        if(code != null && code.startsWith(FilterConst.QUE_FILTER_VALUE_PREF))
            return true;

        return result;
    }

    /**
     * Check code whether is filter submit or not
     * @param code Message Code
     * @return Being filter submit
     */
    public  boolean isFilterApply(String code) {
        boolean result = false;
        if(code != null && code.equalsIgnoreCase(FilterConst.QUE_SAVED_SEARCH_APPLY))
            return true;

        return result;
    }

    /**
     * Check code whether is quesion showing filter box or not
     * @param code Message Code
     * @return Being question showing filter box
     */
    public  boolean isValidTable(String code) {
        boolean result = false;
        if(code!=null &&  code.startsWith(QUE_TABLE_PREF))
            return true;

        return result;
    }

    /**
     * Return value by event code in safe way
     * @param msg Attribute Map
     * @param eventCode Event code
     * @return Return value by event code
     */
    public String getSafeValueByCode(JsonObject msg, String eventCode) {
        String value = "";
        if(msg == null) return value;
        if(msg.containsKey(eventCode)) {
            value = msg.get(eventCode).toString();
        }
        return value;
    }

    /**
     * Being filter optio whether selected or not
     * @param code Event Code
     * @param attCode Attribute Code
     * @param value Value
     * @return filter option selected
     */
    public boolean isFilerColumnSelected(String code,String attCode,String value) {
        boolean result = false;
        if(code != null && code.startsWith(FilterConst.QUE_FILTER_COLUMN))
            return true;

        if(attCode !=null && attCode.startsWith(FilterConst.LNK_FILTER_COLUMN))
            return true;

        return result;
    }

    /**
     * Being filter optio whether selected or not
     * @param code Event Code
     * @param attCode Attribute Code
     * @return filter option selected
     */
    public boolean isFilerOptionSelected(String code, String attCode) {
        boolean result = false;
        if(code != null && code.startsWith(FilterConst.QUE_FILTER_OPTION)) return true;
        if(attCode!=null && attCode.startsWith(FilterConst.LNK_FILTER_OPTION)) return true;

        return result;
    }

    /**
     * Being filter optio whether selected or not
     * @param code Event Code
     * @return filter option selected
     */
    public boolean isSearchSelected(String code) {
        boolean result = false;
        if(code!=null && code.startsWith(FilterConst.QUE_SAVED_SEARCH_LIST))
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

        if(filterVal.contains(PRI_ADDRESS_COUNTRY)){
            return FilterConst.QUE_FILTER_VALUE_COUNTRY;
        } else if(filterVal.contains(PRI_ASSOC_COMP_INTERNSHIP)){
            return FilterConst.QUE_FILTER_VALUE_ACADEMY;
        } else if(filterVal.contains(PRI_DJP_AGREE)){
            return FilterConst.QUE_FILTER_VALUE_DJP_HC;
        } else if(filterVal.contains(PRI_INTERNSHIP_TYPE)){
            return FilterConst.QUE_FILTER_VALUE_INTERNSHIP_TYPE;
        }
        //date,time
        if(suffix.equalsIgnoreCase(DATE)){
            return FilterConst.QUE_FILTER_VALUE_DATE;
        } else if(suffix.equalsIgnoreCase(DATETIME)){
            return FilterConst.QUE_FILTER_VALUE_DATETIME;
        } else if(suffix.equalsIgnoreCase(TIME)){
            return FilterConst.QUE_FILTER_VALUE_TIME;
        }

        //text
        if(!filterVal.isEmpty()) {
            return FilterConst.QUE_FILTER_VALUE_TEXT;
        }
        return questionCode;
    }

    /**
     * Return attribute code by question
     * @param questionCode Question Code
     * @return Return question code by filter code
     */
    public String getAttributeCodeByQuestion(String questionCode){
        Question question = databaseUtils.findQuestionByCode(userToken.getProductCode(), questionCode);
        return question.getAttributeCode();
    }

    /**
     * Return link value based on event value
     * @param value Event Value
     * @return Return link value based on event value
     */
    public String getLinkVal(String value) {
        String lnkVal = "";
        if(value.contains(PRI_ADDRESS_COUNTRY)){
            lnkVal = COUNTRY;
        } else if(value.contains(PRI_ASSOC_COMP_INTERNSHIP)){
            lnkVal = COMPLETE_INTERNSHIP;
        } else if(value.contains(PRI_DJP_AGREE)){
            lnkVal = YES_NO;
        } else if(value.contains(PRI_INTERNSHIP_TYPE)){
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
     * @param code Question code
     * @return Being whether event is pagination event or not
     */
    public boolean isPaginationEvent(String code) {
        if(code.equalsIgnoreCase(FilterConst.PAGINATION_NEXT)
                || code.equalsIgnoreCase(FilterConst.PAGINATION_PREV)) {
            return true;
        }

        return false;
    }

    /**
     * Being whether event is pagination event or not
     * @param code Event code
     * @return Being whether event is pagination event or not
     */
    public boolean isBucketPagination(String code) {
        if(code.equalsIgnoreCase(FilterConst.QUE_TABLE_LAZY_LOAD)) {
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
        if(eventCode.startsWith(FilterConst.QUE_SELECT_INTERN)
                && targetCode.startsWith(FilterConst.BKT_APPLICATIONS)
                && !value.isEmpty())
            return true;

        return result;
    }


    /**
     * Being whether bucket filter select options or not
     * @param code Event code
     * @param attrCode Attribute code
     * @param targetCode Target code
     * @param value Event value
     * @return Being whether bucket filter select options or not
     */
    public boolean isQuickSearchSelectChanged(String code, String attrCode, String targetCode, String value) {
        boolean result = false;
        String newVal =  getStripSelectValue(value);

        if(code!=null && code.startsWith(FilterConst.QUE_SELECT_INTERN)
                && attrCode.startsWith(FilterConst.LNK_PERSON)
                && targetCode.startsWith(FilterConst.BKT_APPLICATIONS)
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
        if(attrCode.equalsIgnoreCase(FilterConst.SEARCH_TEXT)) {
            return true;
        }

        return false;
    }

    /**
     * Check code whether is quesion showing filter box or not
     * @param code Message Code
     * @return Being question showing filter box
     */
    public  boolean isValidBucket(String code) {
        boolean result = false;
        if(code != null && code.startsWith(QUE_TAB_BUCKET_VIEW))
            return true;
        return result;
    }

    /**
     *  Being whether filter button or not
     * @param msg Event message
     * @return Being whether it was sent or not
     */
    public boolean isFilter(QEventMessage msg) {
        String code = msg.getData().getCode();
        boolean result = isValidTable(code) || isValidBucket(code) || isFilterBtn(code) || isAddFilterGroup(msg);
        return result;
    }

    /**
     *  Being whether filter button or not
     * @param code Event code
     * @return Being whether it was sent or not
     */
    public boolean isFilterBtn(String code) {
        boolean result = isFilterApply(code) || isBtnSearchAdd(code) || isBtnSearchDelete(code)
                || isBtnSearchSave(code);

        return result;
    }

    /**
     *  Being whether filter event or not
     * @param msg Event Message
     * @return Being whether it was sent or not
     */
    public boolean isDropdown(QEventMessage msg) {
        String code = msg.getData().getCode();
        String attCode = msg.getAttributeCode();
        String value  = msg.getData().getValue();
//        boolean result = isFilerColumnSelected(code,attCode,value) || isFilerOptionSelected(code,attCode)
//                            || isFilterValueSelected(code);
        boolean result = isFilerOptionSelected(code,attCode) || isFilterValueSelected(code);

        return result;
    }

    /**
     *  Send  quick search and filter group
     * @param code Event code
     * @return Being whether it was sent or not
     */

    public boolean isFilterAndQuickSearch(String code) {
        boolean result = isValidTable(code) || isValidBucket(code)
                || isFilterApply(code);

        return result;
    }


    /**
     * Return add fitler group or not
     * @param msg Event Message
     * @return Whether being add fitler group or not
     */
    public boolean isFilterQuestionPref(QEventMessage msg) {
        if(msg.getData().getParentCode() !=null && msg.getData().getParentCode().startsWith(QUE_FILTER_PREF)) {
            return true;
        }
        if(msg.getData().getCode() !=null && msg.getData().getCode().startsWith(QUE_FILTER_PREF)) {
            return true;
        }
        if(msg.getAttributeCode() !=null && msg.getAttributeCode().startsWith(LNK_FILTER_PREF)) {
            return true;
        }
        return false;
    }

    /**
     * Return add fitler group or not
     * @param msg Event message
     * @return Whether being add fitler group or not
     */
    public boolean isAddFilterGroup(QEventMessage msg) {
        if(msg.getData().getParentCode() !=null
                && msg.getData().getParentCode().equalsIgnoreCase(FilterConst.QUE_ADD_FILTER_SBE_GRP)) {
            return true;
        }
        return false;
    }


    /**
     * Being whether add search button or not
     * @param code Code
     * @return Being whether add search button or not
     */
    public  boolean isBtnSearchAdd(String code) {
        boolean result = false;
        if(code != null && code.startsWith(QUE_ADD_SEARCH))
            return true;
        return result;
    }

    /**
     * Search quick text
     * @param msg Parsed message
     * @param value Message value
     * @param targetCode Target code
     */
    public void searchQuickText(JsonObject msg, String value, String targetCode) {
        String attrCode = Attribute.PRI_NAME;
        String attrName = Operator.LIKE.toString();
        String text =  "%" + value.replaceFirst("!","") + "%";
        List<String> targetCodes =  EventMessageUtils.getTargetCodes(msg);

        /* Go to bucket */
        if (targetCodes.size() > 1) {
            filterService.handleBucketSearch(attrCode, attrName, text, targetCodes);
            /* Go to search text */
        }else {
            filterService.handleSortAndSearch(attrCode, attrName, text, targetCode, Options.SEARCH);
            filterService.sendQuickSearch(FilterConst.QUE_TABLE_FILTER_GRP,FilterConst.QUE_SELECT_INTERN,
                    FilterConst.LNK_PERSON, FilterConst.BKT_APPLICATIONS);
        }
    }

    /**
     * Handle event when filter columns is selected
     * @param targetCode Target code
     * @param value Message value
     * @return Attribute code according to value selected
     */
    public String selectFilerColumn(String targetCode, String value) {
        String queCode = getQuestionCodeByFilterValue(value);
        String attCode = getAttributeCodeByQuestion(queCode);

        String filterCode = "";
        Map<String, Map<String, String>> params = new HashMap<>();
        filterService.sendFilterOption(queCode, targetCode);
        filterService.sendAddFilterGroup(FilterConst.QUE_ADD_FILTER_SBE_GRP,queCode,filterCode,params);
        filterService.sendFilterDetailsByGroup(FilterConst.QUE_SBE_DETAIL_QUESTION_GRP,filterCode,params);

        boolean isSelectBox = isFilterSelectQuestion(value);
        if(isSelectBox) {
            String linkVal = getLinkVal(value);
            filterService.sendFilterValue(FilterConst.QUE_ADD_FILTER_SBE_GRP,queCode,FilterConst.LNK_CORE,linkVal,attCode);
        }

        return attCode;
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
        List<String> bucketCodes = filterService.getBucketCodesBySBE(FilterConst.SBE_TAB_BUCKET_VIEW);
        String baseCode = getStripSelectValue(value);
        String newVal = getBaseNameByCode(baseCode);

        filterService.handleBucketSearch(attrCode, attrName, newVal, bucketCodes);
    }

    /**
     * Being whether saved search or not
     * @param code Event Code
     * @return Being whether save button  or not
     */
    public boolean isBtnSearchSave(String code) {
        boolean result = false;
        if(code!=null && code.startsWith(FilterConst.QUE_SAVED_SEARCH_SAVE))
            return true;

        return result;
    }

    /**
     * Being whether saved search or not
     * @param code Event Code
     * @return Being whether delete button  or not
     */
    public boolean isBtnSearchDelete(String code) {
        boolean result = false;
        if(code!=null && code.startsWith(FilterConst.QUE_SAVED_SEARCH_DELETE))
            return true;

        return result;
    }

    /**
     * Get attribute name
     * @param index Index
     * @return Attribute Name
     */
    public String getAttributeName(int index) {
        String attName = LNK_SAVED_SEARCHES;
        if(index != 0) {
            attName = LNK_SAVED_SEARCHES + "_" + index;
        }

        return attName;
    }
    /**
     * Save search
     * @param code Event name
     * @param queGrp Event name
     * @param sbeCode Event name
     * @param name Event name
     * @param value Event value
     */
    public void saveSearch(String code,String queGrp,String sbeCode, String name,String value) {
        String cleanParams = EventMessageUtils.getCleanFilterParamsByString(value);
        Map<String,Map<String, String>> params = EventMessageUtils.parseFilterMessage(cleanParams);

        //save each attribute as the row of filter table
        saveAttribute(FilterConst.LNK_SAVED_SEARCHES);

        String prefix = FilterConst.SBE_SAVED_SEARCH + "_";
        BaseEntity base = saveBaseEntity(prefix,name,params);
        String filterCode = base.getCode();

        /* update filter existing group */
        filterService.sendFilterGroup(sbeCode,FilterConst.QUE_FILTER_GRP,code,true,filterCode,params);

        filterService.sendListSavedSearches(FilterConst.SBE_DROPDOWN,queGrp,
                FilterConst.QUE_SAVED_SEARCH_LIST, FilterConst.PRI_NAME,FilterConst.VALUE);
    }

    /**
     * Save base entity
     * @param prefix Definition prefix
     * @param name Search name
     * @param params Parameters
     */
    public BaseEntity saveBaseEntity(String prefix,String name,Map<String, Map<String,String>> params) {
        BaseEntity baseEntity = null;

        try {
            BaseEntity defBE = new BaseEntity(prefix);
            String baseCode = prefix + UUID.randomUUID().toString();

            //create the main base entity
            String attCode = FilterConst.LNK_SAVED_SEARCHES;
            Attribute attr = new Attribute(PRI_PREFIX, attCode, DataTypeStr);
            defBE.addAttribute(attr, 1.0, FilterConst.CACHING_SBE);
            baseEntity = beUtils.create(defBE, name, baseCode);
            Attribute attrFound = databaseUtils.findAttributeByCode(userToken.getRealm(),attCode);

            List<String> listUUID = getListUUID(prefix,params.entrySet().size());

            String strLnkArr = convertLnkArrayToString(listUUID);
            baseEntity.addAttribute(attrFound, 1.0, strLnkArr);
            beUtils.updateBaseEntity(baseEntity);

            Attribute childAttr = new Attribute(PRI_PREFIX, attCode, DataTypeStr);
            BaseEntity childDefBE = new BaseEntity(prefix);
            childDefBE.addAttribute(childAttr, 1.0, prefix);

            //create other base entities based on the main base entity
            int index = 0;
            for(Map.Entry<String,Map<String,String>> entry : params.entrySet()) {
                String childBaseCode = listUUID.get(index);

                BaseEntity childBase = beUtils.create(childDefBE, name, childBaseCode);
                String childVal = jsonb.toJson(entry.getValue());
                childBase.addAttribute(attrFound, 1.0,childVal);
                beUtils.updateBaseEntity(childBase);
                index++;
            }

        }catch (Exception ex) {
            log.error(ex);
        }

        return baseEntity;
    }

    /**
     * Return list of UUID
     * @param size Size
     * @return list of UUID
     */
    public List<String> getListUUID(String prefix,int size) {
        List<String> list = new ArrayList<>();
        for(int i=0; i< size;i++) {
            String uuid =   prefix + UUID.randomUUID().toString().toUpperCase();
            list.add(uuid);
        }
        return list;
    }

    /**
     * Get string of link array
     * @param params Parameters
     * @return string of link array
     */
    public String convertLnkArrayToString(List<String> params) {
        String result = "";
        for(int i=0; i< params.size();i++) {
            if(params.size() == 1) {
                return "[" + "\"" + params.get(i) + "\"" + "]";
            }

            if(i == 0) {
                result = "[" + "\"" + params.get(i) + "\"" + ",";
            }else if(i == params.size() - 1) {
                result = result + "\"" + params.get(i) + "\"" + "]";
            } else {
                result = result + "\"" + params.get(i) + "\"" + ",";
            }
        }
        return result;
    }

    /**
     * Save attribute code
     * @param attCode Attribute code
     * @return Return attribute
     */
    public Attribute saveAttribute(String attCode) {
        Attribute attrFound = null;
        try {
            attrFound = databaseUtils.findAttributeByCode(userToken.getRealm(),attCode);
        }catch (Exception ex) {
            Attribute newAtt = new Attribute(attCode, attCode, new DataType());
            qwandaUtils.saveAttribute(newAtt);
            attrFound =  databaseUtils.findAttributeByCode(userToken.getRealm(),attCode);
        }

        return attrFound;
    }

    /**
     * Delete base entity
     * @param code Base entity
     */
    public void deleteSearch(String code) {
        databaseUtils.deleteBaseEntityAndAttribute(userToken.getRealm(), code);
    }

    /**
     * Delete base entity
     * @param code Base entity
     */
    public void deleteSearches(String code) {
        String codes = beUtils.getBaseEntityValueAsString(code,LNK_SAVED_SEARCHES);
        List<BaseEntity> bases = beUtils.convertCodesToBaseEntityArray(codes);
        /* delete primary search */
        deleteSearch(code);

        /* delete other searchs */
        for(BaseEntity base : bases) {
            deleteSearch(base.getCode());
        }
    }

    /**
     * Save searches
     * @param sbeCode Search base entity code
     * @param msg Message object
     */
    public void handleDeleteSearch(String sbeCode,String code, JsonObject msg) {
        Map<String,Map<String, String>> params = new HashMap<>();

        String searchCode = EventMessageUtils.getSearchCode(msg);
//        deleteSearches(searchCode);

        /* update filter existing group */
        filterService.sendFilterGroup(sbeCode,FilterConst.QUE_FILTER_GRP,code,true,searchCode,params);

        String queGroup = EventMessageUtils.getParentCode(msg);
        filterService.sendListSavedSearches(FilterConst.SBE_DROPDOWN,queGroup,FilterConst.QUE_SAVED_SEARCH_LIST,
                FilterConst.PRI_NAME, FilterConst.VALUE);
    }

    /**
     * Get String value of base entity by attribute code
     * @param base Base entity
     * @param attCode Attribute code
     * @return String value of base entity by attribute code
     */
    public String getValueStringByAttCode(BaseEntity base, String attCode) {
        String value = "";

        Set<EntityAttribute> attributeSet =  base.getBaseEntityAttributes();
        Optional<EntityAttribute> ea = attributeSet.stream().filter(e -> e.getAttributeCode().equalsIgnoreCase(attCode))
                .findFirst();
        if(ea.isPresent()) {
            return ea.get().getValueString();
        }

        return value;
    }

    /**
     * Get the table of filter parameters
     * @param filterCode Filter code
     * @return Get the table of filter parameters
     */
    public Map<String,Map<String, String>> getFilterParamByBaseCode(String filterCode) {
        Map<String,Map<String, String>>  result =  new HashMap<>();

        String value = "";
        try {
            // get the filter by base entity code
            BaseEntity base = beUtils.getBaseEntityByCode(filterCode);
            value = getValueStringByAttCode(base,LNK_SAVED_SEARCHES);

            result = jsonb.fromJson(value, Map.class);
        }catch(Exception ex) {}

        return result;
    }

    /**
     * Get the table of filter parameters
     * @param filterCode Filter code
     * @return Get the table of filter parameters
     */
    public Map<String,Map<String, String>> getFilterParamsByBaseCode(String filterCode) {
        Map<String,Map<String, String>>  result =  new HashMap<>();

        try {
            // get the filter by base entity code
            String codes = beUtils.getBaseEntityValueAsString(filterCode,LNK_SAVED_SEARCHES);
            List<BaseEntity> bases = beUtils.convertCodesToBaseEntityArray(codes);

            for(BaseEntity base : bases) {
                String value = getValueStringByAttCode(base,LNK_SAVED_SEARCHES);
                result.put(base.getCode(),jsonb.fromJson(value, Map.class));
            }
        }catch(Exception ex) {}

        return result;
    }

    /**
     * Get the latest filter code
     * @param sbeCode Search base entity code
     * @return The latest filter code
     */
    public String getLatestFilterCode(String sbeCode) {
        String filterCode = "";
        List<BaseEntity> bases = filterService.getListSavedSearches(sbeCode,FilterConst.PRI_NAME,
                FilterConst.VALUE);
        List<BaseEntity> basesSorted =  bases.stream()
                .sorted(Comparator.comparing(BaseEntity::getId).reversed())
                .collect(Collectors.toList());

        if(basesSorted.size() > 0) {
            return basesSorted.get(0).getCode();
        }
        return filterCode;
    }

    /**
     * Send filter and quick search data
     * @param code Code
     * @param queGroup Question group
     * @param sbeCode Search base entity code
     * @params filters Filter parameters
     */
    public void sendFilterAndQuickSearch(String code,String queGroup, String sbeCode, String filterCode,
                                         Map<String,Map<String, String>> filters, boolean isSubmitted) {

        filterService.sendQuickSearch(queGroup,FilterConst.QUE_SELECT_INTERN, FilterConst.LNK_PERSON,
                FilterConst.BKT_APPLICATIONS);

        /* get the latest filter code if filterCode is empty */
        if(filterCode.isEmpty()) {
            filterCode = getLatestFilterCode(sbeCode);
        }

        Map<String,Map<String, String>> filterParams = new HashMap<>();
        if(isSubmitted) {
            filterParams = filters;
        } else {
            filterParams = getFilterParamsByBaseCode(filterCode);
        }

        /* get the latest of filter */
//        filterService.sendFilterGroup(sbeCode,FilterConst.QUE_FILTER_GRP,code,true,filterCode,filterParams);
        filterService.sendFilterColumns(sbeCode);

        /* send saved searches */
        String newSbe = filterService.getSearchBaseEntityCodeByJTI(sbeCode);
        String queCode = FilterConst.QUE_SAVED_SEARCH_LIST;

        /* send saved search list */
        filterService.sendListSavedSearches(newSbe,queGroup,queCode,FilterConst.PRI_NAME,FilterConst.VALUE);

    }


    /**
     * Handle saved search selected
     * @param target Target code or sbe code
     * @param eventCode Event code
     * @param msg Message Object
     */
    public void handleSearchSelected(String target, String eventCode,JsonObject msg) {
        String filterCode =  EventMessageUtils.getFilterCode(msg);
        Map<String,Map<String, String>>  params = getFilterParamsByBaseCode(filterCode);
        filterService.sendFilterGroup(target,FilterConst.QUE_FILTER_GRP,eventCode,true,filterCode,params);
    }


    /**
     * Handle filter event by apply button
     * @param targetCode Target code or search base entity cod
     * @param value Event value
     */
    public void handleFilter(String targetCode, String value) {
        Map<String,Map<String, String>> params = EventMessageUtils.parseFilterMessage(value);
        String sbeCode = EventMessageUtils.getCleanSBECode(targetCode);
        filterService.handleFilter(sbeCode, params);
    }

    /**
     * Handle sorting
     * @param attrCode Attribute code
     * @param attrName Attribute name
     * @param value Event Value
     * @param targetCode Target code
     */
    public void handleSorting(String attrCode,String attrName,String value,String targetCode) {
        filterService.handleSortAndSearch(attrCode,attrName,value,targetCode, FilterService.Options.SEARCH);
        filterService.sendQuickSearch(FilterConst.QUE_TABLE_FILTER_GRP,FilterConst.QUE_SELECT_INTERN,
                FilterConst.LNK_PERSON, FilterConst.BKT_APPLICATIONS);
    }

    /**
     * Send base entity and question
     * @param queCode Question code
     * @param attCode Filter Option
     * @param value Filter Value
     */
    public void sendFilterDetails(String queCode,String attCode,String value) {
        String stripVal = getStripSelectValue(value);

        String filterCode = "";
        Map<String,Map<String,String>> listParam = new HashMap();
        Map<String,String> param = new HashMap<>();

        filterService.sendFilterDetailsByGroup(FilterConst.QUE_SBE_DETAIL_QUESTION_GRP,filterCode,listParam);
        filterService.sendFilterDetailsByBase(FilterConst.QUE_SBE_DETAIL_QUESTION_GRP,queCode,attCode,stripVal);
//        filterService.sendFilterDetailsByPcm(PCM_SBE_DETAIL_VIEW,queCode,attCode,stripVal);
    }

    public void initDropDown(String sbeCode) {
        /* get the latest of filter */
        filterService.sendFilterColumns(sbeCode);
    }

    /**
     * Handle filter event
     * @param event Event
     */
    public void handleEvent(String event) {
        try {
            JsonObject msg = jsonb.fromJson(event, JsonObject.class);;

            String token = EventMessageUtils.getToken(msg);
            String code = EventMessageUtils.getCode(msg);
            String targetCode = EventMessageUtils.getTargetCode(msg);
            String value = EventMessageUtils.getValue(msg);
            String queGroup = EventMessageUtils.getParentCode(msg);
            Map<String,Map<String, String>> filterParams = new HashMap<>();
            boolean isSubmitted = false;

            /* init user token */
            if(!token.isEmpty()) { userToken.init(token);}

            /* bucket pagination */
            if(isBucketPagination(code)) {
                filterService.handleSortAndSearch(code,code,"",targetCode, Options.PAGINATION_BUCKET);

                /* Show saved search for table */
            } else if(isValidTable(code)) {
                targetCode =  getSearchEntityCodeByMsgCode(code);
                queGroup = FilterConst.QUE_TABLE_FILTER_GRP;

                /* Show saved search for bucket */
            } else if(isValidBucket(code))  {
                targetCode =  FilterConst.SBE_APPLIED_APPLICATIONS;
                queGroup = FilterConst.QUE_BUCKET_INTERNS_GRP;

                /* apply filter */
            } else if(isFilterApply(code)) {
                handleFilter(targetCode,value);
                isSubmitted = true;
                filterParams = EventMessageUtils.getCleanFilterParamsByMap(value);
                /* quick search is selected */
            } else if(isQuickSearchSelectOptions(code,targetCode, value)) {
                filterService.sendQuickSearchItems(FilterConst.SBE_DROPDOWN,FilterConst.QUE_BUCKET_INTERNS_GRP
                        ,FilterConst.QUE_SELECT_INTERN,FilterConst.PRI_NAME, value);

                /* Button save search */
            } else if(isBtnSearchSave(code)) {
                String searchName = EventMessageUtils.getSearchName(msg);
                saveSearch(code,queGroup, targetCode,searchName, value);
            }
            /* Button delete search */
            else if(isBtnSearchDelete(code)) {
                handleDeleteSearch(targetCode, code,msg);
            }

            /* send back quick search dropdown and filter group */
            if(isFilterAndQuickSearch(code)) {
                String filterCode = EventMessageUtils.getFilterCode(msg);
                sendFilterAndQuickSearch(code,queGroup,targetCode,filterCode,filterParams,isSubmitted);
            }

        } catch(Exception ex) {
            log.error(ex);
        }
    }


    /**
     * Handle filter data event
     * @param event
     */
    public void handleDropdown(String event) {
        try {
            JsonObject msg = jsonb.fromJson(event, JsonObject.class);

            String token = EventMessageUtils.getToken(msg);;
            String code = EventMessageUtils.getCode(msg);
            String attrCode = EventMessageUtils.getAttributeCode(msg);
            String attrName = "";
            String value = EventMessageUtils.getValue(msg);
            String targetCode = EventMessageUtils.getTargetCode(msg);
            String cleanSBE = filterService.getCleanSBECode(targetCode);

            if(!isFilterPref(code)) return;

            /* init user token */
            if(!token.isEmpty()) { userToken.init(token);}

            /* Go to sorting */
            if(isFilerColumnSelected(code,attrCode,value)) {
                String queValCode = selectFilerColumn(cleanSBE, value);
                filterService.sendPartialPCM(FilterConst.PCM_SBE_ADD_SEARCH, FilterConst.PRI_LOC3, queValCode);
                sendFilterDetails(code,attrCode,value);

             /* Filter option selected */
            } else if(isFilerOptionSelected(code, attrCode)) {
                sendFilterDetails(code,attrCode,value);

            /* Filter value selected */
            } else if(isFilterValueSelected(code)) {
                sendFilterDetails(code,attrCode,value);
            /* Go to list of searches selected */
            } else if(isSearchSelected(code)) {
                handleSearchSelected(targetCode,code,msg);

            /* Quick search selected */
            } else if(isQuickSearchSelectChanged(code, attrCode, targetCode, value)) {
                selectQuickSearch(token, attrCode, attrName, value);
            }

            /* Send table again when selecting filter column */
            if(isValidTable(cleanSBE)) {
                String questionCode = getQuestionCodeBySBE(cleanSBE);
                search.sendTable(questionCode);
            }


        } catch (Exception ex){
            log.error(ex);
        }
    }

    /**
     * Get question code by sbe code
     * @param cleanSBE sbe code
     * @return question code by sbe code
     */
    public String getQuestionCodeBySBE(String cleanSBE) {
        return cleanSBE.replace(SBE_TABLE_PREF, QUE_TABLE_PREF);
    }

}
