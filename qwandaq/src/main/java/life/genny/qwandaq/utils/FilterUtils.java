package life.genny.qwandaq.utils;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
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
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Column;
import life.genny.qwandaq.entity.search.trait.Sort;
import life.genny.qwandaq.entity.search.trait.Ord;
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
    EntityAttributeUtils beaUtils;

    @Inject
    AttributeUtils attributeUtils;

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
     * Return ask with filter group content
     *
     * @param questionCode Question code
     * @return Ask
     */
    public Ask getFilterGroup(String questionCode) {
        Ask ask = new Ask();
        ask.setName(FilterConst.FILTERS);

        Attribute attribute = new Attribute(Attribute.QQQ_QUESTION_GROUP,Attribute.QQQ_QUESTION_GROUP,new DataType(String.class));
        Question question = new Question(questionCode,questionCode,attribute);
        question.setAttributeCode(Attribute.QQQ_QUESTION_GROUP);
		question.setAttribute(attribute);
        ask.setQuestion(question);

        Ask addFilterAsk = getAddFilterGroupBySearchBE(questionCode);
        ask.add(addFilterAsk);

        return ask;
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

        beaUtils.getBaseEntityAttributesForBaseEntityWithAttributeCodePrefix(searchBE.getRealm(), searchBE.getCode(),
                        Prefix.FLC_).forEach(e -> {
                    BaseEntity baseEntity = new BaseEntity();
                    List<EntityAttribute> entityAttributes = new ArrayList<>();

                    EntityAttribute ea = new EntityAttribute();
                    String attrCode = e.getAttributeCode().replaceFirst(Prefix.FLC_, "");
                    ea.setAttributeName(e.getAttributeName());
                    ea.setAttributeCode(attrCode);
					Attribute attr = attributeUtils.getAttribute(attrCode, true, true);
					ea.setAttribute(attr);

                    String baseCode = FilterConst.FILTER_SEL + Prefix.FLC_ + attrCode;
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
     * @param dataType Data Type
     * @return Ask
     */
    public QDataBaseEntityMessage getFilterOptionByCode(String dataType) {
        QDataBaseEntityMessage base = new QDataBaseEntityMessage();

        base.setParentCode(Question.QUE_ADD_FILTER_SBE_GRP);
        base.setLinkCode(Attribute.LNK_CORE);
        base.setLinkValue(Attribute.LNK_ITEMS);
        base.setQuestionCode(Question.QUE_FILTER_OPTION);

        if (dataType.equalsIgnoreCase(FilterConst.DTT_DATETIME) || dataType.equalsIgnoreCase(FilterConst.DTT_DATE)){
            base.add(beUtils.getBaseEntity(FilterConst.SEL_GREATER_THAN));
            base.add(beUtils.getBaseEntity(FilterConst.SEL_GREATER_THAN_OR_EQUAL_TO));
            base.add(beUtils.getBaseEntity(FilterConst.SEL_LESS_THAN));
            base.add(beUtils.getBaseEntity(FilterConst.SEL_LESS_THAN_OR_EQUAL_TO));
            base.add(beUtils.getBaseEntity(FilterConst.SEL_EQUAL_TO));
            base.add(beUtils.getBaseEntity(FilterConst.SEL_NOT_EQUAL_TO));
            return base;
        } else if (dataType.contains(FilterConst.SELECT)) {
            base.add(beUtils.getBaseEntity(FilterConst.SEL_EQUAL_TO));
            base.add(beUtils.getBaseEntity(FilterConst.SEL_NOT_EQUAL_TO));
            return base;
        } else if (dataType.contains(FilterConst.YES_NO) || dataType.contains(FilterConst.BOOLEAN)) {
            base.add(beUtils.getBaseEntity(FilterConst.SEL_EQUAL_TO));
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
                .sorted(Comparator.comparing(BaseEntity::getIndex))
                .collect(Collectors.toList());

        base.setItems(basesSorted);

        return base;
    }

    /**
     * Get search base entity
     * @param sbeCode Search base entity
     * @param lnkCode link code
     * @param lnkValue Link value
     * @return Search entity
     */
    public SearchEntity getListSavedSearch(String sbeCode,String lnkCode, String lnkValue) {
        String startWith = "[\"" + SearchEntity.SBE_SAVED_SEARCH;
        SearchEntity searchBE = new SearchEntity(sbeCode,sbeCode);
        searchBE.add(new Filter(Attribute.PRI_CODE, Operator.LIKE, SearchEntity.SBE_SAVED_SEARCH + "_%"))
                .add(new Filter(Attribute.LNK_SAVED_SEARCHES,Operator.STARTS_WITH,startWith))
                .add(new Filter(Attribute.LNK_AUTHOR,Operator.CONTAINS,userToken.getUserCode()))
                .add(new Sort(Attribute.PRI_CREATED, Ord.DESC))
                .add(new Column(lnkCode, lnkValue));

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
     * @param defs List of definition
     * @return Search entity
     */
    public SearchEntity getListQuickSearches(String sbeCode,String lnkCode,String lnkValue,String typing,List<String> defs) {
        SearchEntity searchBE = new SearchEntity(sbeCode,sbeCode);
        for(int i=0;i< defs.size(); i++){
            if(i== 0) searchBE.add(new Filter(Attribute.LNK_DEF, Operator.CONTAINS, defs.get(i)));
            else searchBE.add(new Or(new Filter(Attribute.LNK_DEF, Operator.CONTAINS, defs.get(i))));
        }
        searchBE.add(new Filter(Attribute.PRI_NAME, Operator.LIKE, typing+ "_%"))
                .add(new Column(lnkCode, lnkValue));
        searchBE.add(new Sort(Attribute.PRI_NAME, Ord.ASC));

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
                attCode.startsWith(Prefix.FLC_)){
            return true;
        }

        return false;
    }
}
