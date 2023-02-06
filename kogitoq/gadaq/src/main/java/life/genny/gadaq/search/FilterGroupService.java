package life.genny.gadaq.search;

import life.genny.kogito.common.service.FilterService;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.FilterConst;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.Definition;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.message.QDataAnswerMessage;
import life.genny.qwandaq.message.QEventMessage;
import life.genny.qwandaq.models.SavedSearch;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import javax.inject.Inject;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.entity.PCM;
import life.genny.qwandaq.constants.Prefix;

@ApplicationScoped
public class FilterGroupService {
    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

    static Jsonb jsonb = JsonbBuilder.create();

    @Inject
    BaseEntityUtils beUtils;

    @Inject
    QwandaUtils qwandaUtils;

    @Inject
    UserToken user;

    @Inject
    FilterService filterService;

    @Inject
    DatabaseUtils databaseUtils;

    public static final String PRI_PREFIX = "PRI_PREFIX";
    public  static final DataType DataTypeStr = DataType.getInstance("life.genny.qwanda.entity.BaseEntity");

    /**
     * Check code whether is filter select question or not
     * @param value Filter Value
     * @return Being filter option
     */
    public  boolean isSelectBox(String value) {
        if(value.contains(FilterConst.SELECT)) {
            return true;
        }
        return false;
    }

    /**
     * Check code whether is filter value or not
     * @param code Message Code
     * @return Being filter value
     */
    public  boolean isValueSelected(String code) {
        if(code != null && code.startsWith(FilterConst.QUE_FILTER_VALUE_PREF)) {
            return true;
        }
        return false;
    }

    /**
     * Check code whether is filter submit or not
     * @param code Message Code
     * @return Being filter submit
     */
    public  boolean isApply(String code) {
        return Question.QUE_SAVED_SEARCH_APPLY.equalsIgnoreCase(code);
    }

    /**
     * Being filter optio whether selected or not
     * @param code Event Code
     * @param attCode Attribute Code
     * @return filter option selected
     */
    public boolean isColumnSelected(String code,String attCode) {
        if(Question.QUE_FILTER_COLUMN.equalsIgnoreCase(code)) {
            return true;
        }
        if(Attribute.LNK_FILTER_COLUMN.equalsIgnoreCase(attCode)) {
            return true;
        }
        return false;
    }

    /**
     * Being filter optio whether selected or not
     * @param code Event Code
     * @param attCode Attribute Code
     * @return filter option selected
     */
    public boolean isOptionSelected(String code, String attCode) {
        if(Question.QUE_FILTER_OPTION.equalsIgnoreCase(code)) {
            return true;
        }
        if(Attribute.LNK_FILTER_OPTION.equalsIgnoreCase(attCode)) {
            return true;
        }
        return false;
    }

    /**
     * Being filter optio whether selected or not
     * @param code Event Code
     * @return filter option selected
     */
    public boolean isSearchSelected(String code) {
        return Question.QUE_SAVED_SEARCH_SELECT.equalsIgnoreCase(code);
    }


    /**
     * Return question code by filter code
     * @param dataType Attribute data type
     * @return Return question code by filter code
     */
    public String getQuestionCodeByValue(String dataType,String attCode){
        if(dataType.contains(FilterConst.DATETIME)) return Question.QUE_FILTER_VALUE_DATETIME;
        if(dataType.contains(FilterConst.COUNTRY)) return Question.QUE_FILTER_VALUE_COUNTRY;
        if(dataType.contains(FilterConst.YES_NO)) return Question.QUE_FILTER_VALUE_TEXT;
        if(!isSelectBox(dataType)) return Question.QUE_FILTER_VALUE_TEXT;

        String questionCode = FilterConst.QUE_FILTER_VALUE_PREF + attCode.replaceFirst(Prefix.LNK_,"");

        return questionCode;
    }

    /**
     *  Being whether filter button or not
     * @param code Event code
     * @return Being whether it was sent or not
     */
    public boolean isFilterBtn(String code) {
        boolean result = isApply(code) || isBtnSearchAdd(code) || isBtnSearchDelete(code) || isBtnSearchSave(code)
                || isDetailDelete(code);

        return result;
    }

    /**
     *  Being whether filter event or not
     * @param msg Event Message
     * @return Being whether it was sent or not
     */
    public boolean isValidEvent(QDataAnswerMessage msg) {
        String code = getQuestionCode(msg);
        String attCode = getAttributeCode(msg);

        boolean result =  isColumnSelected(code,attCode) || isOptionSelected(code,attCode)
                || isValueSelected(code) || isFilterBtn(code) || isQuickSearchDropdown(code);

        if(isSearchSelected(code)) {
            return true;
        }

        return result;
    }

    /**
     *  Being whether filter event or not
     * @param msg Event Message
     * @return Being whether it was sent or not
     */
    public boolean isValidEvent(QEventMessage msg) {
        String code = getQuestionCode(msg);
        String attCode = msg.getAttributeCode();

        boolean result =  isColumnSelected(code,attCode) || isOptionSelected(code,attCode)
                || isValueSelected(code) || isFilterBtn(code) || isQuickSearchDropdown(code);

        if(isSearchSelected(code)) {
            return true;
        }
        return result;
    }

    /**
     * Being whether add search button or not
     * @param code Code
     * @return Being whether add search button or not
     */
    public  boolean isBtnSearchAdd(String code) {
        return Question.QUE_ADD_SEARCH.equalsIgnoreCase(code);
    }


    /**
     * Handle event when filter columns is selected
     * @param value Message value
     * @return Attribute code according to value selected
     */
    public String selectFilerColumn(String value) {
        String sbeCode = filterService.getCachedSbeTable();
        String attCode = getAttributeCodeByValue(value);
        String dataType = getAttributeDataType(attCode);
        String filterQue = getQuestionCodeByValue(dataType,attCode);

        String filterCode = "";
        Map<String, Map<String, String>> params = new HashMap<>();

        // show list of filter options
        filterService.sendFilterOption(sbeCode,dataType);

        // show filter value in the add form
        filterService.sendAddFilterGroup(filterQue);

        boolean selectBox = isSelectBox(dataType);
        if(selectBox) {
            String linkVal = attCode.replaceFirst(Prefix.LNK_,"");
            filterService.sendFilterValue(Question.QUE_ADD_FILTER_SBE_GRP,filterQue,Attribute.LNK_CORE,linkVal,attCode);
        }

        // filter code might be attribute code or conversion based on data type
        String filterAttCode = getFilterAttributeCodeForPCM(filterQue);
        return filterAttCode;
    }

    /**
     * Being whether saved search or not
     * @param code Event Code
     * @return Being whether save button  or not
     */
    public boolean isBtnSearchSave(String code) {
        return Question.QUE_SAVED_SEARCH_SAVE.equalsIgnoreCase(code);
    }

    /**
     * Being whether saved search or not
     * @param code Event Code
     * @return Being whether delete button  or not
     */
    public boolean isBtnSearchDelete(String code) {
        return Question.QUE_SAVED_SEARCH_DELETE.equalsIgnoreCase(code);
    }

    /**
     * Being whether saved search detail or not
     * @param code Event Code
     * @return Being whether detail delete button  or not
     */
    public boolean isDetailDelete(String code) {
        return Question.QUE_SBE_DETAIL_VIEW_DELETE.equalsIgnoreCase(code);
    }

    /**
     * Save search
     * @param nameOrName Event name or Code
     */
    public void saveSearch(String nameOrName) {
        Map<String,SavedSearch> params = getParamsFromCache();
        saveBaseEntity(nameOrName,params);
        filterService.sendListSavedSearches(Question.QUE_SAVED_SEARCH_SELECT_GRP, Question.QUE_SAVED_SEARCH_SELECT);
    }

    /**
     * Save base entity
     * @param nameOrCode Search name
     * @param params Parameters
     */
    public BaseEntity saveBaseEntity(String nameOrCode,Map<String, SavedSearch> params) {
        BaseEntity baseEntity = null;

        try {
            String prefix = SearchEntity.SBE_SAVED_SEARCH;
            Definition defBE = new Definition(prefix,prefix);
            defBE.setRealm(user.getProductCode());
            String baseCode = prefix + "_" + UUID.randomUUID().toString();

            // create the main base entity
            String attCode = Attribute.LNK_SAVED_SEARCHES;
            Attribute attr = new Attribute(PRI_PREFIX, attCode, DataTypeStr);
            defBE.addAttribute(attr, 1.0, Prefix.SBE_);
            if(nameOrCode.startsWith(SearchEntity.SBE_SAVED_SEARCH)) {
                baseEntity = beUtils.getBaseEntity(user.getProductCode(), nameOrCode);
            } else {
                baseEntity = beUtils.create(defBE, nameOrCode, baseCode);
            }

            Attribute attrFound = qwandaUtils.getAttribute(user.getProductCode(),attCode);

            // array of parameters and update main base entity
            List<String> listUUID = getListUUID(prefix + "_",params.entrySet().size());
            String strLnkArr = convertLnkArrayToString(listUUID);

            baseEntity.addAttribute(attrFound, 1.0, strLnkArr);
            beUtils.updateBaseEntity(baseEntity);

            // create child base entities
            Attribute childAttr = new Attribute(PRI_PREFIX, attCode, DataTypeStr);
            Definition childDef = new Definition(prefix,prefix);
            childDef.setRealm(user.getProductCode());
            childDef.addAttribute(childAttr, 1.0, prefix);

            //create other base entities based on the main base entity
            int index = 0;
            for(Map.Entry<String,SavedSearch> entry : params.entrySet()) {
                String childBaseCode = listUUID.get(index);

                BaseEntity childBase = beUtils.create(childDef, nameOrCode, childBaseCode);
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
     * Delete base entity
     * @param code Base entity
     */
    public void deleteSearch(String code) {
        databaseUtils.deleteBaseEntityAndAttribute(user.getProductCode(), code);
    }

    /**
     * Delete base entity
     * @param code Base entity
     */
    public void deleteSearches(String code) {
        String codes = beUtils.getBaseEntityValueAsString(code,Attribute.LNK_SAVED_SEARCHES);
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
     * @param filterCode Filter code
     */
    public void handleDeleteSearch(String filterCode) {
        deleteSearches(filterCode);
        filterService.sendListSavedSearches(Question.QUE_SAVED_SEARCH_SELECT_GRP,Question.QUE_SAVED_SEARCH_SELECT);
    }

    /**
     * Delete details row
     * @param targetCode Target code
     * @param value Message value
     */
    public void deleteDetail(String targetCode,String value) {
        // get from cached
        Map<String,SavedSearch> params = getParamsFromCache();
        Map<String,SavedSearch> paramsClone = getParamsFromCache();

        // remove parameter by attribute code
        for(Map.Entry<String, SavedSearch> param : paramsClone.entrySet()) {
            String strJson = jsonb.toJson(param.getValue());
            SavedSearch ss = jsonb.fromJson(strJson, SavedSearch.class);
            String column = "";
            String[] splitted =  value.split(FilterConst.SEPARATOR);

            if(splitted.length > 0) {
                column = splitted[0];
            }

            if(ss.getColumn().equalsIgnoreCase(column)) {
                params.remove(param.getKey());
            }
        }

        // send pcm  and base entities
        BaseEntity base = new BaseEntity(targetCode);
        filterService.sendPartialPCM(PCM.PCM_SBE_DETAIL_VIEW, PCM.location(1), base.getCode());
        filterService.sendFilterDetailsByBase(base,Question.QUE_SBE_DETAIL_QUESTION_GRP,base.getCode(),params);

        CacheUtils.putObject(user.getProductCode(),filterService.getCachedAnswerKey(),params);
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
    public Map<String,SavedSearch> getFilterParamsByBaseCode(String filterCode) {
        Map<String,SavedSearch>  result =  new HashMap<>();

        try {
            // get the filter by base entity code
            String codes = beUtils.getBaseEntityValueAsString(filterCode,Attribute.LNK_SAVED_SEARCHES);
            List<BaseEntity> bases = beUtils.convertCodesToBaseEntityArray(codes);

            for(BaseEntity base : bases) {
                String value = getValueStringByAttCode(base,Attribute.LNK_SAVED_SEARCHES);
                SavedSearch ss = jsonb.fromJson(value, SavedSearch.class);

                result.put(base.getCode(),ss);
            }
        }catch(Exception ex) {
            log.error(ex);
        }

        CacheUtils.putObject(user.getProductCode(),filterService.getCachedAnswerKey(),result);
        return result;
    }

    /**
     * Handle saved search selected
     * @param queCode Event code
     */
    public void handleSearchSelected(String queCode,String filterCode) {
        Map<String,SavedSearch>  params = getFilterParamsByBaseCode(filterCode);

        BaseEntity base = new BaseEntity(queCode);
        filterService.sendPartialPCM(PCM.PCM_SBE_DETAIL_VIEW, PCM.location(1), base.getCode());
        filterService.sendFilterDetailsByBase(base,Question.QUE_SBE_DETAIL_QUESTION_GRP,base.getCode(),params);
    }


    /**
     * Handle filter event by apply button
     * @param code Message Code
     */
    public void handleFilter(String code) {
        Map<String,SavedSearch> params = getParamsFromCache();
        String sbeCode = filterService.getSbeTableFromCache();

        // handle bucket
        if(isBucketSbe(sbeCode)) {
            filterService.handleFilterBucket(params);
        } else {
            // handle table
            filterService.handleFilter(sbeCode, params);
        }

        BaseEntity base = new BaseEntity(code);
        filterService.sendPartialPCM(PCM.PCM_SBE_DETAIL_VIEW, PCM.location(1), base.getCode());
        filterService.sendFilterDetailsByBase(base,Question.QUE_SBE_DETAIL_QUESTION_GRP,base.getCode(),params);
    }

    /**
     * Send base entity and question
     * @param queCode Question code
     * @param attCode Filter Option
     * @param value Filter Value
     */
    public void sendFilterDetails(String queCode,String attCode,String value) {
        Map<String,SavedSearch> params = getParamsFromCache();

        BaseEntity base = new BaseEntity(queCode);
        filterService.sendPartialPCM(PCM.PCM_SBE_DETAIL_VIEW, PCM.location(1), base.getCode());
        filterService.sendFilterDetailsByBase(base,Question.QUE_SBE_DETAIL_QUESTION_GRP,base.getCode(),params);
    }


    /**
     * Put answers in cache
     * @param filterCode Filter code
     * @param value Values
     */
    public void putAnswerstoCache(String filterCode,String value) {
        // parse saved search
        String attCode = getAttributeCodeByValue(filterCode);
        String dataType = getAttributeDataType(attCode);
        SavedSearch savedSearch = new SavedSearch(attCode,value,dataType);

        // put saved search to cached
        Map<String, SavedSearch> params = CacheUtils.getObject(user.getProductCode(),filterService.getCachedAnswerKey() ,Map.class);
        params.put(UUID.randomUUID().toString() ,savedSearch);
        CacheUtils.putObject(user.getProductCode(),filterService.getCachedAnswerKey(),params);
    }

    /**
     * Get question code
     * @param msg Message object
     * @return Question code
     */
    public String getQuestionCode(QEventMessage msg) {
        if(msg.getData() != null) {
            return msg.getData().getCode();
        }
        return "";
    }

    /**
     * Get question code
     * @param msg Message object
     * @return Question code
     */
    public String getQuestionCode(QDataAnswerMessage msg) {
        if(msg != null &&  msg.getItems().length > 0) {
            return msg.getItems()[0].getCode();
        }
        return "";
    }

    /**
     * Get attribute code
     * @param msg Message object
     * @return Attribute code
     */
    public String getAttributeCode(QDataAnswerMessage msg) {
        if(msg.getItems().length > 0) {
            return msg.getItems()[0].getAttributeCode();
        }
        return "";
    }

    /**
     * Get target code
     * @param msg Message object
     * @return target code
     */
    public String getTargetCode(QDataAnswerMessage msg) {
        if(msg.getItems().length > 0) {
            return msg.getItems()[0].getTargetCode();
        }
        return "";
    }

    /**
     * Get value
     * @param msg Message object
     * @return value
     */
    public String getValue(QDataAnswerMessage msg) {
        if(msg.getItems().length > 0) {
            return msg.getItems()[0].getValue();
        }
        return "";
    }

    /**
     * Dropdown value
     * @param value Value
     * @return dropdown value
     */
    public String getDropdownValue(String value) {
        JsonObject json = jsonb.fromJson(value, JsonObject.class);
        if(json.containsKey(FilterConst.VALUE)) {
            return json.getString(FilterConst.VALUE);
        }
        return "";
    }

    /**
     * Get iscode attribute
     * @param value isCode attribute
     * @return isCode value
     */
    public boolean isCode(String value) {
        JsonObject json = jsonb.fromJson(value, JsonObject.class);
        if(json.containsKey(FilterConst.ISCODE)) {
            return json.getBoolean(FilterConst.ISCODE);
        }
        return false;
    }

    /**
     *  Send  quick search and filter group
     * @param msg Message
     * @return Being whether it was sent or not
     */

    public boolean isQuickSearchDropdown(QEventMessage msg) {
        String code = getQuestionCode(msg);
        if(Question.QUE_QUICK_SEARCH.equalsIgnoreCase(code)) {
            return true;
        }
        return false;
    }

    /**
     *  Send  quick search and filter group
     * @param code Message Code
     * @return Being whether it was sent or not
     */

    public boolean isQuickSearchDropdown(String code) {
        if(Question.QUE_QUICK_SEARCH.equalsIgnoreCase(code)) {
            return true;
        }
        return false;
    }


    /**
     * Handle filter data event
     * @param msg Event message
     */
    public void handleBtnEvents(QEventMessage msg) {
        try {
            String token = msg.getToken();
            String code = msg.getData().getCode();
            String value = msg.getData().getValue();
            String targetCode = msg.getData().getTargetCode();

            // init user token
            if(!token.isEmpty()) { user.init(token);}

            /* Send the list of quick search result */
            if(isQuickSearchDropdown(msg)) {
                sendListQuickSearchDropdown(msg);
                return;
            }

            /* Button save search */
            if(isBtnSearchSave(code)) {
                saveSearch(value);return;
            }
            /* apply filter */
            if(isApply(code)) {
                handleFilter(code);return;
            }
            /* Button delete search */
            if(isBtnSearchDelete(code)) {
                handleDeleteSearch(value);
                return;
            }
            /* Details delete */
            if(isDetailDelete(code)){
                deleteDetail(targetCode, value);
                return;
            }

        } catch (Exception ex){
            log.error(ex);
            ex.printStackTrace();
        }
    }

    /**
     * Handle filter data event
     * @param msg Answer Message
     */
    public void handleDataEvents(QDataAnswerMessage msg) {
        try {
            String token = msg.getToken();
            String code = getQuestionCode(msg);
            String attrCode = getAttributeCode(msg);
            String value = getValue(msg);

            // init user token
            if(!token.isEmpty()) { user.init(token);}

            /* Quick search */
            if(isQuickSearchDropdown(code)) {
                handleQuickSearch(msg);
                return;
            }
            /* Go to Column */
            if(isColumnSelected(code,attrCode)) {
                String queValCode = selectFilerColumn(value);
                filterService.sendPartialPCM(PCM.PCM_SBE_ADD_SEARCH, PCM.location(3), queValCode);
                return;
            }
            /* add button*/
            if(isBtnSearchAdd(code)) {
                putAnswerstoCache(attrCode,value);
                sendFilterDetails(code,attrCode,value);
                return;
            }
            /* saved search saved */
            if(isSearchSelected(code)) {
                String dropdownVal = getDropdownValue(value);
                if(isCode(value)) {
                    handleSearchSelected(code, dropdownVal);
                }
            }

        } catch (Exception ex){
            log.error(ex);
        }
    }


    /**
     * Handle quick search
     * @param msg Answer Message
     */
    public void handleQuickSearch(QDataAnswerMessage msg) {
        try {
            String value = getValue(msg);
            boolean coded = isCode(value);
            String dropdownVal =  getDropdownValue(value);
            String sbe =  filterService.getSbeTableFromCache();
            List<String> definitions = filterService.getListDefinitionCodes(sbe);

            if(isBucketSbe(sbe)){
                // search bucket
                filterService.handleQuickSearchDropdownByBucket(dropdownVal,coded);
            }else {
                // search table
                filterService.handleQuickSearchDropdown(dropdownVal, coded, sbe,definitions);
            }

        } catch (Exception ex){
            log.error(ex);
        }
    }

    /**
     * Initialize Cache
     * @param queCode Question code
     */
    public void init(String queCode) {
        filterService.init(queCode);
    }

    /**
     * Return parameter from cache
     * @return Parameters from cache
     */
    public Map<String,SavedSearch> getParamsFromCache() {
        Map<String, SavedSearch> params = CacheUtils.getObject(user.getProductCode(),filterService.getCachedAnswerKey() ,Map.class);
        if(params == null) params = new HashMap<>();
        return params;
    }

    /**
     * Handle quick search dropdown
     * @param msg Answer Message
     */
    public void sendListQuickSearchDropdown(QEventMessage msg) {
        try {
            String value = msg.getData().getValue();
            if(!value.isEmpty()) {
                String sbeCode = filterService.getSbeTableFromCache();
                List<String> definitions = filterService.getListDefinitionCodes(sbeCode);

                filterService.sendListQuickSearches(Question.QUE_QUICK_SEARCH_GRP,Question.QUE_QUICK_SEARCH,
                        SearchEntity.SBE_QUICK_SEARCH, Attribute.PRI_NAME,FilterConst.VALUE,value,definitions);
            }
        } catch (Exception ex){
            log.error(ex);
        }
    }

    /**
     * Return being bucket or not
     * @param code Sbe code
     * @return being bucket or not
     */
    public boolean isBucketSbe(String code) {
        return SearchEntity.SBE_PROCESS.equals(code);
    }

    /**
     * Return attribute code
     * @param value Question value
     * @return Attribute code
     */
    public String getAttributeCodeByValue(String value) {
        String attCode = value.replace("[","").replace("]", "")
                .replaceFirst(FilterConst.SEL_FILTER_COLUMN_FLC,"")
                .replaceAll("\"","");

        return attCode;
    }

    /**
     * Return data type
     * @param attrCode Attribute code
     * @return Data type
     */
    public String getAttributeDataType(String attrCode) {
        String result = "";
        try {
            Attribute attribute = databaseUtils.findAttributeByCode(user.getProductCode(), attrCode);
            if (attribute != null) result = attribute.dataType.getDttCode();
        } catch (Exception ex){
            log.error(ex);
        }
        return result;
    }

    /**
     * Return filter attribute code for PCM
     * @param filterQuestion Filter question code
     * @return Filter attribute code for PCM
     */
    public String getFilterAttributeCodeForPCM(String filterQuestion) {
        Question question = databaseUtils.findQuestionByCode(user.getProductCode(),filterQuestion);
        return question.getAttributeCode();
    }

}
