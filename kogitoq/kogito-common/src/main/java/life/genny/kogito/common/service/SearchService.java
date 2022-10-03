package life.genny.kogito.common.service;

import static life.genny.qwandaq.attribute.Attribute.PRI_NAME;
import static life.genny.qwandaq.entity.PCM.PCM_CONTENT;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.PCM;
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

	@Inject
	TaskService tasks;

	public static enum SearchOptions {
		PAGINATION,
		SEARCH,
		FILTER,
		PAGINATION_BUCKET
	}

	private Map<String, String> filterParams = new HashMap<>();

	private Map<String, Map<String, String>> listFilterParams = new HashMap<>();

	/**
	 * Perform a Detail View search.
	 *
	 * @param code The code of the target to display
	 */
	public void sendTable(String code) {

		// trim TREE_ITEM_ from code if present
		code = StringUtils.replaceOnce(code, "_TREE_ITEM_", "_");
		String searchCode = StringUtils.replaceOnce(code, Prefix.QUE, Prefix.SBE);
		log.info("Sending Table :: " + searchCode);

		// send pcm with correct template code
		String userCode = userToken.getUserCode();
		PCM pcm = beUtils.getPCM(PCM.PCM_TABLE);
		pcm.setLocation(1, searchCode);
		tasks.dispatch(userCode, userCode, pcm, PCM_CONTENT, "PRI_LOC1");
	}

	/**
	 * Perform a Detail View search.
	 *
	 * @param targetCode The code of the target to display
	 */
	public void sendDetailView(String targetCode) {

		// fetch target and find it's definition
		BaseEntity target = beUtils.getBaseEntity(targetCode);
		BaseEntity definition = defUtils.getDEF(target);
		String type = StringUtils.removeStart(definition.getCode(), Prefix.DEF);

		// construct template and question codes from type
		String template = "TPL_" + type + "_DETAIL_VIEW";
		String questionCode = Prefix.QUE + type;

		// send pcm with correct info
		String userCode = userToken.getUserCode();
		PCM pcm = beUtils.getPCM(PCM.PCM_DETAIL_VIEW);
		pcm.setTemplateCode(template);
		pcm.setQuestionCode(questionCode);

		tasks.dispatch(userCode, userCode, pcm, PCM_CONTENT, "PRI_LOC1");
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
	}

	/**
	 * Send a search PCM with the correct search code.
	 *
	 * @param pcmCode The code of pcm to send
	 * @param searchCode The code of the searhc to send
	 */
	public void sendSearchPCM(String pcmCode, String searchCode) {

		// update content
		BaseEntity content = beUtils.getBaseEntity(PCM_CONTENT);
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
			String searchCode = StringUtils.replaceOnce(code, Prefix.QUE, Prefix.SBE);

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

		sendBucketFilter(GennyConstants.QUE_BUCKET_INTERNS_GRP,GennyConstants.QUE_SELECT_INTERN,
				GennyConstants.LNK_PERSON, GennyConstants.BKT_APPLICATIONS);

		sendBucketFilterOptions(GennyConstants.SBE_HOST_COMPANIES_VIEW,GennyConstants.QUE_BUCKET_INTERNS_GRP
				,GennyConstants.QUE_SELECT_INTERN,GennyConstants.LNK_PERSON, GennyConstants.ITEMS);
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
				searchBE.add(new Filter(code, Operator.LIKE, value));
			}

		}else if(ops.equals(SearchOptions.PAGINATION) || ops.equals(SearchOptions.PAGINATION_BUCKET)) { //pagination
			Optional<EntityAttribute> aeIndex = searchBE.findEntityAttribute(Attribute.PRI_INDEX);
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

			if (!name.isBlank()) { //searching text
				searchBE.add(new Filter(code, Operator.LIKE, value));
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
	public void sendFilterGroup(String sbeCode, String queGrp,String questionCode,boolean addedJti) {
		try {
			SearchEntity searchBE = CacheUtils.getObject(userToken.getRealm(), sbeCode, SearchEntity.class);

			if (searchBE != null) {
				Ask ask = searchUtils.getFilterGroupBySearchBE(sbeCode, questionCode, listFilterParams);
				QDataAskMessage msgFilterGrp = new QDataAskMessage(ask);
				msgFilterGrp.setToken(userToken.getToken());
				String filterCode = "";
				if(addedJti) {
					filterCode = searchUtils.getSearchBaseEntityCodeByJTI(sbeCode);
					ask.setTargetCode(filterCode);

					filterCode = queGrp + "_" + filterCode;
				}else {
					ask.setTargetCode(sbeCode);
					filterCode = queGrp + "_" + sbeCode;
				}
				msgFilterGrp.setTargetCode(filterCode);
				ask.getQuestion().setCode(filterCode);

				msgFilterGrp.setMessage(GennyConstants.FILTERS);
				msgFilterGrp.setTag(GennyConstants.FILTERS);
				msgFilterGrp.setReplace(true);
				KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msgFilterGrp);

				QDataBaseEntityMessage msgAddFilter = searchUtils.getFilterColumBySearchBE(searchBE);

				msgAddFilter.setToken(userToken.getToken());
				msgAddFilter.setReplace(true);
				KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msgAddFilter);
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
	 *
	 * @return Filter Parameters in the application scope
	 */
	public Map<String, String> getFilterParams() {
		return filterParams;
	}

	/**
	 * Set Filter Pamameters in application scope
	 * @param filterParams
	 */
	public void setFilterParams(Map<String, String> filterParams) {
		this.filterParams = filterParams;
	}


	/**
	 * handle filter by string in the table
	 * @param que Question code
	 * @param attrCode Attribute code
	 * @param oper StringFilter Operator
	 * @param value  Value String
	 * @param cleanSbeCode Search base entity code without JTI
	 */
	public void handleFilterByString(String que, String attrCode, Operator oper ,String value, String cleanSbeCode) {
		String sbeCodeJti = searchUtils.getSearchBaseEntityCodeByJTI(cleanSbeCode);

		SearchEntity searchBE = CacheUtils.getObject(userToken.getRealm(), sbeCodeJti, SearchEntity.class);

		if (oper.equals(Operator.LIKE)) {
			value = "%" + value + "%";
		}
		searchBE.add(new Filter(attrCode, oper, value));

		CacheUtils.putObject(userToken.getRealm(), sbeCodeJti, searchBE);

		sendMessageBySearchEntity(searchBE);
		sendSearchPCM(GennyConstants.PCM_TABLE, sbeCodeJti);
	}

	/**
	 * Handle filter by date time in the table
	 * @param que Question code
	 * @param attrCode Attribute code
	 * @param oper Operator
	 * @param value Filter value
	 * @param targetCode Event target code
	 */
	public void handleFilterByDateTime(String que, String attrCode, Operator oper, LocalDateTime value, String targetCode) {
		SearchEntity searchBE = CacheUtils.getObject(userToken.getRealm(), targetCode, SearchEntity.class);

		searchBE.add(new Filter(attrCode, oper, value));

		CacheUtils.putObject(userToken.getRealm(), targetCode, searchBE);

		sendMessageBySearchEntity(searchBE);
		sendSearchPCM(GennyConstants.PCM_TABLE, targetCode);
	}

	/**
	 * Send message to bucket page with filter data
	 * @param queGroup Question group
	 * @param queCode Question code
	 */
	public void sendBucketFilter(String queGroup,String queCode, String attCode, String targetCode) {
		Ask ask = new Ask();
		ask.setName(GennyConstants.FILTERS);
		Question question = new Question();
		question.setCode(queGroup);
		question.setAttributeCode(GennyConstants.QUE_QQQ_GROUP);
		ask.setQuestion(question);

		Ask childAsk = new Ask();
		childAsk.setName(GennyConstants.FILTERS);
		childAsk.getQuestion().setCode(queCode);
		Question childQuestion = new Question();
		childQuestion.setAttributeCode(attCode);
		childQuestion.setCode(queCode);
		childAsk.getQuestion().getAttribute().setCode(attCode);
		childAsk.setQuestion(childQuestion);
		childAsk.setTargetCode(targetCode);

		ask.add(childAsk);

		QDataAskMessage msg = new QDataAskMessage(ask);
		msg.setToken(userToken.getToken());
		msg.setTargetCode(targetCode);
		msg.setQuestionCode(queGroup);
		msg.setMessage(GennyConstants.FILTERS);
		msg.setReplace(true);
		KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
	}

	/**
	 * Send message to bucket page with filter data
	 * @param queGroup Question group
	 * @param queCode Question code
	 */
	public void sendBucketFilterOptions(String sbeCode, String queGroup,String queCode,String lnkCode, String lnkValue) {
		SearchEntity searchEntity = searchUtils.getBucketFilterOptions(sbeCode,queCode,lnkCode,lnkValue);

		QDataBaseEntityMessage msg = new QDataBaseEntityMessage();
		List<BaseEntity> baseEntities = searchUtils.searchBaseEntitys(searchEntity);
		msg.setToken(userToken.getToken());
		msg.setItems(baseEntities);
		msg.setParentCode(queGroup);
		msg.setQuestionCode(queCode);
		msg.setLinkCode(lnkCode);
		msg.setLinkValue(lnkValue);
		msg.setMessage(GennyConstants.FILTERS);
		msg.setReplace(true);

		KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
	}

	/**
	 * Set parameter value by key
	 * @param key Parameter Key
	 * @param value Parameter Value
	 */
	public void setFilterParamValByKey(String key, String value) {
		filterParams.put(key, value);
	}

	/**
	 * Get parameter value by key
	 * @param key Parameter Key
	 */
	public String getFilterParamValByKey(String key) {
		return searchUtils.getFilterParamValByKey(filterParams,key);
	}

	/**
	 * Return Whether filter tag or not
	 * @param code
	 * @return Whether filter tag or not
	 */
	public boolean isFilterTag(String code) {
		for(Map.Entry<String,Map<String, String>> tag:listFilterParams.entrySet()) {
			String question = tag.getKey();
			if (code.equalsIgnoreCase(question)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Add filter paramer to the list
	 */
	public void addFilterParamToList() {
		Map<String, String> newMap = getCloneFilterParams();
		String filterKey = searchUtils.getFilterTagKey(newMap,listFilterParams.size());
		listFilterParams.put(filterKey, newMap);
	}

	/**
	 * Clone filter parameter
	 * @return Clone of filter parameter
	 */
	public Map<String, String> getCloneFilterParams() {
		Map<String, String> newMap = new HashMap<>();

		filterParams.entrySet().stream().forEach( e-> {
			newMap.put(e.getKey(),e.getValue());
		});
		return newMap;
	}

	/**
	 * Remove current filter tag by html
	 * @param questionCode Question code of event
	 */
	public void removeFilterTag(String questionCode) {
		Set<String> keys = new HashSet<>(listFilterParams.keySet());
		for(String tagKey:keys) {
			if(tagKey.equalsIgnoreCase(questionCode)) {
				listFilterParams.remove(tagKey);
			}
		}
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

}
