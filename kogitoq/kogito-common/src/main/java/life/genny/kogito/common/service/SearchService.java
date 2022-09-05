package life.genny.kogito.common.service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import life.genny.qwandaq.message.QCmdMessage;
import life.genny.qwandaq.message.QSearchMessage;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.DefUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.utils.SearchUtils;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.entity.EntityEntity;
import life.genny.qwandaq.message.QDataAskMessage;

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
		FILTER
	}

	/**
	 * Perform a Detail View search.
	 *
	 * @param targetCode The code of the target to display
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
		searchEntity.addFilter("PRI_CODE", SearchEntity.StringFilter.EQUAL, targetCode);

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
		KafkaUtils.writeMsg("webcmds", msg);
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
		KafkaUtils.writeMsg(GennyConstants.EVENT_WEBCMDS, msgProcess);

		QCmdMessage msgCodes = new QCmdMessage(GennyConstants.BUCKET_CODES,GennyConstants.BUCKET_CODES);
		msgCodes.setToken(userToken.getToken());
		msgCodes.setSourceCode(GennyConstants.BUCKET_CODES);
		msgCodes.setTargetCodes(bucketCodes);
		KafkaUtils.writeMsg(GennyConstants.EVENT_WEBCMDS, msgCodes);
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
	 * @param token Token
	 * @param searchBE Search base entity from cache
	 */
	public void sendMessageBySearchEntity(SearchEntity searchBE) {
		QSearchMessage searchBeMsg = new QSearchMessage(searchBE);
		searchBeMsg.setToken(userToken.getToken());
		searchBeMsg.setDestination(GennyConstants.EVENT_WEBCMDS);
		KafkaUtils.writeMsg(GennyConstants.EVENT_SEARCH, searchBeMsg);
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
	 * @param attrCode Attribute code
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
				searchBE.addFilter(code, SearchEntity.StringFilter.LIKE, value);
			}

		}else if(ops.equals(SearchOptions.PAGINATION)) { //pagination
			Optional<EntityAttribute> aeIndex = searchBE.findEntityAttribute(GennyConstants.PAGINATION_INDEX);
			Integer pageSize = searchBE.getPageSize(0);
			Integer indexVal = 0;
			Integer pagePos = 0;

			if(aeIndex.isPresent() && pageSize !=null) {
				if(code.equalsIgnoreCase(GennyConstants.PAGINATION_NEXT)) {
					indexVal = aeIndex.get().getValueInteger() + 1;
				} else if (code.equalsIgnoreCase(GennyConstants.PAGINATION_PREV)) {
					indexVal = aeIndex.get().getValueInteger() - 1;
				}

				pagePos = (indexVal - 1) * pageSize;
			}
			searchBE.setPageStart(pagePos);
			searchBE.setPageIndex(indexVal);
		}
		CacheUtils.putObject(userToken.getRealm(), targetCode, searchBE);

		sendMessageBySearchEntity(searchBE);
		sendSearchPCM(GennyConstants.PCM_TABLE, targetCode);
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
				searchBE.addFilter(code, SearchEntity.StringFilter.LIKE, value);
			}

			CacheUtils.putObject(userToken.getRealm(), targetCode, searchBE);

			sendMessageBySearchEntity(searchBE);
			sendSearchPCM(GennyConstants.PCM_PROCESS, targetCode);
		}
	}


	/**
	 * Return ask with filter group content
	 * @param searchBE Search Base Entity
	 * @return Ask
	 */
	public Ask getFilterGroupBySearchBE(SearchEntity searchBE) {
		Ask ask = new Ask();
		ask.setName(GennyConstants.FILTERS);
		String  targetCode = searchBE.getCode() +  "_" + userToken.getJTI().toUpperCase();
		ask.setTargetCode(targetCode);

		String filterCode = GennyConstants.QUE_FILTER_GRP + "_" + targetCode;
		Question question = new Question();
		question.setCode(filterCode);
		question.setAttributeCode(GennyConstants.QUE_QQQ_GROUP);

		ask.setQuestion(question);
		Ask addFilterAsk = getAddFilterGroupBySearchBE(searchBE);
		ask.addChildAsk(addFilterAsk);

		Ask existFilterAsk = getExistingFilterGroupBySearchBE(searchBE);
		ask.addChildAsk(existFilterAsk);

		return ask;
	}

	/**
	 * Return ask with add filter group content
	 * @param searchBE Search Base entity
	 * @return Ask
	 */
	public Ask getAddFilterGroupBySearchBE(SearchEntity searchBE) {
		String sourceCode = userToken.getUserCode();
		String targetCode = searchBE.getCode() +  "_" + userToken.getJTI().toUpperCase();
		BaseEntity source = beUtils.getBaseEntityByCode(sourceCode);
		BaseEntity target = beUtils.getBaseEntityByCode(targetCode);

		Ask ask = qwandaUtils.generateAskFromQuestionCode(GennyConstants.QUE_ADD_FILTER_GRP, source, target);
		Ask askSubmit = qwandaUtils.generateAskFromQuestionCode("QUE_SUBMIT", source, target);
		ask.addChildAsk(askSubmit);

		return ask;
	}

	/**
	 * Construct exist filter group object
	 * @param searchBE Search Base Entity
	 * @return return existing filter group object
	 */
	public Ask getExistingFilterGroupBySearchBE(SearchEntity searchBE) {
		Ask ask = new Ask();
		ask.setName(GennyConstants.FILTER_QUE_EXIST_NAME);
		String  targetCode = searchBE.getCode() +  "_" + userToken.getJTI().toUpperCase();
		ask.setSourceCode(userToken.getUserCode());
		ask.setTargetCode(targetCode);

		Question question = new Question();
		question.setCode(GennyConstants.FILTER_QUE_EXIST);
		question.setAttributeCode(GennyConstants.QUE_QQQ_GROUP);

		ask.setQuestion(question);
		return ask;
	}

	/**
	 * Return Message of filter column
	 * @param searchBE Search Base Entity
	 * @return Message of Filter column
	 */
	public QDataBaseEntityMessage getFilterColumBySearchBE(SearchEntity searchBE) {
		QDataBaseEntityMessage base = new QDataBaseEntityMessage();

		base.setParentCode(GennyConstants.QUE_ADD_FILTER_GRP);
		base.setLinkCode(GennyConstants.LNK_CORE);
		base.setLinkValue(GennyConstants.LNK_ITEMS);
		base.setQuestionCode(GennyConstants.QUE_FILTER_COLUMN);

		BaseEntity baseEntity = new BaseEntity();
		List<EntityAttribute> entityAttributes = new ArrayList<>();

		searchBE.getBaseEntityAttributes().stream()
			.filter(e -> e.getAttributeCode().startsWith(GennyConstants.FILTER_COL))
			.forEach(e-> {
				EntityAttribute ea = new EntityAttribute();
				String attrCode = e.getAttributeCode().replaceFirst(GennyConstants.FILTER_COL,"");
				ea.setAttributeName(e.getAttributeName());
				ea.setAttributeCode(attrCode);

				String baseCode = GennyConstants.FILTER_SEL + GennyConstants.FILTER_COL + attrCode;
				ea.setBaseEntityCode(baseCode);
				ea.setValueString(e.getAttributeName());

				baseEntity.setCode(baseCode);
				baseEntity.setName(e.getAttributeName());
				entityAttributes.add(ea);

				baseEntity.setBaseEntityAttributes(entityAttributes);
				base.add(baseEntity);
			});

		return base;
	}

	/**
	 * Send filter group and filter column for filter function
	 * @param targetCode Target code
	 */
	public void sendFilterGroup(String targetCode) {
		SearchEntity searchBE = CacheUtils.getObject(userToken.getRealm(), targetCode, SearchEntity.class);

		if(searchBE != null) {
			String filterTargetCode = targetCode + "_" + userToken.getJTI().toUpperCase();

			Ask ask = getFilterGroupBySearchBE(searchBE);

			QDataAskMessage msgFilterGrp = new QDataAskMessage(ask);
			msgFilterGrp.setToken(userToken.getToken());
			msgFilterGrp.setTargetCode(filterTargetCode);
			msgFilterGrp.setMessage(GennyConstants.FILTERS);
			msgFilterGrp.setTag(GennyConstants.FILTERS);
			KafkaUtils.writeMsg(GennyConstants.EVENT_WEBCMDS, msgFilterGrp);


			QDataBaseEntityMessage msgAddFilter = getFilterColumBySearchBE(searchBE);
			msgAddFilter.setToken(userToken.getToken());
			msgAddFilter.setTag("Name");
			KafkaUtils.writeMsg(GennyConstants.EVENT_WEBCMDS, msgAddFilter);
		}

	}

}
