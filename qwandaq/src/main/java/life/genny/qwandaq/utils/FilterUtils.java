package life.genny.qwandaq.utils;

import static life.genny.qwandaq.attribute.Attribute.PRI_CODE;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.List;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.constants.FilterConst;
import life.genny.qwandaq.entity.search.trait.*;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.entity.search.clause.Or;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;

@ApplicationScoped
public class FilterUtils {
    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    QwandaUtils qwandaUtils;

    @Inject
    BaseEntityUtils beUtils;

    @Inject
    SearchUtils searchUtils;

    @Inject
    UserToken userToken;

    /**
     * Strip search base entity code without jti
     *
     * @param orgSbe Original search base entity code
     * @return Search base entity code without jti
     */
    public String getCleanSBECode(String orgSbe) {
        String sbe = "";

        if (orgSbe.indexOf("-") > -1) {
            int index = orgSbe.lastIndexOf("_");
            sbe = orgSbe.substring(0, index);

            return sbe;
        }

        return orgSbe;
    }

    /**
     * Return search base entity code with jti
     *
     * @param sbeCode Search Base entity
     * @return Search base entity with jti
     */
    public String getSearchBaseEntityCodeByJTI(String sbeCode) {
        String cleanSbe = getCleanSBECode(sbeCode);
        String newSbeCode = cleanSbe + "_" + userToken.getJTI().toUpperCase();
        return newSbeCode;
    }

    /**
     * Return ask with filter group content
     *
     * @param sbeCode      Search Base Entity Code
     * @param questionCode Question code
     * @param listParam    List o filter parameters
     * @return Ask
     */
    public Ask getFilterGroupBySearchBE(String sbeCode, String questionCode,String filterCode,
                                        Map<String, Map<String, String>> listParam) {
        Ask ask = new Ask();
        ask.setName(FilterConst.FILTERS);

        Question question = new Question();
        question.setAttributeCode(FilterConst.QUE_QQQ_GROUP);
        ask.setQuestion(question);

        Ask addFilterAsk = getAddFilterGroupBySearchBE(sbeCode, questionCode);
        ask.add(addFilterAsk);

        Ask existFilterAsk = getExistingFilterGroupBySearchBE(sbeCode,filterCode,listParam);

        ask.add(existFilterAsk);

        return ask;
    }

    /**
     * Change existing filter group
     *
     * @param sbeCode       Search base entity code
     * @param ask           Ask existing group
     * @param filterCode    Filter code
     * @param listFilParams List of filter parameters
     */
    public void setExistingFilterGroup(String sbeCode, Ask ask, String filterCode,
                                       Map<String, Map<String, String>> listFilParams) {
        //current filter state
        Ask curAsk = new Ask();
        curAsk.setQuestionCode(FilterConst.QUE_SAVED_SEARCH_CODE);
        curAsk.setAttributeCode(FilterConst.QUE_SAVED_SEARCH_CODE);
        curAsk.setName(filterCode);
        Question curQue = new Question();
        curQue.setCode(FilterConst.QUE_SAVED_SEARCH_CODE);
        curAsk.setQuestion(curQue);

        ask.add(curAsk);

        for (Map.Entry<String, Map<String, String>> param : listFilParams.entrySet()) {
            Ask childAsk = new Ask();
            childAsk.setAttributeCode(param.getKey());

            Question question = new Question();
            question.setAttributeCode(param.getValue().get(FilterConst.COLUMN));
            question.setCode(param.getValue().get(FilterConst.QUESTIONCODE));
            question.setName(param.getValue().get(FilterConst.OPTION));
            question.setHtml(param.getValue().get(FilterConst.VALUE));

            childAsk.setName(param.getValue().get(FilterConst.COLUMN));
            childAsk.setHidden(false);
            childAsk.setDisabled(false);
            childAsk.setQuestionCode(param.getValue().get(FilterConst.QUESTIONCODE));
            childAsk.setQuestion(question);
            childAsk.setTargetCode(getSearchBaseEntityCodeByJTI(sbeCode));

            ask.add(childAsk);
        }
    }

    /**
     * Return Html value by filter parameters
     * @param filterParams Filter parameters
     * @return Html value by filter parameters
     */
    public String getHtmlByFilterParam(Map<String, String> filterParams) {
        String attrCode = getFilterParamValByKey(filterParams, FilterConst.COLUMN)
                .replaceFirst(FilterConst.PRI_PREFIX, "");

        String attrName = getFilterParamValByKey(filterParams, FilterConst.OPTION);
        String value = getFilterParamValByKey(filterParams, FilterConst.VALUE);
        String attrNameStrip = attrName.replaceFirst(FilterConst.SEL_PREF, "")
                .replace("_", " ");

        String finalAttCode = StringUtils.capitalize(getLastWord(attrCode).toLowerCase());
        String finalVal = StringUtils.capitalize(getLastWord(value.toLowerCase()));
        String html = finalAttCode + " " + StringUtils.capitalize(attrNameStrip.toLowerCase()) + " " + finalVal;

        return html;
    }

    /**
     * Return the last word
     *
     * @param str String
     * @return The last word
     */
    public String getLastWord(String str) {
        String word = "";
        int lastIndex = str.lastIndexOf("_");
        if (lastIndex > -1) {
            word = str.substring(lastIndex + 1, str.length());
            return word;
        }
        return str;
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
     * Return ask with add filter group content
     *
     * @param sbeCode      Search Base Entity Code
     * @param questionCode Question code
     * @return Ask
     */
    public Ask getAddFilterGroupBySearchBE(String sbeCode, String questionCode) {
        String sourceCode = userToken.getUserCode();
        BaseEntity source = beUtils.getBaseEntityByCode(sourceCode);
        BaseEntity target = beUtils.getBaseEntityByCode(sbeCode);

        String sbeCodeJti = getSearchBaseEntityCodeByJTI(sbeCode);
        Ask ask = qwandaUtils.generateAskFromQuestionCode(FilterConst.QUE_ADD_FILTER_GRP, source, target);
        ask.getChildAsks().stream().forEach(e -> {
            if (e.getQuestionCode().equalsIgnoreCase(FilterConst.QUE_FILTER_COLUMN)
                    || e.getQuestionCode().equalsIgnoreCase(FilterConst.QUE_FILTER_OPTION)
                    || e.getQuestionCode().equalsIgnoreCase(FilterConst.QUE_SUBMIT)) {
                e.setHidden(false);
            } else if (e.getQuestionCode().equalsIgnoreCase(questionCode)) {
                e.setHidden(false);
            } else {
                e.setHidden(true);
            }

            e.setTargetCode(sbeCodeJti);
        });

        String targetCode = getSearchBaseEntityCodeByJTI(sbeCode);
        ask.setTargetCode(targetCode);

        Ask askSubmit = qwandaUtils.generateAskFromQuestionCode(FilterConst.QUE_SUBMIT, source, target);
//		askSubmit.setDisabled(true);

        ask.setTargetCode(targetCode);

        ask.add(askSubmit);

        return ask;
    }

    /**
     * Construct existing filter group object in Add Filter group form
     *
     * @param sbeCode       Search Base Entity Code
     * @param filterCode    Filter code
     * @param listFilParams List of Filter parameters
     * @return return existing filter group object
     */
    public Ask getExistingFilterGroupBySearchBE(String sbeCode,String filterCode,
                                                Map<String, Map<String, String>> listFilParams) {
        Ask ask = new Ask();
        ask.setName(FilterConst.FILTER_QUE_EXIST_NAME);
        String targetCode = getSearchBaseEntityCodeByJTI(sbeCode);
        ask.setSourceCode(userToken.getUserCode());
        ask.setTargetCode(targetCode);
        ask.setHidden(true);

        Question question = new Question();
        question.setCode(FilterConst.FILTER_QUE_EXIST);
        question.setAttributeCode(FilterConst.QUE_QQQ_GROUP);

        // change exist filter group
        if (listFilParams.size() > 0) {
            setExistingFilterGroup(sbeCode, ask,filterCode, listFilParams);
        }

        ask.setQuestion(question);

        return ask;
    }

    /**
     * Return Message of filter column
     *
     * @param searchBE Search Base Entity
     * @return Message of Filter column
     */
    public QDataBaseEntityMessage getFilterColumBySearchBE(SearchEntity searchBE) {
        QDataBaseEntityMessage msg = new QDataBaseEntityMessage();

        msg.setParentCode(FilterConst.QUE_ADD_FILTER_GRP);
        msg.setLinkCode(FilterConst.LNK_CORE);
        msg.setLinkValue(FilterConst.LNK_ITEMS);
        msg.setQuestionCode(FilterConst.QUE_FILTER_COLUMN);

        List<BaseEntity> baseEntities = new ArrayList<>();

        searchBE.getBaseEntityAttributes().stream()
                .filter(e -> e.getAttributeCode().startsWith(FilterConst.FILTER_COL))
                .forEach(e -> {
                    BaseEntity baseEntity = new BaseEntity();
                    List<EntityAttribute> entityAttributes = new ArrayList<>();

                    EntityAttribute ea = new EntityAttribute();
                    String attrCode = e.getAttributeCode().replaceFirst(FilterConst.FILTER_COL, "");
                    ea.setAttributeName(e.getAttributeName());
                    ea.setAttributeCode(attrCode);

                    String baseCode = FilterConst.FILTER_SEL + FilterConst.FILTER_COL + attrCode;
                    ea.setBaseEntityCode(baseCode);
                    ea.setValueString(e.getAttributeName());

                    entityAttributes.add(ea);

                    baseEntity.setCode(baseCode);
                    baseEntity.setName(e.getAttributeName());

                    baseEntity.setBaseEntityAttributes(entityAttributes);
                    baseEntities.add(baseEntity);
                });

        List<BaseEntity> basesSorted =  baseEntities.stream()
                .sorted(Comparator.comparing(BaseEntity::getName))
                .collect(Collectors.toList());

        msg.setItems(basesSorted);

        return msg;
    }

    /**
     * Return ask with filter option
     *
     * @param questionCode Question code
     * @return Ask
     */
    public QDataBaseEntityMessage getFilterOptionByEventCode(String questionCode) {
        QDataBaseEntityMessage base = new QDataBaseEntityMessage();

        base.setParentCode(FilterConst.QUE_ADD_FILTER_GRP);
        base.setLinkCode(FilterConst.LNK_CORE);
        base.setLinkValue(FilterConst.LNK_ITEMS);
        base.setQuestionCode(FilterConst.QUE_FILTER_OPTION);

        if (questionCode.equalsIgnoreCase(FilterConst.QUE_FILTER_VALUE_DJP_HC)) {
            base.add(beUtils.getBaseEntityByCode(FilterConst.SEL_EQUAL_TO));
            base.add(beUtils.getBaseEntityByCode(FilterConst.SEL_NOT_EQUAL_TO));
            return base;
        } else if (questionCode.equalsIgnoreCase(FilterConst.QUE_FILTER_VALUE_DATE)
                || questionCode.equalsIgnoreCase(FilterConst.QUE_FILTER_VALUE_DATETIME)
                || questionCode.equalsIgnoreCase(FilterConst.QUE_FILTER_VALUE_TIME)) {
            base.add(beUtils.getBaseEntityByCode(FilterConst.SEL_GREATER_THAN));
            base.add(beUtils.getBaseEntityByCode(FilterConst.SEL_GREATER_THAN_OR_EQUAL_TO));
            base.add(beUtils.getBaseEntityByCode(FilterConst.SEL_LESS_THAN));
            base.add(beUtils.getBaseEntityByCode(FilterConst.SEL_LESS_THAN_OR_EQUAL_TO));
            base.add(beUtils.getBaseEntityByCode(FilterConst.SEL_EQUAL_TO));
            base.add(beUtils.getBaseEntityByCode(FilterConst.SEL_NOT_EQUAL_TO));
            return base;
        } else if (questionCode.equalsIgnoreCase(FilterConst.QUE_FILTER_VALUE_COUNTRY)
                || questionCode.equalsIgnoreCase(FilterConst.QUE_FILTER_VALUE_INTERNSHIP_TYPE)
                || questionCode.equalsIgnoreCase(FilterConst.QUE_FILTER_VALUE_STATE)
                || questionCode.equalsIgnoreCase(FilterConst.QUE_FILTER_VALUE_ACADEMY)
                || questionCode.equalsIgnoreCase(FilterConst.QUE_FILTER_VALUE_DJP_HC)) {
            base.add(beUtils.getBaseEntityByCode(FilterConst.SEL_EQUAL_TO));
            base.add(beUtils.getBaseEntityByCode(FilterConst.SEL_NOT_EQUAL_TO));
            return base;
        } else {
            base.add(beUtils.getBaseEntityByCode(FilterConst.SEL_EQUAL_TO));
            base.add(beUtils.getBaseEntityByCode(FilterConst.SEL_NOT_EQUAL_TO));
            base.add(beUtils.getBaseEntityByCode(FilterConst.SEL_LIKE));
            base.add(beUtils.getBaseEntityByCode(FilterConst.SEL_NOT_LIKE));
        }

        return base;
    }

    /**
     * Return ask with filter select option values
     *
     * @param queGrp  Question Group
     * @param queCode Question code
     * @param lnkCode Link code
     * @param lnkVal  Link Value
     * @return Data message of filter select box
     */
    public QDataBaseEntityMessage getFilterSelectBoxValueByCode(String queGrp, String queCode, String lnkCode,
                                                                String lnkVal) {
        QDataBaseEntityMessage base = new QDataBaseEntityMessage();

        base.setParentCode(queGrp);
        base.setLinkCode(FilterConst.LNK_CORE);
        base.setLinkValue(FilterConst.LNK_ITEMS);
        base.setQuestionCode(queCode);

        SearchEntity searchBE = new SearchEntity(FilterConst.SBE_DROPDOWN, FilterConst.SBE_DROPDOWN)
                .add(new Column(PRI_CODE, FilterConst.PRI_CODE_LABEL));
        searchBE.setRealm(userToken.getProductCode());
        searchBE.setLinkCode(lnkCode);
        searchBE.setLinkValue(lnkVal);
        searchBE.setPageStart(0).setPageSize(1000);

        List<BaseEntity> baseEntities = searchUtils.searchBaseEntitys(searchBE);
        List<BaseEntity> basesSorted =  baseEntities.stream()
                .sorted(Comparator.comparing(BaseEntity::getName))
                .collect(Collectors.toList());

        base.setItems(basesSorted);

        return base;
    }

    /**
     * Return ask with bucket filter options
     * @param sbeCode Search entity code
     * @param lnkCode Link Code
     * @param lnkValue Link Value
     * @return Bucket filter options
     */
    public SearchEntity getQuickOptions(String sbeCode,String lnkCode, String lnkValue) {
        SearchEntity searchBE = new SearchEntity(sbeCode,sbeCode);
        searchBE.add(new Or(new Filter(FilterConst.PRI_CODE, Operator.LIKE, "CPY_%")
                        ,new Filter(FilterConst.PRI_CODE, Operator.LIKE, "PER_%")))
                .add(new Column(lnkCode, lnkCode));

        if(!lnkValue.isEmpty()) {
            searchBE.add(new Filter(FilterConst.PRI_NAME, Operator.LIKE, "%" + lnkValue + "%"));
        }

        searchBE.setRealm(userToken.getProductCode());
        searchBE.setPageStart(0).setPageSize(20);

        return searchBE;
    }

    /**
     * Get search base entity
     * @param sbeCode Search base entity
     * @param lnkCode link code
     * @param lnkValue Link value
     * @param isSortedDate being sorted by date
     * @return Search entity
     */
    public SearchEntity getListSavedSearch(String sbeCode,String lnkCode, String lnkValue, boolean isSortedDate) {
        SearchEntity searchBE = new SearchEntity(sbeCode,sbeCode);
        searchBE.add(new Filter(FilterConst.PRI_CODE, Operator.LIKE, FilterConst.SBE_SAVED_SEARCH + "%"))
                .add(new Column(lnkCode, lnkValue));

        String startWith = "[\"" + FilterConst.SBE_SAVED_SEARCH;
        searchBE.add(new Filter(FilterConst.LNK_SAVED_SEARCHES,Operator.STARTS_WITH,startWith));
        searchBE.add(new Filter(FilterConst.LNK_AUTHOR,Operator.CONTAINS,userToken.getUserCode()));

        if(isSortedDate) {
            searchBE.add(new Sort("PRI_CREATED_DATE", Ord.DESC));
        } else {
            searchBE.add(new Sort("PRI_CREATED_DATE", Ord.ASC));
        }

        searchBE.setRealm(userToken.getProductCode());
        searchBE.setPageStart(0).setPageSize(20);

        return searchBE;
    }
}
