package life.genny.kogito.common.service;

import static life.genny.qwandaq.attribute.Attribute.PRI_NAME;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import life.genny.qwandaq.entity.search.clause.ClauseContainer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jboss.logging.Logger;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.message.QCmdMessage;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.message.QSearchMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.DefUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.utils.SearchUtils;

@ApplicationScoped
public class SearchService {

	private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	SearchUtils searchUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	DefUtils defUtils;

	@Inject
	QwandaUtils qwandaUtils;

	public static enum SearchOptions {
		PAGINATION,
		SEARCH,
		FILTER,
		PAGINATION_BUCKET
	}

	/**
	 * Perform a Detail View search.
	 *
	 * @param code The code of the target to display
	 */
	public void sendTable(String code) {

		// trim TREE_ITEM_ from code if present
		code = StringUtils.removeStart(code, "TREE_ITEM_");
		String searchCode = "SBE_"+code;
		log.info("Sending Table :: " + searchCode);

		searchUtils.searchTable(searchCode);
		sendSearchPCM("PCM_TABLE", searchCode);
	}

	/**
	 * Perform a Detail View search.
	 *
	 * @param targetCode The code of the target to display
	 */
	public void sendDetailView(String targetCode) {

		// fetch target and find it's definition
		BaseEntity target = beUtils.getBaseEntityByCode(targetCode);
		BaseEntity definition = defUtils.getDEF(target);

		// grab the corresponding detail view SBE
		String searchCode = "SBE_" + StringUtils.removeStart(definition.getCode(), "DEF_");
		log.info("Sending Detail View :: " + searchCode);

		SearchEntity searchEntity = CacheUtils.getObject(userToken.getProductCode(), searchCode, SearchEntity.class);
		searchEntity.add(new Filter("PRI_CODE", Operator.EQUALS, targetCode));

		// perform the search
		searchUtils.searchTable(searchEntity);
		sendSearchPCM("PCM_DETAIL_VIEW", searchEntity.getCode());
	}

	/**
	 * Perform a Bucket search.
	 */
	public void sendBuckets() {
	}

	/**
	 * Perform a named search from search bar
	 * 
	 * @param searchCode
	 * @param nameWildcard
	 */
	public void sendNameSearch(String searchCode, String nameWildcard) {

		log.info("Sending Name Search :: " + searchCode);

		SearchEntity searchEntity = CacheUtils.getObject(userToken.getProductCode(), 
				searchCode, SearchEntity.class);

		// TODO: remove this from alyson
		nameWildcard = StringUtils.removeStart(nameWildcard, "!");

		searchEntity.add(new Filter(PRI_NAME, Operator.LIKE, "%"+nameWildcard+"%"));

		searchUtils.searchTable(searchEntity);
		// sendSearchPCM("PCM_TABLE", searchCode);
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
	 * Get bucket data with bucket event
	 * @param code Bucket event code
	 */
	public void getBuckets(String code) {
		try {
			String searchCode = "SBE_" + code;

			List<String> originBucketCodes = CacheUtils.getObject(userToken.getRealm(), searchCode, List.class);
			List<String>  bucketCodes = getBucketCodesBySearchEntity(originBucketCodes);
			sendBucketCodes(bucketCodes);

			originBucketCodes.stream().forEach(e -> {
				searchUtils.searchTable(e);
				sendSearchPCM("PCM_PROCESS", e);
			});
		}catch (Exception ex){
			log.error(ex);
		}
	}

	/**
	 * Send the list of bucket codes to frond-end
	 * @param bucketCodes The list of bucket codes
	 */
	public void sendBucketCodes(List<String> bucketCodes) {
		QCmdMessage msgProcess = new QCmdMessage(GennyConstants.BUCKET_DISPLAY,GennyConstants.BUCKET_PROCESS);
		msgProcess.setToken(userToken.getToken());
		KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msgProcess);

		QCmdMessage msgCodes = new QCmdMessage(GennyConstants.BUCKET_CODES,GennyConstants.BUCKET_CODES);
		msgCodes.setToken(userToken.getToken());
		msgCodes.setSourceCode(GennyConstants.BUCKET_CODES);
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
	 * handle sorting, searching in the table
	 * @param code Attribute code
	 * @param attrName Attribute name
	 * @param value  Value String
	 * @param targetCode Target code
	 */
	public void handleSortAndSearch(String code, String attrName,String value, String targetCode, SearchOptions ops) {
		SearchEntity searchBE = CacheUtils.getObject(userToken.getRealm(), targetCode, SearchEntity.class);

		if(ops.equals(SearchOptions.SEARCH)) {
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

		}else if(ops.equals(SearchOptions.PAGINATION) || ops.equals(SearchOptions.PAGINATION_BUCKET)) { //pagination
			Optional<EntityAttribute> aeIndex = searchBE.findEntityAttribute(GennyConstants.PAGINATION_INDEX);
			Integer pageSize = searchBE.getPageSize();
			Integer indexVal = 0;
			Integer pagePos = 0;

			if(aeIndex.isPresent() && pageSize !=null) {
				if(code.equalsIgnoreCase(GennyConstants.PAGINATION_NEXT) ||
						code.equalsIgnoreCase(GennyConstants.QUE_TABLE_LAZY_LOAD)) {
					indexVal = aeIndex.get().getValueInteger() + 1;
				} else if (code.equalsIgnoreCase(GennyConstants.PAGINATION_PREV)) {
					indexVal = aeIndex.get().getValueInteger() - 1;
				}

				pagePos = (indexVal - 1) * pageSize;
			}
			//initial stage of bucket pagination
			else if (aeIndex.isEmpty() && code.equalsIgnoreCase(GennyConstants.QUE_TABLE_LAZY_LOAD)) {
				indexVal = 2;
				pagePos = pageSize;
			}

			searchBE.setPageStart(pagePos);
			searchBE.setPageIndex(indexVal);
		}
		CacheUtils.putObject(userToken.getRealm(), targetCode, searchBE);


		if(ops.equals(SearchOptions.PAGINATION_BUCKET)) {
			sendCmdMsgByCodeType(GennyConstants.BUCKET_DISPLAY, GennyConstants.NONE);
			sendMessageBySearchEntity(searchBE);
		} else {
			sendMessageBySearchEntity(searchBE);
			sendSearchPCM(GennyConstants.PCM_TABLE, targetCode);
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
			SearchEntity searchBE = CacheUtils.getObject(userToken.getRealm(), targetCode, SearchEntity.class);
			EntityAttribute ea = createEntityAttributeBySortAndSearch(code, name, value);

			//remove searching text and filter
			Filter searchText = new Filter(code, Operator.LIKE, value);
			Filter filter = new Filter(GennyConstants.PRI_ASSOC_HC, Operator.EQUALS, value);

			searchBE.remove(searchText);
			searchBE.remove(filter);

			//searching text
			if (!name.isBlank()) {
				searchBE.add(new Filter(code, Operator.LIKE, value));
			}

			//filter by select box
			if (code.equalsIgnoreCase(GennyConstants.LNK_PERSON)) {
				searchBE.add(filter);
			}

			CacheUtils.putObject(userToken.getRealm(), targetCode, searchBE);

			sendMessageBySearchEntity(searchBE);
			sendSearchPCM(GennyConstants.PCM_PROCESS, targetCode);
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
			SearchEntity searchBE = CacheUtils.getObject(userToken.getRealm(), sbeCode, SearchEntity.class);

			if (searchBE != null) {
				Ask ask = searchUtils.getFilterGroupBySearchBE(sbeCode, questionCode, filterCode, listFilterParams);
				QDataAskMessage msgFilterGrp = new QDataAskMessage(ask);
				msgFilterGrp.setToken(userToken.getToken());
				String queCode = "";
				if(addedJti) {
					queCode = searchUtils.getSearchBaseEntityCodeByJTI(sbeCode);
					ask.setTargetCode(queCode);

					queCode = queGrp + "_" + queCode;
				}else {
					ask.setTargetCode(sbeCode);
					queCode = queGrp + "_" + sbeCode;
				}
				msgFilterGrp.setTargetCode(queCode);
				ask.setQuestionCode(queCode);
				ask.getQuestion().setCode(queCode);

				msgFilterGrp.setMessage(GennyConstants.FILTERS);
				msgFilterGrp.setTag(GennyConstants.FILTERS);
				msgFilterGrp.setReplace(true);
				KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msgFilterGrp);

				QDataBaseEntityMessage msgColumn = searchUtils.getFilterColumBySearchBE(searchBE);

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
		QDataBaseEntityMessage msg = searchUtils.getFilterOptionByEventCode(questionCode);
		String sbeCodeJti =  searchUtils.getSearchBaseEntityCodeByJTI(sbeCode);

		msg.setToken(userToken.getToken());
		msg.setParentCode(GennyConstants.QUE_ADD_FILTER_GRP);
		msg.setLinkCode(GennyConstants.LNK_CORE);
		msg.setLinkValue(GennyConstants.LNK_ITEMS);
		msg.setQuestionCode(GennyConstants.QUE_FILTER_OPTION);
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
	 */
	public void sendFilterValue(String queGrp,String queCode, String lnkCode, String lnkVal) {
		QDataBaseEntityMessage msg = null;
		msg = searchUtils.getFilterSelectBoxValueByCode(queGrp,queCode, lnkCode,lnkVal);

		msg.setToken(userToken.getToken());
		msg.setTargetCode(queCode);
		msg.setMessage(GennyConstants.FILTERS);
		msg.setReplace(true);
		KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
	}


	/**
	 * handle filter by string in the table
	 * @param sbeCode Search base entity code without JTI
	 * @param listFilterParams List of filter parameters
	 */
	public void handleFilter(String sbeCode, Map<String,Map<String, String>> listFilterParams) {
		String sbeCodeJti = searchUtils.getSearchBaseEntityCodeByJTI(sbeCode);

		SearchEntity searchBE = CacheUtils.getObject(userToken.getRealm(), sbeCodeJti, SearchEntity.class);

		excludeExtraFilterBySearchBE(searchBE);

		//add conditions by filter parameters
		setFilterParamsToSearchBE(searchBE,listFilterParams);

		CacheUtils.putObject(userToken.getRealm(), sbeCodeJti, searchBE);

		sendMessageBySearchEntity(searchBE);
		sendSearchPCM(GennyConstants.PCM_TABLE, sbeCodeJti);
	}

	/**
	 * Remove extra filter parameters to search base entity
	 * @param searchBE Search base entity
	 */
	public void excludeExtraFilterBySearchBE(SearchEntity searchBE) {
		List<ClauseContainer> clauses = new ArrayList<>(searchBE.getClauseContainers());
		for(ClauseContainer clause : clauses){
			if(clause.getFilter().getType() == Filter.FILTER_TYPE.EXTRA) {
				searchBE.remove(clause.getFilter());
			}
		}
	}

	/**
	 * Add filer parameters to search base entity
	 * @param searchBE Search base entity
	 */
	public void setFilterParamsToSearchBE(SearchEntity searchBE, Map<String,Map<String, String>> listFilterParams) {

		for(Map.Entry<String, Map<String,String>> e : listFilterParams.entrySet()) {
			String queCode = searchUtils.getFilterParamValByKey(e.getValue(), GennyConstants.QUESTIONCODE);
			String operatorCode = searchUtils.getFilterParamValByKey(e.getValue(), GennyConstants.OPTION);
			String value = searchUtils.getFilterParamValByKey(e.getValue(), GennyConstants.VALUE)
					.replaceFirst(GennyConstants.SEL_PREF, "");
			String field = searchUtils.getFilterParamValByKey(e.getValue(), GennyConstants.COLUMN)
											.replaceFirst(GennyConstants.SEL_FILTER_COLUMN_FLC, "");
			Operator operator = getOperatorByVal(operatorCode);

			if (operator.equals(Operator.LIKE)) {
				value = "%" + value + "%";
			}

			boolean isDate = isDateTimeSelected(queCode);
			Filter filter = null;

			if (isDate) {
				LocalDateTime dateTime = parseStringToDate(value);
				filter = new Filter(field, operator, dateTime, Filter.FILTER_TYPE.EXTRA);
			} else {
				filter = new Filter(field, operator, value, Filter.FILTER_TYPE.EXTRA);
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
		ask.setName(GennyConstants.BUCKET_FILTER_LABEL);
		Question question = new Question();
		question.setCode(queGroup);
		question.setAttributeCode(GennyConstants.QUE_QQQ_GROUP);
		ask.setQuestion(question);

		Ask childAsk = new Ask();
		childAsk.setName(GennyConstants.BUCKET_FILTER_LABEL);
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

		ask.addChildAsk(childAsk);

		QDataAskMessage msg = new QDataAskMessage(ask);
		msg.setToken(userToken.getToken());
		msg.setTargetCode(targetCode);
		msg.setQuestionCode(queGroup);
		msg.setMessage(GennyConstants.BUCKET_FILTER_LABEL);
		msg.setReplace(true);
		KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);

		sendQuickSearchItems(GennyConstants.SBE_DROPDOWN,queGroup,queCode,GennyConstants.PRI_NAME, "");
	}

	/**
	 * Send message to bucket page with filter data
	 * @param queGroup Question group
	 * @param queCode Question code
	 */
	public void sendQuickSearchItems(String sbeCode, String queGroup,String queCode,String lnkCode, String lnkValue) {
		SearchEntity searchEntity = searchUtils.getQuickOptions(sbeCode,lnkCode,lnkValue);
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
		List<String> originBucketCodes = CacheUtils.getObject(userToken.getRealm(), searchCode, List.class);
		List<String>  bucketCodes = getBucketCodesBySearchEntity(originBucketCodes);

		return bucketCodes;
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

	/**
	 * Send dropdown options data
	 * @param sbeCode Search base entity code
	 * @param group Question group code
	 * @param code Question code
	 * @param lnkCode Link code
	 * @param lnkValue Link value
	 */
	public void sendListSavedSearches(String sbeCode,String group,String code,String lnkCode,String lnkValue) {
		String sbeJti = getSearchBaseEntityCodeByJTI(GennyConstants.SBE_SAVED_SEARCH);
		SearchEntity searchEntity = searchUtils.getListSavedSearch(sbeJti,lnkCode,lnkValue, true);
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
		String sbeJti = getSearchBaseEntityCodeByJTI(GennyConstants.SBE_SAVED_SEARCH);
		SearchEntity search = searchUtils.getListSavedSearch(sbeJti,lnkCode,lnkValue, true);
		List<BaseEntity> bases = searchUtils.searchBaseEntitys(search);
		return bases;
	}

	/**
	 * Send ask by question code and Search base entity code
	 * @param code Question code
	 * @param sbeCode Search base entity code
	 */
	public void sendAsk(String code, String sbeCode, Pair<String, String>... childCodes) {
		Ask ask = new Ask();

		ask.setQuestionCode(code);
		ask.setName(code);
		ask.setTargetCode(sbeCode);

		Question question = new Question();
		question.setCode(code);
		question.setAttributeCode(GennyConstants.QUE_QQQ_GROUP);
		ask.setQuestion(question);

		for(Pair<String,String> pair  : childCodes) {
			Ask childAsk = new Ask();
			childAsk.setQuestionCode(pair.getKey());
			childAsk.setName(pair.getKey());
			childAsk.setTargetCode(sbeCode);

			Question childQuestion = new Question();
			childQuestion.setCode(pair.getKey());
			childQuestion.setAttributeCode(pair.getKey());
			childQuestion.setPlaceholder(pair.getValue());
			childAsk.setQuestion(childQuestion);
			ask.addChildAsk(childAsk);
		}

		QDataAskMessage msg = new QDataAskMessage(ask);
		msg.setToken(userToken.getToken());
		msg.setTargetCode(sbeCode);
		msg.setQuestionCode(code);
		msg.setMessage(code);
		msg.setReplace(true);

		KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
	}


	/**
	 * Return search base entity code with jti
	 *
	 * @param sbeCode Search Base entity
	 * @return Search base entity with jti
	 */
	public String getSearchBaseEntityCodeByJTI(String sbeCode) {
		String newSbeCode = searchUtils.getSearchBaseEntityCodeByJTI(sbeCode);
		return newSbeCode;
	}

	/**
	 * Send a search PCM with the correct search code.
	 * @param searchCode Search base entity code
	 * @param pcmCode The code of pcm to send
	 * @param queQuickSearch The code of quick search
	 */
	public void sendPCM(String searchCode, String pcmCode,String queQuickGp) {
		BaseEntity main = beUtils.getBaseEntity(GennyConstants.PCM_CONTENT);
		BaseEntity pcmTable = beUtils.getBaseEntity(pcmCode);

		/* loc 1 */
		Attribute mainAtt = qwandaUtils.getAttribute(GennyConstants.PRI_LOC1);
		EntityAttribute mainEA = new EntityAttribute(main, mainAtt, 1.0, GennyConstants.PCM_TABLE);
		main.addAttribute(mainEA);

		Attribute pcmAtt = qwandaUtils.getAttribute(GennyConstants.PRI_LOC1);
		EntityAttribute pcmEA = new EntityAttribute(pcmTable, pcmAtt, 1.0, searchCode);
		pcmTable.addAttribute(pcmEA);

		/* loc 2 */
		pcmAtt = qwandaUtils.getAttribute(GennyConstants.PRI_LOC2);
		pcmEA = new EntityAttribute(pcmTable, pcmAtt, 1.0, GennyConstants.LNK_PERSON);
		pcmTable.addAttribute(pcmEA);

		Attribute priCode = qwandaUtils.getAttribute(GennyConstants.PRI_QUESTION_CODE);
		mainEA = new EntityAttribute(main, priCode, 1.0, queQuickGp);
		pcmTable.addAttribute(mainEA);

		/* loc 3 - saved search popup */
		mainAtt = qwandaUtils.getAttribute(GennyConstants.PRI_LOC3);
		mainEA = new EntityAttribute(main, mainAtt, 1.0, GennyConstants.PCM_SAVED_SEARCH_POPUP);
		pcmTable.addAttribute(mainEA);

		BaseEntity pcm3 = beUtils.getBaseEntity(GennyConstants.PCM_SAVED_SEARCH_POPUP);
		pcmAtt = qwandaUtils.getAttribute(GennyConstants.PRI_LOC2);
		pcmEA = new EntityAttribute(pcm3, pcmAtt, 1.0, GennyConstants.PCM_SAVED_SEARCH);
		pcm3.addAttribute(pcmEA);

		/* loc 3a - saved search inside popup */
		BaseEntity pcm3a = beUtils.getBaseEntity(GennyConstants.PCM_SAVED_SEARCH);

		pcmAtt = qwandaUtils.getAttribute(GennyConstants.PRI_LOC1);
		pcmEA = new EntityAttribute(pcm3a, pcmAtt, 1.0, "");
		pcm3a.addAttribute(pcmEA);

		pcmAtt = qwandaUtils.getAttribute(GennyConstants.PRI_LOC2);
		pcmEA = new EntityAttribute(pcm3a, pcmAtt, 1.0, GennyConstants.PCM_SBE_DETAIL_VIEW);
		pcm3a.addAttribute(pcmEA);

		/* send messages */
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(main);
		msg.add(pcmTable);
		msg.add(pcm3);
		msg.add(pcm3a);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);
		KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
	}

	/**
	 * Send table data
	 * @param sbeCode Search base entity code
	 */
	public void searchTable(String sbeCode) {
		searchUtils.searchTable(sbeCode);
	}

}
