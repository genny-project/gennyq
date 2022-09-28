package life.genny.kogito.common.core;

import java.lang.invoke.MethodHandles;
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
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
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

	public static String ASK_CACHE_KEY_FORMAT = "%s:%s";

	/**
	 * Cache an ask for a processId and questionCode combination.
	 * 
	 * @param processData The processData to cache for
	 * @param asks        e ask to cache
	 */
	public void cacheBulkMessage(ProcessData processData, QBulkMessage msg) {

		String key = String.format(ASK_CACHE_KEY_FORMAT, processData.getProcessId(), "MSG");
		CacheUtils.putObject(userToken.getProductCode(), key, msg);
	}

	/**
	 * Fetch an ask from cache for a processId and questionCode combination.
	 * 
	 * @param processData The processData to fetch for
	 * @return
	 */
	public QBulkMessage fetchBulkMessage(ProcessData processData) {

		String key = String.format(ASK_CACHE_KEY_FORMAT, processData.getProcessId(), "MSG");
		QBulkMessage msg = CacheUtils.getObject(userToken.getProductCode(), key, QBulkMessage.class);

		if (msg == null)
			msg = generateBulkMessage(processData);

		return msg;
	}

	/**
	 * Generate the asks and save them to the cache
	 * 
	 * @param processData The process data
	 */
	public QBulkMessage generateBulkMessage(ProcessData processData) {

		String questionCode = processData.getQuestionCode();
		String sourceCode = processData.getSourceCode();
		String targetCode = processData.getTargetCode();
		String processId = processData.getProcessId();

		BaseEntity source = beUtils.getBaseEntity(sourceCode);
		BaseEntity target = beUtils.getBaseEntity(targetCode);

		log.info("Generating asks -> " + questionCode + ":" + source.getCode() + ":" + target.getCode());

		QBulkMessage msg = new QBulkMessage();

		if (questionCode != null) {
			// fetch question from DB
			Ask ask = qwandaUtils.generateAskFromQuestionCode(questionCode, source, target);
			Ask events = createEvents(processData.getEvents(), sourceCode, targetCode);
			ask.add(events);
			msg.add(ask);
		}

		// find first level PCM
		traversePCM(processData.getPcmCode(), source, target, msg);

		// update target and processId
		BaseEntity processEntity = qwandaUtils.generateProcessEntity(processData);
		updateRequiredAskFields(msg.getAsks(), processEntity, processId);

		// cache them for fast fetching
		cacheBulkMessage(processData, msg);

		return msg;
	}

	/**
	 * Traverse a PCM looking for a non-readonly question.
	 * 
	 * @param pcm
	 * @param source
	 * @param target
	 * @return
	 */
	public Boolean traversePCM(String pcmCode, BaseEntity source, BaseEntity target, QBulkMessage msg) {

		if ("PCM_DASHBOARD".equals(pcmCode))
			return false;
		// add pcm to bulk message
		BaseEntity pcm = beUtils.getBaseEntity(pcmCode);
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

		Boolean expecting = false;

		// iterate fields recursively
		List<EntityAttribute> locations = pcm.getBaseEntityAttributes().stream()
				.filter(ea -> ea.getAttribute() != null && ea.getAttribute().getCode() != null)
				.filter(ea -> ea.getAttribute().getCode().startsWith("PRI_LOC"))
				.collect(Collectors.toList());

		for (EntityAttribute entityAttribute : locations) {

			// recursively check PCM fields
			String value = entityAttribute.getAsString();
			if (value.startsWith("PCM_")) {
				if (traversePCM(value, source, target, msg))
					expecting = true;
				continue;
			}

			// cannot check asks if not existant
			if (ask == null)
				continue;

			// find appropriate ask (could use map instead)
			Optional<Ask> child = ask.getChildren().stream()
					.filter(a -> a.getQuestion().getAttributeCode().equals(value))
					.findFirst();
			if (child.isEmpty()) {
				log.warn("No corresponding child for pcm location " + value);
				continue;
			}

			// return true if answer expected
			if (!child.get().getReadonly())
				expecting = true;
		}

		return expecting;
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
	 * @param processData
	 * @return
	 */
	public Boolean isExpectingAnswers(ProcessData processData) {

		QBulkMessage msg = fetchBulkMessage(processData);

		for (Ask ask : msg.getAsks())
			if (containsNonReadonly(ask))
				return true;

		return false;
	}

	/**
	 * @param ask
	 * @return
	 */
	public Boolean containsNonReadonly(Ask ask) {

		if (ask.getReadonly())
			return true;

		if (ask.getChildren() != null) {
			for (Ask child : ask.getChildren())
				if (containsNonReadonly(child))
					return true;
		}

		return false;
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
	 * Send Asks, PCMs and Searches
	 *
	 * @param processData
	 */
	public void sendData(ProcessData processData) {

		// construct bulk message
		QBulkMessage msg = fetchBulkMessage(processData);
		List<Ask> asks = msg.getAsks();

		// check mandatory fields
		BaseEntity processEntity = qwandaUtils.generateProcessEntity(processData);
		Boolean answered = qwandaUtils.mandatoryFieldsAreAnswered(asks, processEntity);

		// pre-send ask updates
		BaseEntity defBE = beUtils.getBaseEntity(processData.getDefinitionCode());
		Map<String, Ask> flatMapOfAsks = qwandaUtils.updateDependentAsks(asks, processEntity, defBE);
		flatMapOfAsks.get("EVT_SUBMIT").setDisabled(!answered);;

		// filter unwanted attributes
		privacyFilter(processEntity, processData.getAttributeCodes());

		log.info("Sending " + processEntity.getBaseEntityAttributes().size() + " processBE attributes");
		msg.add(processEntity);

		// handle initial dropdown selections
		recursivelyHandleDropdownAttributes(asks, processEntity, msg);

		// send to user
		msg.setToken(userToken.getToken());
		msg.setTag("BulkMessage");
		KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);


		// TODO: send searches

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
