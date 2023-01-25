package life.genny.qwandaq.utils;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.constants.FilterConst;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.datatype.capability.core.CapabilitySet;
import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;
import life.genny.qwandaq.entity.search.trait.*;
import org.jboss.logging.Logger;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.entity.search.clause.Or;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;

@ApplicationScoped
public class FilterUtils {
    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());
    static Jsonb jsonb = JsonbBuilder.create();

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
     * @param questionCode Question code
     * @param listParam    List o filter parameters
     * @return Ask
     */
    public Ask getFilterGroup(String questionCode,String filterCode,
                              Map<String, Map<String, String>> listParam) {
        Ask ask = new Ask();
        ask.setName(FilterConst.FILTERS);

        Attribute attribute = new Attribute(Attribute.QQQ_QUESTION_GROUP,Attribute.QQQ_QUESTION_GROUP,new DataType());
        Question question = new Question(questionCode,questionCode,attribute);
        question.setAttributeCode(Attribute.QQQ_QUESTION_GROUP);
        ask.setQuestion(question);

        Ask addFilterAsk = getAddFilterGroupBySearchBE(questionCode);
        ask.add(addFilterAsk);

        return ask;
    }

    /**
     * Return the link value code
     * @param value Value
     * @return Return the link value code
     */
    public String getLinkValueCode(String value) {
        String fieldName = "";
        int priIndex = -1;
        int fieldIndex = value.lastIndexOf(Prefix.FIELD);
        if(fieldIndex > -1) {
            priIndex = value.indexOf(Prefix.PRI) + Prefix.PRI.length();
            fieldName = value.substring(priIndex,fieldIndex - 1);
            return fieldName;
        } else {
            priIndex = value.lastIndexOf(Prefix.PRI) + Prefix.PRI.length();
        }
        if(priIndex > -1) {
            fieldName = value.substring(priIndex);
            fieldName = fieldName.replaceFirst("\"]","");
        }
        return fieldName;
    }

    /**
     * Return ask with add filter group content
     *
     * @param questionCode Question code
     * @return Ask
     */
    public Ask getAddFilterGroupBySearchBE(String questionCode) {
        String sourceCode = userToken.getUserCode();
        BaseEntity source = beUtils.getBaseEntity(sourceCode);
        BaseEntity target = beUtils.getBaseEntity(sourceCode);

        Ask ask = qwandaUtils.generateAskFromQuestionCode(Question.QUE_ADD_FILTER_SBE_GRP, source, target, new CapabilitySet(target), new ReqConfig());
        ask.getChildAsks().stream().forEach(e -> {
            if (e.getQuestionCode().equalsIgnoreCase(Question.QUE_FILTER_COLUMN)
                    || e.getQuestionCode().equalsIgnoreCase(Question.QUE_FILTER_OPTION)
                    || e.getQuestionCode().equalsIgnoreCase(Question.QUE_SUBMIT)) {
                e.setHidden(false);
            } else if(e.getQuestionCode().equalsIgnoreCase(questionCode)) {
                e.setHidden(false);
            } else {
                e.setHidden(true);
            }

            e.setTargetCode(sourceCode);
        });

        Ask askSubmit = qwandaUtils.generateAskFromQuestionCode(Question.QUE_SUBMIT, source, target, new CapabilitySet(target), new ReqConfig());

        ask.setTargetCode(sourceCode);
        ask.add(askSubmit);

        return ask;
    }

    /**
     * Return Message of filter column
     *
     * @param searchBE Search Base Entity
     * @return Message of Filter column
     */
    public QDataBaseEntityMessage getFilterValuesByColum(SearchEntity searchBE) {
        QDataBaseEntityMessage msg = new QDataBaseEntityMessage();

        msg.setParentCode(Question.QUE_ADD_FILTER_SBE_GRP);
        msg.setLinkCode(Attribute.LNK_CORE);
        msg.setLinkValue(Attribute.LNK_ITEMS);
        msg.setQuestionCode(Question.QUE_FILTER_COLUMN);

        List<BaseEntity> baseEntities = new ArrayList<>();

        for (Map.Entry<String, EntityAttribute> entry : searchBE.getBaseEntityAttributesMap().entrySet()) {
            String key = entry.getKey();
            EntityAttribute value = entry.getValue();
            if (!key.startsWith(Prefix.FLC)) {
                continue;
            }
            BaseEntity baseEntity = new BaseEntity();
            List<EntityAttribute> entityAttributes = new ArrayList<>();

            EntityAttribute ea = new EntityAttribute();
            String attrCode = value.getAttributeCode().replaceFirst(Prefix.FLC, "");
            ea.setAttributeName(value.getAttributeName());
            ea.setAttributeCode(attrCode);

            String baseCode = FilterConst.FILTER_SEL + Prefix.FLC + attrCode;
            ea.setBaseEntityCode(baseCode);
            ea.setValueString(value.getAttributeName());

            entityAttributes.add(ea);

            baseEntity.setCode(baseCode);
            baseEntity.setName(value.getAttributeName());

            baseEntity.setBaseEntityAttributes(entityAttributes);
            baseEntities.add(baseEntity);
        }

        List<BaseEntity> basesSorted =  baseEntities.stream()
                .sorted(Comparator.comparing(BaseEntity::getName))
                .collect(Collectors.toList());

        msg.setItems(basesSorted);

        return msg;
    }

    /**
     * Return ask with filter option
     *
     * @param value Value
     * @return Ask
     */
    public QDataBaseEntityMessage getFilterOptionByCode(String value) {
        QDataBaseEntityMessage base = new QDataBaseEntityMessage();

        base.setParentCode(Question.QUE_ADD_FILTER_SBE_GRP);
        base.setLinkCode(Attribute.LNK_CORE);
        base.setLinkValue(Attribute.LNK_ITEMS);
        base.setQuestionCode(Question.QUE_FILTER_OPTION);

        if (value.contains(FilterConst.DATETIME)){
            base.add(beUtils.getBaseEntity(FilterConst.SEL_GREATER_THAN));
            base.add(beUtils.getBaseEntity(FilterConst.SEL_GREATER_THAN_OR_EQUAL_TO));
            base.add(beUtils.getBaseEntity(FilterConst.SEL_LESS_THAN));
            base.add(beUtils.getBaseEntity(FilterConst.SEL_LESS_THAN_OR_EQUAL_TO));
            base.add(beUtils.getBaseEntity(FilterConst.SEL_EQUAL_TO));
            base.add(beUtils.getBaseEntity(FilterConst.SEL_NOT_EQUAL_TO));
            return base;
        } else if (value.contains(FilterConst.SELECT)) {
            base.add(beUtils.getBaseEntity(FilterConst.SEL_EQUAL_TO));
            base.add(beUtils.getBaseEntity(FilterConst.SEL_NOT_EQUAL_TO));
            return base;
        } else {
            base.add(beUtils.getBaseEntity(FilterConst.SEL_EQUAL_TO));
            base.add(beUtils.getBaseEntity(FilterConst.SEL_NOT_EQUAL_TO));
            base.add(beUtils.getBaseEntity(FilterConst.SEL_LIKE));
            base.add(beUtils.getBaseEntity(FilterConst.SEL_NOT_LIKE));
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
    public QDataBaseEntityMessage getSelectBoxValueByCode(String queGrp,String queCode,String lnkCode,String lnkVal) {
        QDataBaseEntityMessage base = new QDataBaseEntityMessage();

        base.setParentCode(queGrp);
        base.setLinkCode(Attribute.LNK_CORE);
        base.setLinkValue(Attribute.LNK_ITEMS);
        base.setQuestionCode(queCode);

        SearchEntity searchBE = new SearchEntity(SearchEntity.SBE_DROPDOWN, SearchEntity.SBE_DROPDOWN)
                .add(new Column(Attribute.PRI_CODE, Attribute.PRI_CODE));
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
        searchBE.add(new Or(new Filter(Attribute.PRI_CODE, Operator.STARTS_WITH, Prefix.CPY)
                        ,new Filter(Attribute.PRI_CODE, Operator.STARTS_WITH, Prefix.PER)))
                .add(new Column(lnkCode, lnkCode));

        if(!lnkValue.isEmpty()) {
            searchBE.add(new Filter(Attribute.PRI_NAME, Operator.LIKE, "%" + lnkValue + "%"));
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
        searchBE.add(new Filter(Attribute.PRI_CODE, Operator.LIKE, SearchEntity.SBE_SAVED_SEARCH + "_%"))
                .add(new Column(lnkCode, lnkValue));

        String startWith = "[\"" + SearchEntity.SBE_SAVED_SEARCH;
        searchBE.add(new Filter(Attribute.LNK_SAVED_SEARCHES,Operator.STARTS_WITH,startWith));
        searchBE.add(new Filter(Attribute.LNK_AUTHOR,Operator.CONTAINS,userToken.getUserCode()));

        if(isSortedDate) {
            searchBE.add(new Sort(Attribute.PRI_CREATED_DATE, Ord.DESC));
        } else {
            searchBE.add(new Sort(Attribute.PRI_CREATED_DATE, Ord.ASC));
        }

        searchBE.setRealm(userToken.getProductCode());
        searchBE.setPageStart(0).setPageSize(100);

        return searchBE;
    }


    /**
     * Get search base entity
     * @param sbeCode Search base entity
     * @param lnkCode link code
     * @param lnkValue Link value
     * @param typing Typing value
     * @return Search entity
     */
    public SearchEntity getListQuickSearches(String sbeCode,String lnkCode,String lnkValue,String typing) {
        SearchEntity searchBE = new SearchEntity(sbeCode,sbeCode);
        searchBE.add(new Filter(Attribute.PRI_NAME, Operator.LIKE, typing+ "_%"))
                .add(new Column(lnkCode, lnkValue));

        searchBE.setRealm(userToken.getProductCode());
        searchBE.setPageStart(0).setPageSize(20);

        return searchBE;
    }

    /**
     * Return Being valid of filter answer or not
     * @param attCode Attribute code
     * @return Being valid of filter answer or not
     */
    public boolean validFilter(String attCode) {
        if(attCode.equalsIgnoreCase(Attribute.LNK_FILTER_COLUMN) ||
                attCode.equalsIgnoreCase(Attribute.LNK_SAVED_SEARCH) ||
                attCode.equalsIgnoreCase(Attribute.LNK_QUICK_SEARCH) ||
                attCode.startsWith(Prefix.FLC)){
            return true;
        }

        return false;
    }
}
