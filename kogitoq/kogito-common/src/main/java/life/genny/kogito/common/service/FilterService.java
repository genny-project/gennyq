package life.genny.kogito.common.service;

import life.genny.qwandaq.constants.FilterConst;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.utils.*;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.enterprise.context.ApplicationScoped;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

import life.genny.qwandaq.entity.search.clause.ClauseContainer;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.message.QCmdMessage;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.message.QSearchMessage;
import life.genny.qwandaq.entity.BaseEntity;


@ApplicationScoped
public class FilterService {
    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

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

    public static final String SEPARATOR = ";";

    public static enum Options {
        PAGINATION,
        SEARCH,
        FILTER,
        PAGINATION_BUCKET
    }

    /**
     * Send the list of bucket codes to frond-end
     * @param bucketCodes The list of bucket codes
     */
    public void sendBucketCodes(List<String> bucketCodes) {
        QCmdMessage msgProcess = new QCmdMessage(FilterConst.BUCKET_DISPLAY,FilterConst.BUCKET_PROCESS);
        msgProcess.setToken(userToken.getToken());
        KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msgProcess);

        QCmdMessage msgCodes = new QCmdMessage(FilterConst.BUCKET_CODES,FilterConst.BUCKET_CODES);
        msgCodes.setToken(userToken.getToken());
        msgCodes.setSourceCode(FilterConst.BUCKET_CODES);
        msgCodes.setTargetCodes(bucketCodes);
        KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msgCodes);
    }

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
     * handle sorting, searching in the table
     * @param code Attribute code
     * @param attrName Attribute name
     * @param value  Value String
     * @param targetCode Target code
     */
    public void handleSortAndSearch(String code, String attrName,String value, String targetCode, Options ops) {
        SearchEntity searchBE = CacheUtils.getObject(userToken.getProductCode(), targetCode, SearchEntity.class);

        if(ops.equals(Options.SEARCH)) {
            EntityAttribute ea = createEntityAttributeBySortAndSearch(code, attrName, value);

            if (ea != null && attrName.isBlank()) { //sorting
                searchBE.removeAttribute(code);
                searchBE.addAttribute(ea);
            }

            if (!attrName.isBlank()) { //searching text
                Filter filter = new Filter(code, Operator.LIKE, value);
                searchBE.remove(filter);
                searchBE.add(filter);
            }

        }else if(ops.equals(Options.PAGINATION) || ops.equals(Options.PAGINATION_BUCKET)) { //pagination
            Optional<EntityAttribute> aeIndex = searchBE.findEntityAttribute(FilterConst.PAGINATION_INDEX);
            Integer pageSize = searchBE.getPageSize();
            Integer indexVal = 0;
            Integer pagePos = 0;

            if(aeIndex.isPresent() && pageSize !=null) {
                if(code.equalsIgnoreCase(FilterConst.PAGINATION_NEXT) ||
                        code.equalsIgnoreCase(FilterConst.QUE_TABLE_LAZY_LOAD)) {
                    indexVal = aeIndex.get().getValueInteger() + 1;
                } else if (code.equalsIgnoreCase(FilterConst.PAGINATION_PREV)) {
                    indexVal = aeIndex.get().getValueInteger() - 1;
                }

                pagePos = (indexVal - 1) * pageSize;
            }
            //initial stage of bucket pagination
            else if (aeIndex.isEmpty() && code.equalsIgnoreCase(FilterConst.QUE_TABLE_LAZY_LOAD)) {
                indexVal = 2;
                pagePos = pageSize;
            }

            searchBE.setPageStart(pagePos);
            searchBE.setPageIndex(indexVal);
        }
        CacheUtils.putObject(userToken.getProductCode(), targetCode, searchBE);


        if(ops.equals(Options.PAGINATION_BUCKET)) {
            sendCmdMsgByCodeType(FilterConst.BUCKET_DISPLAY, FilterConst.NONE);
            sendMessageBySearchEntity(searchBE);
        } else {
            sendMessageBySearchEntity(searchBE);
            sendSearchPCM(FilterConst.PCM_TABLE, targetCode);
        }
    }


    /**
     * Handle search text in bucket page
     * @param code Message code
     * @param name Message name
     * @param value Search text
     * @param targetCodes List of target codes
     */
    public void handleBucketSearch(String code, String name,String value, List<String> targetCodes) {
        sendBucketCodes(targetCodes);

        for(String targetCode : targetCodes) {
            SearchEntity searchBE = CacheUtils.getObject(userToken.getProductCode(), targetCode, SearchEntity.class);
            EntityAttribute ea = createEntityAttributeBySortAndSearch(code, name, value);

            //remove searching text and filter
            Filter searchText = new Filter(code, Operator.LIKE, value);
            Filter filter = new Filter(FilterConst.PRI_ASSOC_HC, Operator.EQUALS, value);

            searchBE.remove(searchText);
            searchBE.remove(filter);

            //searching text
            if (!name.isBlank()) {
                searchBE.add(new Filter(code, Operator.LIKE, value));
            }

            //filter by select box
            if (code.equalsIgnoreCase(FilterConst.LNK_PERSON)) {
                searchBE.add(filter);
            }

            CacheUtils.putObject(userToken.getProductCode(), targetCode, searchBE);

            sendMessageBySearchEntity(searchBE);
            sendSearchPCM(FilterConst.PCM_PROCESS, targetCode);
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

    public void sendFilterDetailsByGroup(String queGrp,String filterCode,Map<String, Map<String,String>> listParam) {
        Ask ask = filterUtils.getFilterDetailsGroup(queGrp,filterCode, listParam);
        sendAsk(ask,queGrp, false);
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
     */
    public void sendFilterOption(String questionCode, String sbeCode) {
        QDataBaseEntityMessage msg = filterUtils.getFilterOptionByCode(questionCode);
        String sbeCodeJti =  filterUtils.getCleanSBECode(sbeCode);

        msg.setToken(userToken.getToken());
        msg.setParentCode(FilterConst.QUE_ADD_FILTER_SBE_GRP);
        msg.setLinkCode(FilterConst.LNK_CORE);
        msg.setLinkValue(FilterConst.LNK_ITEMS);
        msg.setQuestionCode(FilterConst.QUE_FILTER_OPTION);
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
     * @param listFilterParams List of filter parameters
     */
    public void handleFilter(String sbeCode, Map<String,Map<String, String>> listFilterParams) {
        String sessionCode = searchUtils.sessionSearchCode(sbeCode);
        String cachedKey = FilterConst.LAST_SEARCH + sessionCode;

        SearchEntity searchBE = CacheUtils.getObject(userToken.getProductCode(), cachedKey, SearchEntity.class);

        excludeExtraFilterBySearchBE(searchBE);

        // add conditions by filter parameters
        setFilterParams(searchBE,listFilterParams);

        CacheUtils.putObject(userToken.getProductCode(), cachedKey, searchBE);

        String queCode =  sbeCode.replaceFirst(FilterConst.SBE_PREF,FilterConst.QUE_PREF);
        search.sendTable(queCode);
    }

    /**
     * Remove extra filter parameters to search base entity
     * @param searchBE Search base entity
     */
    public void excludeExtraFilterBySearchBE(SearchEntity searchBE) {
        List<ClauseContainer> clauses = new ArrayList<>(searchBE.getClauseContainers());
        for(ClauseContainer clause : clauses){
//            if(clause.getFilter().getType() == Filter.FILTER_TYPE.EXTRA) {
                searchBE.remove(clause.getFilter());
//            }
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
    public void setFilterParams(SearchEntity searchBE, Map<String,Map<String, String>> listFilterParams) {
        for(Map.Entry<String, Map<String,String>> e : listFilterParams.entrySet()) {
            String queCode = getFilterParamValByKey(e.getValue(), FilterConst.QUESTIONCODE);
            String operatorCode = getFilterParamValByKey(e.getValue(), FilterConst.OPTION);
            String value = getFilterParamValByKey(e.getValue(), FilterConst.VALUE);
            String field = getFilterParamValByKey(e.getValue(), FilterConst.COLUMN);
            Operator operator = getOperatorByVal(operatorCode);

            if (operator.equals(Operator.LIKE)) {
                value = "%" + value + "%";
            }

            boolean isDate = isDateTimeSelected(queCode);
            Filter filter = null;

            if (isDate) {
                LocalDateTime dateTime = parseStringToDate(value);
                filter = new Filter(field, operator, dateTime);
            } else {
                filter = new Filter(field, operator, value);
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
        ask.setName(FilterConst.FILTER_LABEL);
        Attribute attribute = new Attribute(FilterConst.QUE_QQQ_GROUP,FilterConst.QUE_QQQ_GROUP,new DataType());
        Question question = new Question(queGroup,FilterConst.FILTER_LABEL,attribute);
        ask.setQuestion(question);

        Ask childAsk = new Ask();
        childAsk.setName(FilterConst.FILTER_LABEL);
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
        msg.setMessage(FilterConst.FILTER_LABEL);
        msg.setReplace(true);
        KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);

        sendQuickSearchItems(FilterConst.SBE_DROPDOWN,queGroup,queCode,FilterConst.PRI_NAME, "");
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
        if(filterVal.equalsIgnoreCase(FilterConst.SEL_GREATER_THAN)){
            return Operator.GREATER_THAN;
        }
        if(filterVal.equalsIgnoreCase(FilterConst.SEL_GREATER_THAN_OR_EQUAL_TO)){
            return Operator.GREATER_THAN_OR_EQUAL;
        }
        if(filterVal.equalsIgnoreCase(FilterConst.SEL_LESS_THAN)){
            return Operator.LESS_THAN;
        }
        if(filterVal.equalsIgnoreCase(FilterConst.SEL_LESS_THAN_OR_EQUAL_TO)){
            return Operator.LESS_THAN_OR_EQUAL;
        }
        if(filterVal.equalsIgnoreCase(FilterConst.SEL_EQUAL_TO)){
            return Operator.EQUALS;
        }
        if(filterVal.equalsIgnoreCase(FilterConst.SEL_NOT_EQUAL_TO)){
            return Operator.NOT_EQUALS;
        }
        if(filterVal.equalsIgnoreCase(FilterConst.SEL_LIKE)){
            return Operator.LIKE;
        }
        if(filterVal.equalsIgnoreCase(FilterConst.SEL_NOT_LIKE)){
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
        String sbeJti = getSearchBaseEntityCodeByJTI(FilterConst.SBE_SAVED_SEARCH);
        SearchEntity searchEntity = filterUtils.getListSavedSearch(sbeJti,lnkCode,lnkValue, true);
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

        List<BaseEntity> bases = searchUtils.searchBaseEntitys(search);

        List<BaseEntity> basesSorted =  bases.stream().sorted(Comparator.comparing(BaseEntity::getId).reversed())
                .collect(Collectors.toList());

        msg.setToken(userToken.getToken());
        msg.setItems(basesSorted);
        msg.setParentCode(group);
        msg.setQuestionCode(code);
        msg.setLinkCode(lnkCode);
        msg.setLinkValue(lnkValue);
        msg.setReplace(true);

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
        String sbeJti = getSearchBaseEntityCodeByJTI(FilterConst.SBE_SAVED_SEARCH);
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

    public void sendFilterDetailsByPcm(String pcmCode,String queCode,String attCode,String value) {
        BaseEntity base = beUtils.getBaseEntity(pcmCode);

        for(EntityAttribute ea : base.getBaseEntityAttributes()) {
            if(ea.getAttributeCode().equalsIgnoreCase(FilterConst.PRI_LOC1)) {
                ea.setValue(attCode);
                ea.setValueString(attCode);
            }
        }

        sendBaseEntity(base);
    }

    /**
     * Send fitler details by base entity
     * @param parentCode Parent code
     * @param queCode Question code
     * @param attCode Attribute code
     * @param value Value
     */
    public void sendFilterDetailsByBase(String parentCode,String queCode,String attCode,String value) {
        List<BaseEntity> baseEntities = new ArrayList<>();

        BaseEntity base = new BaseEntity(attCode);
        String  rowVal = queCode + SEPARATOR + attCode + SEPARATOR + value;

        Attribute attribute = qwandaUtils.getAttribute(attCode);
        EntityAttribute ea = new EntityAttribute(base,attribute,1.0);
        ea.setValue(rowVal);
        ea.setValueString(rowVal);

        base.addAttribute(ea);
        baseEntities.add(base);

        sendBaseEntity(baseEntities,parentCode,queCode,false);
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


}
