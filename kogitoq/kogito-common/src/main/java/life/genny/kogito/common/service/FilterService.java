package life.genny.kogito.common.service;

import life.genny.qwandaq.constants.FilterConst;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.search.clause.Or;
import life.genny.qwandaq.graphql.ProcessData;
import life.genny.qwandaq.message.*;
import life.genny.qwandaq.models.SavedSearch;
import life.genny.qwandaq.utils.FilterUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.utils.SearchUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.BaseEntityUtils;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.enterprise.context.ApplicationScoped;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;

import life.genny.qwandaq.entity.search.clause.ClauseContainer;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.PCM;
import life.genny.qwandaq.constants.Prefix;
import life.genny.kogito.common.core.Dispatch;

@ApplicationScoped
public class FilterService {
    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());
    static Jsonb jsonb = JsonbBuilder.create();

    @Inject
    UserToken userToken;

    @Inject
    FilterUtils filterUtils;

    @Inject
    BaseEntityUtils beUtils;

    @Inject
    QwandaUtils qwandaUtils;

    @Inject
    SearchService search;
    @Inject
    SearchUtils searchUtils;

    @Inject
    Dispatch dispatch;

    /**
     * Get the list of bucket codes with session id
     * @param originBucketCodes List of bucket codes
     * @return The list of bucket code with session id
     */
    public List<String> getBucketCodesBySearchEntity(List<String> originBucketCodes){
        List<String> bucketCodes = new ArrayList<>();
        originBucketCodes.stream().forEach(e -> {
            SearchEntity searchEntity = CacheUtils.getObject(userToken.getProductCode(),e, SearchEntity.class);
            String searchCode = searchEntity.getCode() + "_" + userToken.getJTI().toUpperCase();
            bucketCodes.add(searchCode);
        });

        return bucketCodes;
    }

    /**
     * Send search message to front-end
     * @param searchBE Search base entity from cache
     */
    public void sendMessageBySearchEntity(SearchEntity searchBE) {
        QSearchMessage searchBeMsg = new QSearchMessage(searchBE);
        searchBeMsg.setToken(userToken.getToken());
        KafkaUtils.writeMsg(KafkaTopic.SEARCH_EVENTS, searchBeMsg);
    }

    /**
     * create new entity attribute by attribute code, name and value
     * @param attrCode Attribute code
     * @param attrName Attribute name
     * @param value Attribute value
     * @return return json object
     */
    public EntityAttribute createEntityAttributeBySortAndSearch(String attrCode, String attrName, Object value){
        EntityAttribute ea = null;
        try {
            BaseEntity base = beUtils.getBaseEntity(attrCode);
            Attribute attribute = qwandaUtils.getAttribute(attrCode);
            ea = new EntityAttribute(base, attribute, 1.0, attrCode);
            if(!attrName.isEmpty()) {
                ea.setAttributeName(attrName);
            }
            if(value instanceof String) {
                ea.setValueString(value.toString());
            }
            if(value instanceof Integer) {
                ea.setValueInteger((Integer) value);
            }

            base.addAttribute(ea);
        } catch(Exception ex){
            log.error(ex);
        }
        return ea;
    }

    /**
     * Send a search PCM with the correct search code.
     *
     * @param pcmCode The code of pcm to send
     * @param searchCode The code of the searhc to send
     */
    public void sendSearchPCM(String pcmCode, String searchCode) {

        // update content
        BaseEntity content = beUtils.getBaseEntity("PCM_CONTENT");
        Attribute attribute = qwandaUtils.getAttribute("PRI_LOC1");
        EntityAttribute ea = new EntityAttribute(content, attribute, 1.0, pcmCode);
        content.addAttribute(ea);

        // update target pcm
        BaseEntity pcm = beUtils.getBaseEntity(pcmCode);
        ea = new EntityAttribute(pcm, attribute, 1.0, searchCode);
        pcm.addAttribute(ea);

        // send to alyson
        QDataBaseEntityMessage msg = new QDataBaseEntityMessage(content);
        msg.add(pcm);
        msg.setToken(userToken.getToken());
        msg.setReplace(true);
        KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
    }

    /**
     * Handle search text in bucket page
     * @param code Message code
     * @param name Message name
     * @param value Search text
     * @param targetCodes List of target codes
     */
    public void handleBucketSearch(String code, String name,String value, List<String> targetCodes) {
        for(String targetCode : targetCodes) {
            SearchEntity searchBE = CacheUtils.getObject(userToken.getProductCode(), targetCode, SearchEntity.class);
            EntityAttribute ea = createEntityAttributeBySortAndSearch(code, name, value);

            //remove searching text and filter
            Filter searchText = new Filter(code, Operator.LIKE, value);

            searchBE.remove(searchText);

            //searching text
            if (!name.isBlank()) {
                searchBE.add(new Filter(code, Operator.LIKE, value));
            }


            CacheUtils.putObject(userToken.getProductCode(), targetCode, searchBE);

            sendMessageBySearchEntity(searchBE);
            sendSearchPCM(PCM.PCM_PROCESS, targetCode);
        }
    }

    /**
     * Handle quick search
     * @param value Value
     * @param targetCode Target code
     */
    public void handleQuickSearch(String value, String targetCode) {
        String sessionCode = searchUtils.sessionSearchCode(targetCode);
        String cachedKey = FilterConst.LAST_SEARCH + sessionCode;

        SearchEntity searchBE = CacheUtils.getObject(userToken.getProductCode(), cachedKey, SearchEntity.class);

        // searching text
        String newValue = value.replaceFirst("!","");
        Filter filter = new Filter(Attribute.PRI_NAME, Operator.LIKE, "%" + newValue + "%");
        searchBE.remove(filter);
        searchBE.add(filter);

        CacheUtils.putObject(userToken.getProductCode(), cachedKey, searchBE);

        String queCode =  targetCode.replaceFirst(Prefix.SBE,Prefix.QUE);
        search.sendTable(queCode);

    }

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
                searchBE.add(new Filter(Attribute.LNK_DEF, Operator.STARTS_WITH, definitions.get(i)));
            } else searchBE.add(new Or(new Filter(Attribute.LNK_DEF, Operator.STARTS_WITH, definitions.get(i))));
        }

        // searching by text or search by code
        Filter filter = null;
        String newValue = value.replaceFirst("!","");
        if(coded) {
            filter = new Filter(Attribute.PRI_CODE, Operator.EQUALS, value);
        }else {
            filter = new Filter(Attribute.PRI_NAME, Operator.LIKE, "%" + newValue + "%");
        }
        searchBE.remove(filter);
        searchBE.add(filter);

        CacheUtils.putObject(userToken.getProductCode(), cachedKey, searchBE);

        String queCode =  targetCode.replaceFirst(Prefix.SBE,Prefix.QUE);
        search.sendTable(queCode);
    }

    /**
     * Clearn filers from search base entity
     * @param searchBE Search base entity
     */
    public void clearFilters(SearchEntity searchBE) {
        List<ClauseContainer> cloneList = new ArrayList(searchBE.getClauseContainers());

        for(ClauseContainer cc : cloneList) {
            searchBE.remove(cc.getFilter());
        }
    }

    /**
     * Send filter group and filter column for filter function
     * @param sbeCode SBE code
     * @param queGrp Question group code
     * @param questionCode Question code
     * @param addedJti Adding JTI to search base entity
     */
    public void sendFilterGroup(String sbeCode, String queGrp,String questionCode,boolean addedJti,String filterCode,
                                Map<String, Map<String,String>> listFilterParams) {
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
     * Send filter group and filter column for filter function
     * @param queGrp Question group code
     * @param questionCode Question code
     * @param filterCode Filter code
     * @param listFilterParams Filter parameters
     */
    public void sendAddFilterGroup(String queGrp,String questionCode, String filterCode,
                                   Map<String, Map<String,String>> listFilterParams) {
        try {
            Ask ask = filterUtils.getFilterGroup(questionCode, filterCode, listFilterParams);
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
     * @param attCode Attribute code
     */
    public void sendFilterOption(String questionCode, String sbeCode,String attCode) {
        QDataBaseEntityMessage msg = filterUtils.getFilterOptionByCode(questionCode,attCode);
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
                searchBE.add(new Filter(Attribute.LNK_DEF, Operator.STARTS_WITH, definitions.get(i)));
            } else searchBE.add(new Or(new Filter(Attribute.LNK_DEF, Operator.STARTS_WITH, definitions.get(i))));
        }

        // add conditions by filter parameters
        setFilterParams(searchBE,params);

        CacheUtils.putObject(userToken.getProductCode(), cachedKey, searchBE);

        String queCode =  sbeCode.replaceFirst(Prefix.SBE,Prefix.QUE);
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
     * Get parameter value by key
     * @param filterParams Filter Parameters
     * @param key Parameter Key
     */
    public String getFilterParamValByKey(Map<String, String> filterParams, String key) {
        String value = "";
        if (filterParams == null)
            return value;
        if (filterParams.containsKey(key)) {
            value = filterParams.get(key).toString();
        }
        String finalVal = value.replace("\"", "")
                .replace("[", "").replace("]", "")
                .replaceFirst(FilterConst.SEL_FILTER_COLUMN_FLC, "");

        return finalVal;
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
            if (operator.equals(Operator.LIKE)) {
                value = "%" + value + "%";
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
     * Send message to bucket page with filter data
     * @param queGroup Question group
     * @param queCode Question code
     */
    public void sendQuickSearch(String queGroup,String queCode,String attCode, String targetCode) {
        Ask ask = new Ask();
        ask.setName(queGroup);
        Attribute attribute = new Attribute(Attribute.QQQ_QUESTION_GROUP,Attribute.QQQ_QUESTION_GROUP,new DataType());
        Question question = new Question(queGroup,queGroup,attribute);
        ask.setQuestion(question);

        Ask childAsk = new Ask();
        childAsk.setName(queCode);
        childAsk.setQuestionCode(queCode);
        Question childQuestion = new Question();
        childQuestion.setAttributeCode(attCode);
        childQuestion.setCode(queCode);
        childAsk.setAttributeCode(attCode);

        Attribute childAttr = qwandaUtils.getAttribute(attCode);
        childAttr.setCode(attCode);
        childAttr.setName(attCode);
        childQuestion.setAttribute(childAttr);

        childAsk.setQuestion(childQuestion);
        childAsk.setTargetCode(targetCode);

        ask.add(childAsk);

        QDataAskMessage msg = new QDataAskMessage(ask);
        msg.setToken(userToken.getToken());
        msg.setTargetCode(targetCode);
        msg.setQuestionCode(queGroup);
        msg.setMessage(queGroup);
        msg.setReplace(true);
        KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);

        sendQuickSearchItems(SearchEntity.SBE_DROPDOWN,queGroup,queCode,Attribute.PRI_NAME, "");
    }

    /**
     * Send message to bucket page with filter data
     * @param queGroup Question group
     * @param queCode Question code
     */
    public void sendQuickSearchItems(String sbeCode, String queGroup,String queCode,String lnkCode, String lnkValue) {
        SearchEntity searchEntity = filterUtils.getQuickOptions(sbeCode,lnkCode,lnkValue);
        QDataBaseEntityMessage msg = getBaseItemsMsg(queGroup,queCode,lnkCode,lnkValue,searchEntity);
        KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
    }

    /**
     * Send command message type
     * @param cmdType Cmd Type
     * @param code Code
     */
    public void sendCmdMsgByCodeType(String cmdType, String code){
        QCmdMessage msg = new QCmdMessage(cmdType,code);
        msg.setToken(userToken.getToken());
        msg.setSourceCode(cmdType);
        msg.setTargetCode(code);
        KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
    }

    /**
     * Return the list of search entity code
     * @param searchCode Search entity code
     * @return the list of search entity code
     */
    public List<String> getBucketCodesBySBE(String searchCode) {
        List<String> originBucketCodes = CacheUtils.getObject(userToken.getProductCode(), searchCode, List.class);
        List<String>  bucketCodes = getBucketCodesBySearchEntity(originBucketCodes);

        return bucketCodes;
    }

    /**
     * Get Search filter by filter value
     * @param filterVal Filter value
     * @return Get Search filter by filter value
     */
    public Operator getOperatorByVal(String filterVal){
        if(filterVal.equalsIgnoreCase(FilterConst.SEL_GREATER_THAN.replaceFirst(Prefix.SEL,""))){
            return Operator.GREATER_THAN;
        }
        if(filterVal.equalsIgnoreCase(FilterConst.SEL_GREATER_THAN_OR_EQUAL_TO.replaceFirst(Prefix.SEL,""))){
            return Operator.GREATER_THAN_OR_EQUAL;
        }
        if(filterVal.equalsIgnoreCase(FilterConst.SEL_LESS_THAN.replaceFirst(Prefix.SEL,""))){
            return Operator.LESS_THAN;
        }
        if(filterVal.equalsIgnoreCase(FilterConst.SEL_LESS_THAN_OR_EQUAL_TO.replaceFirst(Prefix.SEL,""))){
            return Operator.LESS_THAN_OR_EQUAL;
        }
        if(filterVal.equalsIgnoreCase(FilterConst.SEL_EQUAL_TO.replaceFirst(Prefix.SEL,""))){
            return Operator.EQUALS;
        }
        if(filterVal.equalsIgnoreCase(FilterConst.SEL_NOT_EQUAL_TO.replaceFirst(Prefix.SEL,""))){
            return Operator.NOT_EQUALS;
        }
        if(filterVal.equalsIgnoreCase(FilterConst.SEL_LIKE.replaceFirst(Prefix.SEL,""))){
            return Operator.LIKE;
        }
        if(filterVal.equalsIgnoreCase(FilterConst.SEL_NOT_LIKE.replaceFirst(Prefix.SEL,""))){
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
        if(questionCode.contains(FilterConst.DATETIME)) return true;

        return false;
    }

    /**
     * Send dropdown options data
     * @param group Question group code
     * @param code Question code
     * @param lnkCode Link code
     * @param lnkValue Link value
     */
    public void sendListSavedSearches(String group,String code,String lnkCode,String lnkValue) {
        String sbeCode = SearchEntity.SBE_SAVED_SEARCH;
        SearchEntity searchEntity = filterUtils.getListSavedSearch(sbeCode,lnkCode,lnkValue, true);
        QDataBaseEntityMessage msg = getBaseItemsMsg(group,code,lnkCode,lnkValue,searchEntity);
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
            msg.setToken(userToken.getToken());

            List<BaseEntity> bases = searchUtils.searchBaseEntitys(search);
            List<BaseEntity> basesSorted = bases.stream().sorted(Comparator.comparing(BaseEntity::getId).reversed())
                    .collect(Collectors.toList());

            msg.setItems(basesSorted);
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
     * Return the list of dropdown items
     * @param sbeCode Search base entity code
     * @param lnkCode Link code
     * @param lnkValue Link value
     * @return The list of dropdown items
     */
    public List<BaseEntity> getListSavedSearches(String sbeCode,String lnkCode,String lnkValue) {
        String sbeJti = getSearchBaseEntityCodeByJTI(SearchEntity.SBE_SAVED_SEARCH);
        SearchEntity search = filterUtils.getListSavedSearch(sbeJti,lnkCode,lnkValue, true);
        List<BaseEntity> bases = searchUtils.searchBaseEntitys(search);
        return bases;
    }

    /**
     * Return search base entity code with jti
     *
     * @param sbeCode Search Base entity
     * @return Search base entity with jti
     */
    public String getSearchBaseEntityCodeByJTI(String sbeCode) {
        String newSbeCode = filterUtils.getSearchBaseEntityCodeByJTI(sbeCode);
        return newSbeCode;
    }

    /**
     * Strip search base entity code without jti
     *
     * @param orgSbe Original search base entity code
     * @return Search base entity code without jti
     */
    public String getCleanSBECode(String orgSbe) {
        return filterUtils.getCleanSBECode(orgSbe);
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
            lblBuild.append(ss.getOperator().replaceFirst(Prefix.SEL, "").replaceAll("_"," "));
            lblBuild.append(FilterConst.SEPARATOR + ss.getValue().replaceFirst(Prefix.SEL, ""));

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
    public void sendBaseEntity(BaseEntity baseEntity,String parentCode,String queCode) {
        QDataBaseEntityMessage msg = new QDataBaseEntityMessage(baseEntity);
        msg.setToken(userToken.getToken());
        msg.setParentCode(parentCode);
        msg.setQuestionCode(queCode);
        msg.setReplace(true);
        KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
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
     * Send base entity
     * @param baseEntities List of base entities
     * @param parentCode Parent code
     * @param queCode Question Code
     * @param replaced Replaced message or not
     */
    public void sendBaseEntity(List<BaseEntity> baseEntities,String parentCode,String queCode,boolean replaced) {
        QDataBaseEntityMessage msg = new QDataBaseEntityMessage();

        msg.setToken(userToken.getToken());
        msg.setParentCode(parentCode);
        msg.setQuestionCode(queCode);

        msg.setItems(baseEntities);
        msg.setReplace(replaced);
        KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
    }

    /**
     * Send ask
     * @param ask Ask
     * @param queGrp question group
     * @param replaced Replaced message
     */
    public void sendAsk(Ask ask,String queGrp, boolean replaced) {
        QDataAskMessage msg = new QDataAskMessage(ask);

        msg.setToken(userToken.getToken());
        msg.setTargetCode(queGrp);
        msg.setMessage(queGrp);
        msg.setReplace(replaced);
        KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
    }

    /**
     * Return the link value code
     * @param value Filter Value
     * @return Return the link value code
     */
    public String getLinkValueCode(String value) {
        return  filterUtils.getLinkValueCode(value);
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
        String sbe = queCode.replaceFirst(Prefix.QUE,Prefix.SBE);
        sendFilterColumns(sbe);
        CacheUtils.putObject(userToken.getProductCode(),getCachedSbeTable(), sbe);

        sendListSavedSearches(Question.QUE_SAVED_SEARCH_SELECT_GRP,
                Question.QUE_SAVED_SEARCH_SELECT, Attribute.PRI_NAME,FilterConst.VALUE);

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
        if(sbe == null) return sbe = "";
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
        PCM pcm = beUtils.getPCM(PCM.PCM_PROCESS);

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
        QBulkMessage msg = dispatch.build(processData, pcm);
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
                    searchBE.add(new Filter(Attribute.LNK_DEF, Operator.STARTS_WITH, definitions.get(i)));
                } else searchBE.add(new Or(new Filter(Attribute.LNK_DEF, Operator.STARTS_WITH, definitions.get(i))));
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
                    searchBE.add(new Filter(Attribute.LNK_DEF, Operator.STARTS_WITH, definitions.get(i)));
                } else searchBE.add(new Or(new Filter(Attribute.LNK_DEF, Operator.STARTS_WITH, definitions.get(i))));
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
        if(SearchEntity.SBE_PROCESS.equals(sbeCode)) {
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
        for(ClauseContainer clause : clauses) {
            if(clause.getFilter().getCode().equals(Attribute.LNK_DEF)) {
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

        List<EntityAttribute> locations = pcm.findPrefixEntityAttributes(Prefix.LOCATION);
        for (EntityAttribute entityAttribute : locations) {
            String value = entityAttribute.getAsString();
            if (value.startsWith(Prefix.SBE)) {
                String defCode = getDefinitionCode(value);
                if(!defCode.isEmpty()) {
                    definitions.add(defCode);
                }
            }
        }
    }
}