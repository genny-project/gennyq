package life.genny.kogito.common.core;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.kogito.common.service.NavigationService;
import life.genny.kogito.common.service.SearchService;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.graphql.ProcessData;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.message.QBulkMessage;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.DefUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.utils.SearchUtils;

/**
 * Dispatch
 */
@ApplicationScoped
public class Dispatch {

	private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	QwandaUtils qwandaUtils;
	@Inject
	BaseEntityUtils beUtils;
	@Inject
	SearchUtils search;


	public static String ASK_CACHE_KEY_FORMAT = "%s:ASKS";

	/**
	 * Cache an ask for a processId and questionCode combination.
	 * 
	 * @param processData The processData to cache for
	 * @param asks        e ask to cache
	 */
	public void cacheAsks(ProcessData processData, List<Ask> asks) {

		String key = String.format(ASK_CACHE_KEY_FORMAT, processData.getProcessId());
		CacheUtils.putObject(userToken.getProductCode(), key, asks);
	}

	/**
	 * Fetch an ask from cache for a processId and questionCode combination.
	 * 
	 * @param processData The processData to fetch for
	 * @return
	 */
	public List<Ask> fetchAsks(ProcessData processData) {

		String key = String.format(ASK_CACHE_KEY_FORMAT, processData.getProcessId());
		List<Ask> asks = CacheUtils.getObject(userToken.getProductCode(), key, List.class);

		return asks;
	}

	/**
	 * Send Asks, PCMs and Searches
	 *
	 * @param processData
	 */
	public void buildAndSend(ProcessData processData) {
		buildAndSend(processData, null);
	}

	/**
	 * Send Asks, PCMs and Searches
	 *
	 * @param processData
	 * @param pcm
	 */
	public void buildAndSend(ProcessData processData, BaseEntity pcm) {

		// fetch source and target entities
		String sourceCode = processData.getSourceCode();
		String targetCode = processData.getTargetCode();
		BaseEntity source = beUtils.getBaseEntity(sourceCode);
		BaseEntity target = beUtils.getBaseEntity(targetCode);

		QBulkMessage msg = new QBulkMessage();

		String questionCode = processData.getQuestionCode();
		if (questionCode != null) {
			// fetch question from DB
			log.info("Generating asks -> " + questionCode + ":" + source.getCode() + ":" + target.getCode());
			Ask ask = qwandaUtils.generateAskFromQuestionCode(questionCode, source, target);
			Ask events = createEvents(processData.getEvents(), sourceCode, targetCode);
			ask.add(events);
			msg.add(ask);
		}

		// init lists
		if (processData.getAttributeCodes() == null)
			processData.setAttributeCodes(new ArrayList<String>());
		if (processData.getSearches() == null)
			processData.setSearches(new ArrayList<String>());

		// traverse pcm to build data
		Map<String, Ask> flatMapOfAsks = new HashMap<String, Ask>();
		pcm = (pcm == null ? beUtils.getBaseEntity(processData.getPcmCode()) : pcm);
		traversePCM(pcm, source, target, flatMapOfAsks, msg, processData);
		List<Ask> asks = msg.getAsks();

		// only build processEntity if answers are expected
		List<String> attributeCodes = processData.getAttributeCodes();
		log.info("Non-Readonly Attributes: " + attributeCodes);
		if (!attributeCodes.isEmpty()) {
			// update target and processId
			BaseEntity processEntity = qwandaUtils.generateProcessEntity(processData);
			updateRequiredAskFields(asks, processEntity, processData.getProcessId());

			// check mandatory fields
			Boolean answered = qwandaUtils.mandatoryFieldsAreAnswered(asks, processEntity);

			// pre-send ask updates
			BaseEntity defBE = beUtils.getBaseEntity(processData.getDefinitionCode());
			qwandaUtils.updateDependentAsks(asks, processEntity, defBE, flatMapOfAsks);
			flatMapOfAsks.get("EVT_SUBMIT").setDisabled(!answered);

			// filter unwanted attributes
			privacyFilter(processEntity, attributeCodes);

			log.info("Sending " + processEntity.getBaseEntityAttributes().size() + " processBE attributes");
			msg.add(processEntity);

			// handle initial dropdown selections
			recursivelyHandleDropdownAttributes(asks, processEntity, msg);

			// only cache for non-readonly invocation
			cacheAsks(processData, asks);
		}

		// update parent pcm
		String parent = processData.getParent();
		if (!"PCM_TREE".equals(parent)) {
			BaseEntity parentPCM = beUtils.getBaseEntity(parent);
			parentPCM.setValue(processData.getLocation(), processData.getPcmCode());
			msg.add(parentPCM);
		}

		QDataAskMessage asksMessage = new QDataAskMessage(msg.getAsks());
		asksMessage.setToken(userToken.getToken());
		QDataBaseEntityMessage baseEntityMessage = new QDataBaseEntityMessage(msg.getEntities());
		baseEntityMessage.setToken(userToken.getToken());

		// send to user
		// msg.setToken(userToken.getToken());
		// msg.setTag("BulkMessage");
		KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, asksMessage);
		KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, baseEntityMessage);

		// send searches
		for (String code : processData.getSearches())
			search.searchTable(code);
	}

	/**
	 * @param code
	 * @param source
	 * @param target
	 * @param map
	 * @param msg
	 * @param processData
	 */
	public void traversePCM(String code, BaseEntity source, BaseEntity target, 
			Map<String, Ask> map, QBulkMessage msg, ProcessData processData) {

		if ("PCM_DASHBOARD".equals(code))
			return;

		// add pcm to bulk message
		BaseEntity pcm = beUtils.getBaseEntity(code);
		traversePCM(pcm, source, target, map, msg, processData);
	}

	/**
	 * Traverse a PCM looking for a non-readonly question.
	 * 
	 * @param pcm
	 * @param source
	 * @param target
	 * @return
	 */
	public void traversePCM(BaseEntity pcm, BaseEntity source, BaseEntity target, 
			Map<String, Ask> map, QBulkMessage msg, ProcessData processData) {

		log.info("Traversing " + pcm.getCode());
		log.info(jsonb.toJson(pcm));
		msg.add(pcm);

		Ask ask = null;
		String questionCode = pcm.getValueAsString("PRI_QUESTION_CODE");
		if (questionCode == null) {
			log.warn("Question Code is null for " + pcm.getCode());
		} else {
			// add ask to bulk message
			ask = qwandaUtils.generateAskFromQuestionCode(questionCode, source, target);
			msg.add(ask);
		}

		// iterate fields recursively
		List<EntityAttribute> locations = pcm.getBaseEntityAttributes().stream()
				.filter(ea -> ea.getAttribute() != null && ea.getAttribute().getCode() != null)
				.filter(ea -> ea.getAttribute().getCode().startsWith("PRI_LOC"))
				.collect(Collectors.toList());

		for (EntityAttribute entityAttribute : locations) {

			log.info(entityAttribute.getAttributeCode());
			// recursively check PCM fields
			String value = entityAttribute.getAsString();
			log.info(value);
			if (value.startsWith(Prefix.PCM)) {
				traversePCM(value, source, target, map, msg, processData);
				continue;
			} else if (value.startsWith(Prefix.SBE)) {
				processData.getSearches().add(value);
				continue;
			}

			// cannot check asks if not existant
			if (ask == null)
				continue;

			// find appropriate ask (could use map instead)
			Optional<Ask> opt = ask.getChildren().stream()
					.filter(a -> a.getQuestion().getAttributeCode().equals(value))
					.findFirst();
			if (opt.isEmpty()) {
				log.warn("No corresponding child for pcm location " + value);
				continue;
			}

			// return true if answer expected
			Ask child = opt.get();
			if (!child.getReadonly())
				processData.getAttributeCodes().add(child.getQuestion().getAttribute().getCode());
		}

	}

	/**
	 * Create the events ask group.
	 * 
	 * @param events     The events string
	 * @param sourceCode The source entity code
	 * @param targetCode The target entity code
	 * @return The events ask group
	 */
	public Ask createEvents(String events, String sourceCode, String targetCode) {

		// fetch attributes and create group
		Attribute groupAttribute = qwandaUtils.getAttribute("QQQ_QUESTION_GROUP");
		Question groupQuestion = new Question("QUE_EVENTS", "", groupAttribute);

		// init ask
		Ask ask = new Ask(groupQuestion, sourceCode, targetCode);
		ask.setRealm(userToken.getProductCode());

		// split events string by comma
		for (String event : events.split(",")) {
			// create child and add to ask
			Attribute attribute = qwandaUtils.createEvent(event, event);
			Question question = new Question("QUE_" + event, event, attribute);
			Ask child = new Ask(question, sourceCode, targetCode);
			ask.add(child);
		}

		return ask;
	}

	/**
	 * Recursively update the ask target and process id.
	 * 
	 * @param asks
	 * @param target
	 * @param processId
	 */
	public void updateRequiredAskFields(List<Ask> asks, BaseEntity target, String processId) {

		for (Ask ask : asks) {
			ask.setTargetCode(target.getCode());
			ask.setProcessId(processId);

			if (ask.hasChildren())
				updateRequiredAskFields(ask.getChildren(), target, processId);
		}
	}

	/**
	 * @param baseEntity
	 * @param codes
	 */
	public void privacyFilter(BaseEntity baseEntity, List<String> codes) {

		// grab all entityAttributes from the entity
		Set<EntityAttribute> entityAttributes = ConcurrentHashMap
				.newKeySet(baseEntity.getBaseEntityAttributes().size());
		for (EntityAttribute ea : baseEntity.getBaseEntityAttributes()) {
			entityAttributes.add(ea);
		}

		for (EntityAttribute ea : entityAttributes) {
			if (!codes.contains(ea.getAttributeCode())) {
				baseEntity.removeAttribute(ea.getAttributeCode());
			}
		}
	}

	/**
	 * Recursively traverse the asks and add any entity selections to the msg.
	 *
	 * @param asks    The ask to traverse
	 * @param target The target entity used in finding values
	 * @param asks    The msg to add entities to
	 */
	public void recursivelyHandleDropdownAttributes(List<Ask> asks, BaseEntity target, QBulkMessage msg) {

		for (Ask ask : asks) {

			// recursively handle any child asks
			if (ask.getChildren() != null) {
				recursivelyHandleDropdownAttributes(ask.getChildren(), target, msg);
			}

			// check for dropdown attribute
			if (ask.getQuestion().getAttribute().getCode().startsWith("LNK_")) {

				// get list of value codes
				List<String> codes = beUtils.getBaseEntityCodeArrayFromLinkAttribute(target, ask.getQuestion().getAttribute().getCode());

				if (codes == null || codes.isEmpty())
					sendDropdownItems(ask, target);
				else
					collectSelections(codes, msg);
			}
		}
	}

	/**
	 * @param codes
	 * @param msg
	 */
	public void collectSelections(List<String> codes, QBulkMessage msg) {

		// fetch entity for each and add to msg
		for (String code : codes) {
			BaseEntity be = beUtils.getBaseEntity(code);
			msg.add(be);
		}
	}

	/**
	 * Recursively traverse the ask to find any already selected dropdown 
	 * items to send, and trigger dropdown searches.
	 *
	 * @param ask The Ask to traverse
	 * @param target The target entity used in processing
	 * @param rootCode The code of the root question used in sending DD messages
	*/
	public void sendDropdownItems(Ask ask, BaseEntity target) {

		Question question = ask.getQuestion();
		Attribute attribute = question.getAttribute();

		if (attribute.getCode().startsWith("LNK_")) {

			// check for already selected items
			List<String> codes = beUtils.getBaseEntityCodeArrayFromLinkAttribute(target, attribute.getCode());
			if (codes != null && !codes.isEmpty()) {

				// grab selection baseentitys
				QDataBaseEntityMessage selectionMsg = new QDataBaseEntityMessage();
				for (String code : codes) {
					if (StringUtils.isBlank(code)) {
						continue;
					}

					BaseEntity selection = beUtils.getBaseEntity(code);

					// Ensure only the PRI_NAME attribute exists in the selection
					selection = beUtils.addNonLiteralAttributes(selection);
					selection = beUtils.privacyFilter(selection, Collections.singleton("PRI_NAME"));
					selectionMsg.add(selection);
				}

				// send selections
				if (selectionMsg.getItems() != null) {
					selectionMsg.setToken(userToken.getToken());
					selectionMsg.setReplace(true);
					log.info("Sending selection items with " + selectionMsg.getItems().size() + " items");
					KafkaUtils.writeMsg(KafkaTopic.WEBDATA, selectionMsg);
				} else {
					log.info("No selection items found for " + attribute.getCode());
				}
			}

			// trigger dropdown search in dropkick
			JsonObject json = Json.createObjectBuilder()
			.add("event_type", "DD")
			.add("data", Json.createObjectBuilder()
				.add("questionCode", question.getCode())
				.add("sourceCode", ask.getSourceCode())
				.add("targetCode", ask.getTargetCode())
				.add("value", "")
				.add("processId", ask.getProcessId()))
			.add("attributeCode", attribute.getCode())
			.add("token", userToken.getToken())
			.build();

			KafkaUtils.writeMsg(KafkaTopic.EVENTS, json.toString());
		}
	}

}
