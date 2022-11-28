package life.genny.qwandaq.utils;

import static life.genny.qwandaq.attribute.Attribute.PRI_CODE;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.entity.search.trait.Column;
import life.genny.qwandaq.exception.runtime.DebugException;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.message.MessageData;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.message.QEventDropdownMessage;
import life.genny.qwandaq.message.QSearchMessage;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.Page;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;

/**
 * A utility class used for performing table
 * searches and search related operations.
 * 
 * @author Jasper Robison
 */
@ApplicationScoped
public class SearchUtils {

	static Logger log = Logger.getLogger(SearchUtils.class);
	static Jsonb jsonb = JsonbBuilder.create();

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	CapabilitiesManager capabilityUtils;

	@Inject
	ServiceToken serviceToken;

	@Inject
	UserToken userToken;

	/**
	 * Call the Fyodor API to fetch a list of {@link BaseEntity}
	 * objects using a {@link SearchEntity} object.
	 *
	 * @param searchEntity A {@link SearchEntity} object used to determine the
	 *                     results
	 * @return A list of {@link BaseEntity} objects
	 */
	public List<BaseEntity> searchBaseEntitys(SearchEntity searchEntity) {

		// build uri, serialize payload and fetch data from fyodor
		String uri = GennySettings.fyodorServiceUrl() + "/api/search/fetch";
		String json = jsonb.toJson(searchEntity);
		HttpResponse<String> response = HttpUtils.post(uri, json, userToken);

		if (response == null) {
			log.error("Null response from " + uri);
			return null;
		}

		Integer status = response.statusCode();

		if (Response.Status.Family.familyOf(status) != Response.Status.Family.SUCCESSFUL) {
			log.error("Bad response status " + status + " from " + uri);
		}

		try {
			// deserialise and grab entities
			Page results = jsonb.fromJson(response.body(), Page.class);
			return results.getItems();
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Call the Fyodor API to fetch a list of codes
	 * associated with result entities.
	 *
	 * @param searchEntity A {@link SearchEntity} object used to determine the
	 *                     results
	 * @return A list of code strings
	 */
	public List<String> searchBaseEntityCodes(SearchEntity searchEntity) {

		// build uri, serialize payload and fetch data from fyodor
		String uri = GennySettings.fyodorServiceUrl() + "/api/search";
		String json = jsonb.toJson(searchEntity);
		HttpResponse<String> response = HttpUtils.post(uri, json, userToken);

		if (response == null) {
			log.error("Null response from " + uri);
			return null;
		}

		Integer status = response.statusCode();

		if (Response.Status.Family.familyOf(status) != Response.Status.Family.SUCCESSFUL) {
			log.error("Bad response status " + status + " from " + uri);
		}

		try {
			// deserialise and grab entities
			Page results = jsonb.fromJson(response.body(), Page.class);
			return results.getCodes();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Call the Fyodor API to fetch a count of {@link BaseEntity}
	 * objects using a {@link SearchEntity} object.
	 *
	 * @param searchEntity A {@link SearchEntity} object used to determine the
	 *                     results
	 * @return A count of items
	 */
	public Long countBaseEntitys(SearchEntity searchEntity) {

		// build uri, serialize payload and fetch data from fyodor
		String uri = GennySettings.fyodorServiceUrl() + "/api/search/count";
		String json = jsonb.toJson(searchEntity);
		HttpResponse<String> response = HttpUtils.post(uri, json, userToken);

		if (response == null) {
			log.error("Null response from " + uri);
			return null;
		}

		Integer status = response.statusCode();

		if (Response.Status.Family.familyOf(status) != Response.Status.Family.SUCCESSFUL) {
			log.error("Bad response status " + status + " from " + uri);
		}

		try {
			// deserialise and return count
			Long results = jsonb.fromJson(response.body(), Long.class);
			return results;
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Perform a table like search in Genny using a {@link SearchEntity} code.
	 * The respective {@link SearchEntity} will be fetched from the cache befor
	 * processing.
	 *
	 * @param code the code of the SearchEntity to grab from cache and search
	 */
	public void searchTable(String code) {

		if (!code.startsWith(Prefix.SBE))
			throw new DebugException("Code " + code + " does not represent a SearchEntity");

		log.info("SBE CODE   ::   " + code);

		// fetch search from cache
		String sessionCode = sessionSearchCode(code);
		SearchEntity searchEntity = CacheUtils.getObject(userToken.getProductCode(), "LAST-SEARCH:" + sessionCode, SearchEntity.class);
		if (searchEntity == null) {
			searchEntity = CacheUtils.getObject(userToken.getProductCode(), code, SearchEntity.class);
			searchEntity.setCode(sessionCode);
		}

		searchTable(searchEntity);
	}

	/**
	 * Perform a table like search in Genny using a {@link SearchEntity}.
	 *
	 * @param searchEntity the SearchEntity to search
	 */
	public void searchTable(SearchEntity searchEntity) {

		if (searchEntity == null)
			throw new NullPointerException("searchEntity");

		CacheUtils.putObject(userToken.getProductCode(), "LAST-SEARCH:" + searchEntity.getCode(),
				searchEntity);

		// remove JTI from code
		searchEntity.setCode(removeJTI(searchEntity.getCode()));

		// package and send search message to fyodor
		QSearchMessage searchBeMsg = new QSearchMessage(searchEntity);
		searchBeMsg.setToken(userToken.getToken());
		searchBeMsg.setDestination(GennyConstants.EVENT_WEBCMDS);
		KafkaUtils.writeMsg(KafkaTopic.SEARCH_EVENTS, searchBeMsg);
	}

	/**
	 * @param searchEntity
	 * @return
	 */
	public String sessionSearchCode(SearchEntity searchEntity) {
		return sessionSearchCode(searchEntity.getCode());
	}

	/**
	 * @param code
	 * @return
	 */
	public String sessionSearchCode(String code) {
		String jti = userToken.getJTI().toUpperCase();
		return (code.contains(jti) ? code : new StringBuilder(code).append("_").append(jti).toString());
	}

	/**
	 * @param searchEntity
	 * @return
	 */
	public String removeJTI(SearchEntity searchEntity) {
		return removeJTI(searchEntity.getCode());
	}

	/**
	 * @param code
	 * @return
	 */
	public String removeJTI(String code) {
		String jti = userToken.getJTI().toUpperCase();
		return code.replace("_".concat(jti), "");
	}

	/**
	 * @param searchBE the SearchEntity to send filter questions for
	 */
	public void sendFilterQuestions(SearchEntity searchBE) {
		log.error("Function not complete!");
	}


	/**
	 * Perform a dropdown search through dropkick.
	 *
	 * @param ask the ask to perform dropdown search for
	 */
	public void performDropdownSearch(Ask ask) {

		// setup message data
		MessageData messageData = new MessageData();
		messageData.setCode(ask.getQuestion().getCode());
		messageData.setSourceCode(ask.getSourceCode());
		messageData.setTargetCode(ask.getTargetCode());
		messageData.setValue("");

		// setup dropdown message and assign data
		QEventDropdownMessage msg = new QEventDropdownMessage();
		msg.setData(messageData);
		msg.setAttributeCode(ask.getQuestion().getAttribute().getCode());

		// publish to events for dropkick
		msg.setToken(userToken.getToken());
		KafkaUtils.writeMsg(KafkaTopic.EVENTS, msg);
	}

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
	public Ask getFilterGroupBySearchBE(String sbeCode, String questionCode,
			Map<String, Map<String, String>> listParam) {
		Ask ask = new Ask();
		ask.setName(GennyConstants.FILTERS);

		Question question = new Question();
		question.setAttributeCode(Attribute.QQQ_QUESTION_GROUP);
		ask.setQuestion(question);

		Ask addFilterAsk = getAddFilterGroupBySearchBE(sbeCode, questionCode);
		ask.add(addFilterAsk);

		Ask existFilterAsk = getExistingFilterGroupBySearchBE(sbeCode, listParam);

		ask.add(existFilterAsk);

		return ask;
	}

	/**
	 * Change existing filter group
	 * 
	 * @param sbeCode       Search base entity code
	 * @param ask           Ask existing group
	 * @param listFilParams List of filter parameters
	 */
	public void setExistingFilterGroup(String sbeCode, Ask ask, Map<String, Map<String, String>> listFilParams) {
		for (Map.Entry<String, Map<String, String>> filterParams : listFilParams.entrySet()) {
			Ask childAsk = new Ask();
			childAsk.getQuestion().setAttributeCode(Attribute.PRI_EVENT);

			String html = getHtmlByFilterParam(filterParams.getValue());

			Question question = new Question();
			question.setCode(filterParams.getKey());
			question.setName(html);
			question.setHtml(html);

			childAsk.setName(html);
			childAsk.setHidden(false);
			childAsk.getQuestion().setCode(filterParams.getKey());
			childAsk.setQuestion(question);
			childAsk.setTargetCode(getSearchBaseEntityCodeByJTI(sbeCode));

			ask.add(childAsk);
		}
	}

	/**
	 * Return filter tag key
	 * 
	 * @param filterParams Filter Parameters
	 * @param index        Index of list filter
	 * @return Filter tag key
	 */
	public String getFilterTagKey(Map<String, String> filterParams, int index) {
		String attrCode = getFilterParamValByKey(filterParams, GennyConstants.ATTRIBUTECODE);
		String partKey = getLastWord(attrCode).toUpperCase();
		String filterTagKey = GennyConstants.QUE_TAG_PREF + partKey + "_" + index;
		return filterTagKey;
	}

	/**
	 * Return Html value by filter parameters
	 * 
	 * @param filterParams
	 * @return Html value by filter parameters
	 */
	public String getHtmlByFilterParam(Map<String, String> filterParams) {
		String attrCode = getFilterParamValByKey(filterParams, GennyConstants.ATTRIBUTECODE)
				.replaceFirst(Prefix.PRI, "");

		String attrName = getFilterParamValByKey(filterParams, GennyConstants.QUE_FILTER_OPTION);
		String value = getFilterParamValByKey(filterParams, GennyConstants.QUE_FILTER_VALUE);
		String attrNameStrip = attrName.replaceFirst(Prefix.SEL, "")
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
		int lastIndex = str.lastIndexOf("_");
		if (lastIndex > -1) {
			return str.substring(lastIndex + 1, str.length());
		}
		return str;
	}

	/**
	 * Get parameter value by key
	 * 
	 * @param key Parameter Key
	 */
	public String getFilterParamValByKey(Map<String, String> filterParams, String key) {
		String value = "";
		if (filterParams == null)
			return value;
		if (filterParams.containsKey(key)) {
			value = filterParams.get(key);
		}
		String finalVal = value.replace("\"", "")
				.replace("[", "").replace("]", "")
				.replaceFirst(GennyConstants.SEL_FILTER_COLUMN_FLC, "");

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
		BaseEntity source = beUtils.getBaseEntity(sourceCode);
		BaseEntity target = beUtils.getBaseEntity(sbeCode);

		String sbeCodeJti = getSearchBaseEntityCodeByJTI(sbeCode);
		Ask ask = qwandaUtils.generateAskFromQuestionCode(GennyConstants.QUE_ADD_FILTER_GRP, source, target);
		ask.getChildAsks().stream().forEach(e -> {
			if (e.getQuestion().getCode().equalsIgnoreCase(GennyConstants.QUE_FILTER_COLUMN)
					|| e.getQuestion().getCode().equalsIgnoreCase(GennyConstants.QUE_FILTER_OPTION)
					|| e.getQuestion().getCode().equalsIgnoreCase(Question.QUE_SUBMIT)) {
				e.setHidden(false);
			} else if (e.getQuestion().getCode().equalsIgnoreCase(questionCode)) {
				e.setHidden(false);
			} else {
				e.setHidden(true);
			}

			e.setTargetCode(sbeCodeJti);
		});

		String targetCode = getSearchBaseEntityCodeByJTI(sbeCode);
		ask.setTargetCode(targetCode);

		Ask askSubmit = qwandaUtils.generateAskFromQuestionCode(Question.QUE_SUBMIT, source, target);
		ask.setTargetCode(targetCode);

		ask.add(askSubmit);

		return ask;
	}

	/**
	 * Construct existing filter group object in Add Filter group form
	 * 
	 * @param sbeCode       Search Base Entity Code
	 * @param listFilParams List of Filter parameters
	 * @return return existing filter group object
	 */
	public Ask getExistingFilterGroupBySearchBE(String sbeCode, Map<String, Map<String, String>> listFilParams) {
		Ask ask = new Ask();
		ask.setName(GennyConstants.FILTER_QUE_EXIST_NAME);
		String targetCode = getSearchBaseEntityCodeByJTI(sbeCode);
		ask.setSourceCode(userToken.getUserCode());
		ask.setTargetCode(targetCode);

		Question question = new Question();
		question.setCode(GennyConstants.FILTER_QUE_EXIST);
		question.setAttributeCode(Attribute.QQQ_QUESTION_GROUP);

		// change exist filter group
		if (listFilParams.size() > 0) {
			setExistingFilterGroup(sbeCode, ask, listFilParams);
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

		msg.setParentCode(GennyConstants.QUE_ADD_FILTER_GRP);
		msg.setLinkCode(Attribute.LNK_CORE);
		msg.setLinkValue(Attribute.LNK_ITEMS);
		msg.setQuestionCode(GennyConstants.QUE_FILTER_COLUMN);

		List<BaseEntity> baseEntities = new ArrayList<>();

		searchBE.getBaseEntityAttributes().stream()
				.filter(e -> e.getAttributeCode().startsWith(Prefix.FLC))
				.forEach(e -> {
					BaseEntity baseEntity = new BaseEntity();
					List<EntityAttribute> entityAttributes = new ArrayList<>();

					EntityAttribute ea = new EntityAttribute();
					String attrCode = e.getAttributeCode().replaceFirst(Prefix.FLC, "");
					ea.setAttributeName(e.getAttributeName());
					ea.setAttributeCode(attrCode);

					String baseCode = GennyConstants.FILTER_SEL + Prefix.FLC + attrCode;
					ea.setBaseEntityCode(baseCode);
					ea.setValueString(e.getAttributeName());

					entityAttributes.add(ea);

					baseEntity.setCode(baseCode);
					baseEntity.setName(e.getAttributeName());

					baseEntity.setBaseEntityAttributes(entityAttributes);
					baseEntities.add(baseEntity);
				});

		msg.setItems(baseEntities);

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

		base.setParentCode(GennyConstants.QUE_ADD_FILTER_GRP);
		base.setLinkCode(Attribute.LNK_CORE);
		base.setLinkValue(Attribute.LNK_ITEMS);
		base.setQuestionCode(GennyConstants.QUE_FILTER_OPTION);
		questionCode = questionCode.toUpperCase();

		if (questionCode.equals(GennyConstants.QUE_FILTER_VALUE_DJP_HC)) {
			base.add(beUtils.getBaseEntity(GennyConstants.SEL_EQUAL_TO));
			base.add(beUtils.getBaseEntity(GennyConstants.SEL_NOT_EQUAL_TO));
			return base;
		} else if (questionCode.equals(GennyConstants.QUE_FILTER_VALUE_DATE)
				|| questionCode.equals(GennyConstants.QUE_FILTER_VALUE_DATETIME)
				|| questionCode.equals(GennyConstants.QUE_FILTER_VALUE_TIME)) {
			base.add(beUtils.getBaseEntity(GennyConstants.SEL_GREATER_THAN));
			base.add(beUtils.getBaseEntity(GennyConstants.SEL_GREATER_THAN_OR_EQUAL_TO));
			base.add(beUtils.getBaseEntity(GennyConstants.SEL_LESS_THAN));
			base.add(beUtils.getBaseEntity(GennyConstants.SEL_LESS_THAN_OR_EQUAL_TO));
			base.add(beUtils.getBaseEntity(GennyConstants.SEL_EQUAL_TO));
			base.add(beUtils.getBaseEntity(GennyConstants.SEL_NOT_EQUAL_TO));
			return base;
		} else if (questionCode.equals(GennyConstants.QUE_FILTER_VALUE_COUNTRY)
				|| questionCode.equals(GennyConstants.QUE_FILTER_VALUE_INTERNSHIP_TYPE)
				|| questionCode.equals(GennyConstants.QUE_FILTER_VALUE_STATE)
				|| questionCode.equals(GennyConstants.QUE_FILTER_VALUE_ACADEMY)
				|| questionCode.equals(GennyConstants.QUE_FILTER_VALUE_DJP_HC)) {
			base.add(beUtils.getBaseEntity(GennyConstants.SEL_EQUAL_TO));
			base.add(beUtils.getBaseEntity(GennyConstants.SEL_NOT_EQUAL_TO));
			return base;
		} else {
			base.add(beUtils.getBaseEntity(GennyConstants.SEL_EQUAL_TO));
			base.add(beUtils.getBaseEntity(GennyConstants.SEL_NOT_EQUAL_TO));
			base.add(beUtils.getBaseEntity(GennyConstants.SEL_LIKE));
			base.add(beUtils.getBaseEntity(GennyConstants.SEL_NOT_LIKE));
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
		base.setLinkCode(Attribute.LNK_CORE);
		base.setLinkValue(Attribute.LNK_ITEMS);
		base.setQuestionCode(queCode);

		SearchEntity searchBE = new SearchEntity(GennyConstants.SBE_DROPDOWN, GennyConstants.SBE_DROPDOWN)
				.add(new Column(PRI_CODE, GennyConstants.PRI_CODE_LABEL));
		searchBE.setRealm(userToken.getProductCode());
		searchBE.setLinkCode(lnkCode);
		searchBE.setLinkValue(lnkVal);
		searchBE.setPageStart(0).setPageSize(1000);

		List<BaseEntity> baseEntities = searchBaseEntitys(searchBE);
		base.setItems(baseEntities);

		return base;
	}

	/**
	 * Return ask with bucket filter options
	 * 
	 * @param queCode  Question Code
	 * @param lnkCode  Link Code
	 * @param lnkValue Link Value
	 * @return Bucket filter options
	 */
	public SearchEntity getBucketFilterOptions(String sbeCode, String queCode, String lnkCode, String lnkValue) {
		SearchEntity searchBE = new SearchEntity(sbeCode, GennyConstants.SBE_DROPDOWN)
				.add(new Column(lnkCode, lnkValue));
		searchBE.setRealm(userToken.getProductCode());
		searchBE.setPageStart(0).setPageSize(100);

		return searchBE;
	}
}
