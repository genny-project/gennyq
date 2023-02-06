package life.genny.kogito.common.service;

import life.genny.qwandaq.constants.FilterConst;
import life.genny.qwandaq.entity.search.clause.Or;
import life.genny.qwandaq.graphql.ProcessData;
import life.genny.qwandaq.models.SavedSearch;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.CacheUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import life.genny.qwandaq.entity.search.clause.ClauseContainer;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.message.QBulkMessage;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.PCM;
import life.genny.qwandaq.constants.Prefix;

@ApplicationScoped
public class FilterService extends KogitoService {

	@Inject
	Logger log;

    /**
     * Handle quick search
     * @param value Value
     * @param coded being coded or not
     * @param targetCode Target code
     */
    public void handleQuickSearchDropdown(String value,boolean coded,String targetCode,List<String> definitions) {
        String sessionCode = searchUtils.sessionSearchCode(targetCode);
        String cachedKey = FilterConst.LAST_SEARCH + sessionCode;

        SearchEntity searchBE = CacheUtils.getObject(userToken.getProductCode(), cachedKey, SearchEntity.class);
        clearFilters(searchBE);

        // add definitions
        for(int i=0;i< definitions.size(); i++){
            if(i== 0) {
                searchBE.add(new Filter(Attribute.LNK_DEF, Operator.CONTAINS, definitions.get(i)));
            } else {
                searchBE.add(new Or(new Filter(Attribute.LNK_DEF, Operator.CONTAINS, definitions.get(i))));
            }
        }

        // searching by text or search by code
        Filter filter = null;
        String newValue = value.replaceFirst("!","");

        if(coded) {
            filter = new Filter(Attribute.PRI_CODE, Operator.EQUALS, value);
        } else {
            filter = new Filter(Attribute.PRI_NAME, Operator.LIKE, "%" + newValue + "%");
        }

        searchBE.remove(filter);
        searchBE.add(filter);

        CacheUtils.putObject(userToken.getProductCode(), cachedKey, searchBE);

        String queCode =  targetCode.replaceFirst(Prefix.SBE_,Prefix.QUE_);
        search.sendTable(queCode);
    }

    /**
     * Clearn filers from search base entity
     * @param searchBE Search base entity
     */
    public void clearFilters(SearchEntity searchBE) {
        List<ClauseContainer> cloneList = new ArrayList<ClauseContainer>(searchBE.getClauseContainers());

        for(ClauseContainer cc : cloneList) {
            searchBE.remove(cc.getFilter());
        }
    }

    /**
     * Send filter group and filter column for filter function
     * @param questionCode Question code
     */
    public void sendAddFilterGroup(String questionCode) {
        try {
            Ask ask = filterUtils.getFilterGroup(questionCode);
            QDataAskMessage msgFilterGrp = new QDataAskMessage(ask);
            msgFilterGrp.setToken(userToken.getToken());

            msgFilterGrp.setTargetCode(questionCode);
            ask.setQuestionCode(questionCode);
            ask.getQuestion().setCode(questionCode);

            msgFilterGrp.setMessage(FilterConst.FILTERS);
            msgFilterGrp.setTag(FilterConst.FILTERS);
            msgFilterGrp.setReplace(true);
            KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msgFilterGrp);
        }catch (Exception ex) {
            log.error(ex);
        }
    }

    /**
     * Send values of filter columns
     * @param sbeCode SBE code
     */
    public void sendFilterColumns(String sbeCode) {
        try {
            SearchEntity searchBE = CacheUtils.getObject(userToken.getProductCode(), sbeCode, SearchEntity.class);

            if (searchBE != null) {
                QDataBaseEntityMessage msgColumn = filterUtils.getFilterValuesByColum(searchBE);

                msgColumn.setToken(userToken.getToken());
                msgColumn.setReplace(true);
                KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msgColumn);
            }
        }catch (Exception ex) {
            log.error(ex);
        }
    }

    /**
     * Send filter option
     * @param questionCode Question Code
     * @param sbeCode Search Base Entiy Code
     * @param value Selected value
     */
    public void sendFilterOption(String questionCode, String sbeCode,String value) {
        QDataBaseEntityMessage msg = filterUtils.getFilterOptionByCode(questionCode,value);
        String sbeCodeJti =  filterUtils.getCleanSBECode(sbeCode);

        msg.setToken(userToken.getToken());
        msg.setParentCode(Question.QUE_ADD_FILTER_SBE_GRP);
        msg.setLinkCode(Attribute.LNK_CORE);
        msg.setLinkValue(Attribute.LNK_ITEMS);
        msg.setQuestionCode(Question.QUE_FILTER_OPTION);
        msg.setTargetCode(sbeCodeJti);
        msg.setReplace(true);
        KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
    }

    /**
     * Send filter select box or text box value
     * @param queGrp Question group
     * @param queCode Question code
     * @param lnkCode Link Code
     * @param lnkVal Link Value
     * @param attCode Attribute code
     */
    public void sendFilterValue(String queGrp,String queCode, String lnkCode, String lnkVal,String attCode) {
        QDataBaseEntityMessage msg = filterUtils.getSelectBoxValueByCode(queGrp,queCode, lnkCode,lnkVal);

        msg.setToken(userToken.getToken());
        msg.setTargetCode(queCode);
        msg.setMessage(FilterConst.FILTERS);
        msg.setReplace(true);
        msg.setAttributeCode(attCode);

        KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
    }


    /**
     * handle filter by string in the table
     * @param sbeCode Search base entity code without JTI
     * @param params List of filter parameters
     */
    public void handleFilter(String sbeCode, Map<String,SavedSearch> params) {
        String sessionCode = searchUtils.sessionSearchCode(sbeCode);
        String cachedKey = FilterConst.LAST_SEARCH + sessionCode;

        SearchEntity searchBE = CacheUtils.getObject(userToken.getProductCode(), cachedKey, SearchEntity.class);
        excludeExtraFilterBySearchBE(searchBE);

        // add definitions
        List<String> definitions = getListDefinitionCodes(sbeCode);

        for(int i=0;i< definitions.size(); i++){
            if(i== 0) {
                searchBE.add(new Filter(Attribute.LNK_DEF, Operator.CONTAINS, definitions.get(i)));
            } else {
                searchBE.add(new Or(new Filter(Attribute.LNK_DEF, Operator.CONTAINS, definitions.get(i))));
            }
        }

        // add conditions by filter parameters
        setFilterParams(searchBE,params);

        CacheUtils.putObject(userToken.getProductCode(), cachedKey, searchBE);

        String queCode =  sbeCode.replaceFirst(Prefix.SBE_,Prefix.QUE_);
        search.sendTable(queCode);
    }

    /**
     * Remove extra filter parameters to search base entity
     * @param searchBE Search base entity
     */
    public void excludeExtraFilterBySearchBE(SearchEntity searchBE) {
        List<ClauseContainer> clauses = new ArrayList<>(searchBE.getClauseContainers());
        for(ClauseContainer clause : clauses){
            searchBE.remove(clause.getFilter());
        }
    }

    /**
     * Add filer parameters to search base entity
     * @param searchBE Search base entity
     */
    public void setFilterParams(SearchEntity searchBE, Map<String,SavedSearch> params) {
        for(Map.Entry<String,SavedSearch> param : params.entrySet()) {
            String strJson = jsonb.toJson(param.getValue());
            SavedSearch ss = jsonb.fromJson(strJson, SavedSearch.class);

            Operator operator = getOperatorByVal(ss.getOperator());
            String value = ss.getValue();
            // like operation
            if (operator.equals(Operator.LIKE)) {
                value = "%" + value + "%";
            }
            // equals operation to link attribute
            if (operator.equals(Operator.EQUALS) && ss.getColumn().contains(Prefix.LNK_)) {
                value = "[\"" + Prefix.SEL_ + value + "\"]";
            }

            Filter filter = null;
            if (ss.getDataType().equalsIgnoreCase(FilterConst.DATETIME)) {
                LocalDateTime dateTime = parseStringToDate(value);
                filter = new Filter(ss.getColumn(),operator, dateTime);
            } else if (ss.getDataType().equalsIgnoreCase(FilterConst.YES_NO)) {
                filter = new Filter(ss.getColumn(),Boolean.valueOf(value.equalsIgnoreCase("YES")?true:false));
            } else {
                filter = new Filter(ss.getColumn(),operator, value);
            }

            searchBE.remove(filter);
            searchBE.add(filter);
        }
    }

    /**
     * Get Search filter by filter value
     * @param filterVal Filter value
     * @return Get Search filter by filter value
     */
    public Operator getOperatorByVal(String filterVal){
        if(filterVal.equalsIgnoreCase(FilterConst.SEL_GREATER_THAN.replaceFirst(Prefix.SEL_,""))){
            return Operator.GREATER_THAN;
        }
        if(filterVal.equalsIgnoreCase(FilterConst.SEL_GREATER_THAN_OR_EQUAL_TO.replaceFirst(Prefix.SEL_,""))){
            return Operator.GREATER_THAN_OR_EQUAL;
        }
        if(filterVal.equalsIgnoreCase(FilterConst.SEL_LESS_THAN.replaceFirst(Prefix.SEL_,""))){
            return Operator.LESS_THAN;
        }
        if(filterVal.equalsIgnoreCase(FilterConst.SEL_LESS_THAN_OR_EQUAL_TO.replaceFirst(Prefix.SEL_,""))){
            return Operator.LESS_THAN_OR_EQUAL;
        }
        if(filterVal.equalsIgnoreCase(FilterConst.SEL_EQUAL_TO.replaceFirst(Prefix.SEL_,""))){
            return Operator.EQUALS;
        }
        if(filterVal.equalsIgnoreCase(FilterConst.SEL_NOT_EQUAL_TO.replaceFirst(Prefix.SEL_,""))){
            return Operator.NOT_EQUALS;
        }
        if(filterVal.equalsIgnoreCase(FilterConst.SEL_LIKE.replaceFirst(Prefix.SEL_,""))){
            return Operator.LIKE;
        }
        if(filterVal.equalsIgnoreCase(FilterConst.SEL_NOT_LIKE.replaceFirst(Prefix.SEL_,""))){
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
     * Send dropdown options data
     * @param group Question group code
     * @param code Question code
     */
    public void sendListSavedSearches(String group,String code) {
        String sbeCode = SearchEntity.SBE_SAVED_SEARCH;
        SearchEntity searchEntity = filterUtils.getListSavedSearch(sbeCode,Attribute.PRI_NAME,FilterConst.VALUE);
        QDataBaseEntityMessage msg = getBaseItemsMsg(group,code,Attribute.PRI_NAME,FilterConst.VALUE,searchEntity);
        KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
    }

    /**
     * Get Message of list base entity
     * @param group Question group code
     * @param code Question code
     * @param lnkCode Link code
     * @param lnkValue Link value
     * @param search Search entity
     * @return Message of list base entity
     */
    public QDataBaseEntityMessage getBaseItemsMsg(String group,String code,String lnkCode,String lnkValue,
                                                  SearchEntity search) {
        QDataBaseEntityMessage msg = new QDataBaseEntityMessage();

        try {
            List<BaseEntity> bases = searchUtils.searchBaseEntitys(search);

            msg.setToken(userToken.getToken());
            msg.setItems(bases);
            msg.setParentCode(group);
            msg.setQuestionCode(code);
            msg.setLinkCode(lnkCode);
            msg.setLinkValue(lnkValue);
            msg.setReplace(true);
        }catch(Exception ex) {
            log.error(ex);
        }

        return msg;
    }

    /**
     * Send a partial PCM with the correct search code.
     *
     * @param pcmCode The code of pcm to send
     * @param loc The location code
     * @param queValCode Question value code
     * @param queValCode Question value code
     */
    public void sendPartialPCM(String pcmCode,String loc,String queValCode) {
        BaseEntity pcm = beUtils.getBaseEntity(pcmCode);
        for(EntityAttribute ea : pcm.getBaseEntityAttributes()) {
            if(ea.getAttributeCode().equalsIgnoreCase(loc)) {
                ea.setValue(queValCode);
                ea.setValueString(queValCode);
            }
        }
        sendBaseEntity(pcm);
    }

    /**
     * Send fitler details by base entity
     * @param parentCode Parent code
     * @param queCode Question code
     * @param params Parameters
     */
    public void sendFilterDetailsByBase(BaseEntity base,String parentCode,String queCode,Map<String,SavedSearch> params) {
        for (Map.Entry<String,SavedSearch> param : params.entrySet()) {
            String strJson = jsonb.toJson(param.getValue());
            SavedSearch ss = jsonb.fromJson(strJson, SavedSearch.class);

            Attribute att= qwandaUtils.getAttribute(ss.getColumn());
            StringBuilder valBuild = new StringBuilder(ss.getColumn());
            valBuild.append(FilterConst.SEPARATOR+ss.getOperator());
            valBuild.append(FilterConst.SEPARATOR+ss.getValueCode());

            StringBuilder lblBuild = new StringBuilder(att.getName() + FilterConst.SEPARATOR);
            lblBuild.append(ss.getOperator().replaceFirst(Prefix.SEL_, "").replaceAll("_"," "));
            lblBuild.append(FilterConst.SEPARATOR + ss.getValue().replaceFirst(Prefix.SEL_, ""));

            EntityAttribute ea = new EntityAttribute(base, att, 1.0);
            ea.setAttributeName(lblBuild.toString());
            ea.setValue(valBuild.toString());
            ea.setValueString(valBuild.toString());

            base.getBaseEntityAttributes().add(ea);
        }

        sendBaseEntity(base,parentCode,queCode,false);
    }

    /**
     * Send base entity
     * @param baseEntity Base entity
     */
    public void sendBaseEntity(BaseEntity baseEntity,String parentCode,String queCode,boolean replaced) {
        QDataBaseEntityMessage msg = new QDataBaseEntityMessage(baseEntity);
        msg.setToken(userToken.getToken());
        msg.setParentCode(parentCode);
        msg.setQuestionCode(queCode);
        msg.setReplace(replaced);
        KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
    }

    /**
     * Send base entity
     * @param baseEntity Base entity
     */
    public void sendBaseEntity(BaseEntity baseEntity) {
        QDataBaseEntityMessage msg = new QDataBaseEntityMessage(baseEntity);
        msg.setToken(userToken.getToken());
        msg.setReplace(true);
        KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
    }

    /**
     * Send dropdown options data
     * @param code Question code
     * @param lnkCode Link code
     * @param lnkValue Link value
     */
    public void sendListQuickSearches(String queGrp, String code,String sbeCode,String lnkCode,String lnkValue,
                                      String typing,List<String> defs) {
        SearchEntity searchEntity = filterUtils.getListQuickSearches(sbeCode,lnkCode,lnkValue,typing,defs);
        QDataBaseEntityMessage msg = getBaseItemsMsg(queGrp,code,lnkCode,lnkValue,searchEntity);
        KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
    }

    /**
     * Initialize Cache
     * @param queCode Question code
     */
    public void init(String queCode) {
        clearParamsInCache();
        String sbe = queCode.replaceFirst(Prefix.QUE_,Prefix.SBE_);
        CacheUtils.putObject(userToken.getProductCode(),getCachedSbeTable(), sbe);

        sendFilterColumns(sbe);
        sendListSavedSearches(Question.QUE_SAVED_SEARCH_SELECT_GRP,Question.QUE_SAVED_SEARCH_SELECT);
    }

    /**
     * Clear params in cache
     */
    public void clearParamsInCache() {
        Map<String, SavedSearch> params = new HashMap<>();
        CacheUtils.putObject(userToken.getProductCode(),getCachedAnswerKey(),params);
    }

    /**
     * Get sbe code from cache
     * @return sbe code from cache
     */
    public String getSbeTableFromCache() {
        String sbe = CacheUtils.getObject(userToken.getProductCode(),getCachedSbeTable() ,String.class);
        if(sbe == null) {
            return "";
        }
        return sbe;
    }

    /**
     * Cached sbe table code
     * @return Sbe table code
     */
    public String getCachedSbeTable() {
        String key = FilterConst.LAST_SBE_TABLE + ":" + userToken.getUserCode();
        return key;
    }

    /**
     * Cached answer key
     * @return Cached answer key
     */
    public String getCachedAnswerKey() {
        String key = FilterConst.LAST_ANSWERS_MAP + ":" + userToken.getUserCode();
        return key;
    }

    /**
     * Build process data
     * @return Process data
     */
    public ProcessData  buildProcessData() {

        // construct
        ProcessData processData = new ProcessData();
        processData.setSourceCode(userToken.getUserCode());
        processData.setTargetCode(userToken.getUserCode());

        // set pcm
        processData.setPcmCode(PCM.PCM_PROCESS);
        processData.setParent(PCM.PCM_CONTENT);
        processData.setLocation(PCM.location(1));

        // get target
        BaseEntity target = beUtils.getBaseEntity(userToken.getUserCode());

        // build and send data
        QBulkMessage msg = dispatch.build(processData);
        msg.add(target);
        dispatch.sendData(msg);

        return processData;
    }
    /**
     * handle filter by string in the table
     * @param params List of filter parameters
     */
    public void handleFilterBucket(Map<String,SavedSearch> params) {
        ProcessData  processData = buildProcessData();

        // send searches
        for (String code : processData.getSearches()) {
            log.info("Sending search: " + code);

            String sessionCode = searchUtils.sessionSearchCode(code);
            String cachedKey = FilterConst.LAST_SEARCH + sessionCode;

            SearchEntity searchBE = CacheUtils.getObject(userToken.getProductCode(), cachedKey, SearchEntity.class);
            excludeExtraFilterBySearchBE(searchBE);

            // add definitions
            List<String> definitions = getListDefinitionCodes(code);

            for(int i=0;i< definitions.size(); i++){
                if(i== 0) {
                    searchBE.add(new Filter(Attribute.LNK_DEF, Operator.CONTAINS, definitions.get(i)));
                } else {
                    searchBE.add(new Or(new Filter(Attribute.LNK_DEF, Operator.CONTAINS, definitions.get(i))));
                }
            }

            // add conditions by filter parameters
            setFilterParams(searchBE,params);

            CacheUtils.putObject(userToken.getProductCode(), cachedKey, searchBE);

            searchUtils.searchTable(code);
        }
    }

    /**
     * Handle quick search
     * @param value Value
     * @param coded being coded or not
     */
    public void handleQuickSearchDropdownByBucket(String value,boolean coded) {
        ProcessData  processData = buildProcessData();

        // send searches
        for (String code : processData.getSearches()) {
            String sessionCode = searchUtils.sessionSearchCode(code);
            String cachedKey = FilterConst.LAST_SEARCH + sessionCode;

            SearchEntity searchBE = CacheUtils.getObject(userToken.getProductCode(), cachedKey, SearchEntity.class);

            clearFilters(searchBE);

            // add definitions
            List<String> definitions = getListDefinitionCodes(code);

            for(int i=0;i< definitions.size(); i++){
                if(i== 0) {
                    searchBE.add(new Filter(Attribute.LNK_DEF, Operator.CONTAINS, definitions.get(i)));
                } else {
                    searchBE.add(new Or(new Filter(Attribute.LNK_DEF, Operator.CONTAINS, definitions.get(i))));
                }
            }

            // searching by text or search by code
            Filter filter = null;
            String newValue = value.replaceFirst("!", "");

            if (coded) {
                filter = new Filter(Attribute.PRI_CODE, Operator.EQUALS, value);
            } else {
                filter = new Filter(Attribute.PRI_NAME, Operator.LIKE, "%" + newValue + "%");
            }

            searchBE.remove(filter);
            searchBE.add(filter);

            CacheUtils.putObject(userToken.getProductCode(), cachedKey, searchBE);

            searchUtils.searchTable(code);
        }
    }

    /**
     * Return the list of base entity code
     * @param sbeCode Search base entity code
     * @return List of base entity code
     */
    public List<String> getListDefinitionCodes(String sbeCode) {
        List<String> definitions = new ArrayList<>();

        // bucket page
        if (SearchEntity.SBE_PROCESS.equals(sbeCode)) {
            addDefinitionCodeByBucket(definitions);
        // Table
        } else {
            String defCode = getDefinitionCode(sbeCode);
            if(!defCode.isEmpty()) {
                definitions.add(defCode);
            }
        }

        return definitions;
    }

    /**
     * Return definition code
     * @param sbeCode search base entity code
     * @return definition code
     */
    public String getDefinitionCode(String sbeCode) {
        SearchEntity search = CacheUtils.getObject(userToken.getProductCode(),sbeCode,SearchEntity.class);
        List<ClauseContainer> clauses = search.getClauseContainers();
        for (ClauseContainer clause : clauses) {
            if (clause.getFilter().getCode().equals(Attribute.LNK_DEF)) {
                return clause.getFilter().getValue().toString();
            }
        }
        return "";
    }

	/**
     * Add definition to the list of definition codes
     * @param definitions List of definition codes
     */
    public void addDefinitionCodeByBucket(List<String> definitions) {
        PCM pcm = beUtils.getPCM(PCM.PCM_PROCESS);

        List<EntityAttribute> locations = pcm.findPrefixEntityAttributes(Prefix.PRI_LOC);
        for (EntityAttribute entityAttribute : locations) {
            String value = entityAttribute.getAsString();
            if (value.startsWith(Prefix.SBE_)) {
                String defCode = getDefinitionCode(value);
                if(!defCode.isEmpty()) {
                    definitions.add(defCode);
                }
            }
        }
    }
}
