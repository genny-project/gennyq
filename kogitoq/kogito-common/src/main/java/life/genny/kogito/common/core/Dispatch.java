package life.genny.kogito.common.core;

import life.genny.kogito.common.service.TaskService;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.PCM;
import life.genny.qwandaq.graphql.ProcessData;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.message.QBulkMessage;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static life.genny.qwandaq.entity.PCM.PCM_TREE;

/**
 * Dispatch
 */
@ApplicationScoped
public class Dispatch {

	private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	Jsonb jsonb = JsonbBuilder.create();

	public static final String[] BUTTON_EVENTS = { Attribute.EVT_SUBMIT, Attribute.EVT_NEXT, Attribute.EVT_UPDATE,
			Attribute.EVT_CANCEL, Attribute.EVT_UNDO, Attribute.EVT_REDO, Attribute.EVT_RESET };

	@Inject
	UserToken userToken;

	@Inject
	CapabilitiesManager capMan;

	@Inject
	CacheManager cm;

	@Inject
	QwandaUtils qwandaUtils;
	@Inject
	BaseEntityUtils beUtils;
	@Inject
	SearchUtils search;

	@Inject
	TaskService tasks;

	/**
	 * Send Asks, PCMs and Searches
	 *
	 * @param processData
	 */
	public QBulkMessage build(ProcessData processData) {
		return build(processData, null);
	}

	/**
	 * Send Asks, PCMs and Searches
	 *
	 * @param processData
	 * @param pcm
	 */
	public QBulkMessage build(ProcessData processData, PCM pcm) {

		ReqConfig reqConfig = capMan.getUserCapabilities();

		// fetch source and target entities
		String sourceCode = processData.getSourceCode();
		String targetCode = processData.getTargetCode();
		BaseEntity source = beUtils.getBaseEntity(sourceCode);
		BaseEntity target = beUtils.getBaseEntity(targetCode);

		// ensure pcm is not null
		pcm = (pcm == null ? beUtils.getPCM(processData.getPcmCode()) : pcm);

		QBulkMessage msg = new QBulkMessage();
		msg.setTag("Dispatch:build:line93");
		// check for a provided question code
		String questionCode = processData.getQuestionCode();
		if (questionCode != null) {
			// fetch question from DB
			log.info("Generating asks -> " + questionCode + ":" + source.getCode() + ":" + target.getCode());
			Ask ask = qwandaUtils.generateAskFromQuestionCode(questionCode, source, target, reqConfig);
			msg.add(ask);
		}

		// generate events if specified
		String buttonEvents = processData.getButtonEvents();
		if (buttonEvents != null) {
			Ask eventsAsk = createButtonEvents(buttonEvents, sourceCode, targetCode);
			msg.add(eventsAsk);

			// TODO: fix this as it removes flexibility
			PCM eventsPCM = beUtils.getPCM(PCM.PCM_EVENTS);
			// Now set the unique code of the PCM_EVENTS so that it is unique
			eventsPCM.setCode("PCM_EVENTS");
			// msg.add(eventsPCM);
			// Now update the PCM to point the last location to the PCM_EVENTS
			// add a LOC2 to the PCM if it doesn't exist
			if (pcm.getLocation(2) == null) {
				pcm.addStringAttribute("PRI_LOC2", "LOC2", PCM.PCM_EVENTS);
			}
			pcm.setLocation(2, eventsPCM.getCode()); // TODO - find last location?
		}

		// init if null to stop null pointers
		if (processData.getAttributeCodes() == null)
			processData.setAttributeCodes(new ArrayList<String>());
		if (processData.getSearches() == null)
			processData.setSearches(new ArrayList<String>());

		// traverse pcm to build data
		traversePCM(pcm, source, target, msg, processData);
		// update questionCode after traversing
		if (questionCode != null)
			pcm.setQuestionCode(questionCode);

		/**
		 * update parent pcm
		 * NOTE: the null check protects from cases where
		 * dispatch is called with a null parent and location.
		 * This occurs when triggering dispatch for a pcm
		 * that requires a different target code.
		 */
		String parent = processData.getParent();
		if (parent != null && !PCM_TREE.equals(parent)) {
			PCM parentPCM = beUtils.getPCM(parent);
			Integer location = PCM.findLocation(processData.getLocation());
			log.info("Updating " + parentPCM.getCode() + " : Location " + location + " -> " + processData.getPcmCode());
			parentPCM.setLocation(location, processData.getPcmCode());
			msg.add(parentPCM);
		}

		return msg;
	}

	/**
	 * @param processData
	 * @param msg
	 * @return
	 */
	public Boolean containsNonReadonly(Map<String, Ask> flatMapOfAsks, ProcessData processData) {

		// only build processEntity if answers are expected
		findReadonlyAttributeCodes(flatMapOfAsks, processData);
		List<String> attributeCodes = processData.getAttributeCodes();
		log.info("Non-Readonly Attributes: " + attributeCodes);
		return !attributeCodes.isEmpty();
		// if (!attributeCodes.isEmpty())
		// return true;

		// return false;
	}

	/**
	 * Build the process entity and perform checks
	 * only required for asks expecting answers.
	 *
	 * @param processData
	 * @param asks
	 * @param flatMapOfAsks
	 * @param msg
	 */
	public BaseEntity handleNonReadonly(ProcessData processData, List<Ask> asks, Map<String, Ask> flatMapOfAsks,
			QBulkMessage msg) {
		// update all asks target and processId
		BaseEntity processEntity = qwandaUtils.generateProcessEntity(processData);
		String code = processEntity.getCode();
		String processId = processData.getProcessId();
		for (Ask ask : flatMapOfAsks.values()) {
			ask.setTargetCode(code);
			ask.setProcessId(processId);
		}

		// TODO: This should not be necessary, but is
		for (Ask ask : asks)
			qwandaUtils.recursivelySetProcessId(ask, processId);

		// check mandatory fields
		// TODO: change to use flatMap
		Boolean answered = qwandaUtils.mandatoryFieldsAreAnswered(flatMapOfAsks, processEntity);

		// pre-send ask updates
		BaseEntity defBE = beUtils.getBaseEntity(processData.getDefinitionCode());
		qwandaUtils.updateDependentAsks(processEntity, defBE, flatMapOfAsks);

		// update any button Events
		for (String event : BUTTON_EVENTS) {
			Ask evt = flatMapOfAsks.get(event);
			if (evt != null)
				evt.setDisabled(!answered);
		}
		// this is ok since flatmap is referencing asks
		msg.getAsks().addAll(asks);

		// filter unwanted attributes
		List<String> attributeCodes = processData.getAttributeCodes();
		privacyFilter(processEntity, attributeCodes);
		log.info("ProcessEntity contains " + processEntity.getBaseEntityAttributes().size() + " attributes");

		return processEntity;
	}

	/**
	 * @param map
	 * @param processData
	 */
	public void findReadonlyAttributeCodes(Map<String, Ask> map, ProcessData processData) {

		log.info("Finding non readonlys");
		for (Ask ask : map.values()) {
			log.info("Looking at ask: " + ask.getQuestion().getCode() + ", readonly: " + ask.getReadonly());
			// add to active attrbute codes if answer expected
			if (!ask.getReadonly())
				processData.getAttributeCodes().add(ask.getQuestion().getAttribute().getCode());
		}
	}

	/**
	 * Fetch a PCM to traverse, looking for a non-readonly question.
	 *
	 * @param code
	 * @param source
	 * @param target
	 * @param map
	 * @param msg
	 * @param processData
	 */
	public void traversePCM(String code, BaseEntity source, BaseEntity target,
			QBulkMessage msg, ProcessData processData) {

		// add pcm to bulk message
		PCM pcm = beUtils.getPCM(code);
		traversePCM(pcm, source, target, msg, processData);
	}

	/**
	 * Traverse a PCM looking for a non-readonly question.
	 * 
	 * @param pcm
	 * @param source
	 * @param target
	 * @return
	 */
	public void traversePCM(PCM pcm, BaseEntity source, BaseEntity target,
			QBulkMessage msg, ProcessData processData) {

		ReqConfig reqConfig = capMan.getUserCapabilities();
		if (!pcm.requirementsMet(reqConfig)) {
			log.warn("User " + userToken.getUserCode() + " Capability requirements not met for pcm: " + pcm.getCode());
			return;
		}
		log.info("Traversing " + pcm.getCode());
		if ("PCM_FORM_EDIT".equals(pcm.getCode())) { // TODO, this could be better
			log.info("Found PCM_FORM_EDIT");
			pcm.setQuestionCode(processData.getQuestionCode()); // Need to override the hard wired QUE_BASEENTITY_GRP
		}
		log.info(jsonb.toJson(pcm));

		// check for a question code
		Ask ask = null;
		String questionCode = pcm.getValueAsString(Attribute.PRI_QUESTION_CODE);
		log.info("PcmCode = " + pcm.getCode() + ", QuestionCode = " + questionCode);
		if (questionCode != null) {
			// use pcm target if one is specified
			String targetCode = pcm.getTargetCode();
			if (targetCode != null && !targetCode.equals(target.getCode())) {
				// merge targetCode
				Map<String, Object> ctxMap = new HashMap<>();
				ctxMap.put("TARGET", target);
				targetCode = MergeUtils.merge(targetCode, ctxMap);
				// providing a null parent & location since it is already set in the parent
				tasks.dispatch(source.getCode(), targetCode, pcm, null, null);
				return;
			} else if (!Question.QUE_EVENTS.equals(questionCode)) {
				// add ask to bulk message
				ask = qwandaUtils.generateAskFromQuestionCode(questionCode, source, target, reqConfig);
				msg.add(ask);
			}
		} else {
			log.warn("Question Code is null for " + pcm.getCode());
		}

		// add pcm for sending
		msg.add(pcm);

		// iterate locations
		List<EntityAttribute> locations = pcm.findPrefixEntityAttributes(Prefix.LOCATION);
		for (EntityAttribute entityAttribute : locations) {

			// recursively check PCM fields
			String value = entityAttribute.getAsString();
			if (value.startsWith(Prefix.PCM)) {
				traversePCM(value, source, target, msg, processData);
				continue;
			} else if (value.startsWith(Prefix.SBE)) {
				processData.getSearches().add(value);
				continue;
			}
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
	public Ask createButtonEvents(String buttonEvents, String sourceCode, String targetCode) {

		// fetch attributes and create group
		Attribute groupAttribute = cm.getAttribute(Attribute.QQQ_QUESTION_GROUP);
		Question groupQuestion = new Question(Question.QUE_EVENTS, "", groupAttribute);

		// init ask
		Ask ask = new Ask(groupQuestion, sourceCode, targetCode);
		ask.setRealm(userToken.getProductCode());

		// split events string by comma
		for (String name : buttonEvents.split(",")) {
			String code = name.toUpperCase();
			// create child and add to ask
			Attribute attribute = qwandaUtils.createButtonEvent(code, name);
			Question question = new Question(Prefix.QUE + code, name, attribute);
			Ask child = new Ask(question, sourceCode, targetCode);
			ask.add(child);
		}

		return ask;
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
	 * NOTE: This needs optimisation. Ultimately we would like
	 * to make use of a flat map, or tree map.
	 *
	 * @param asks   The ask to traverse
	 * @param target The target entity used in finding values
	 * @param asks   The msg to add entities to
	 */
	public void handleDropdownAttributes(Ask ask, String parentCode, BaseEntity target, QBulkMessage msg) {

		String questionCode = ask.getQuestion().getCode();
		log.info("Ask: " + questionCode + ", parentCode: " + parentCode);

		// recursion
		if (ask.hasChildren()) {
			for (Ask child : ask.getChildAsks())
				handleDropdownAttributes(child, questionCode, target, msg);
		}

		// check for dropdown attribute
		if (ask.getQuestion().getAttribute().getCode().startsWith(Prefix.LNK)) {

			// get list of value codes
			List<String> codes = beUtils.getBaseEntityCodeArrayFromLinkAttribute(target,
					ask.getQuestion().getAttribute().getCode());

			if (codes == null || codes.isEmpty())
				sendDropdownItems(ask, target, parentCode);
			else
				collectSelections(codes, msg);
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
	 * @param ask      The Ask to traverse
	 * @param target   The target entity used in processing
	 * @param rootCode The code of the root question used in sending DD messages
	 */
	public void sendDropdownItems(Ask ask, BaseEntity target, String parentCode) {

		Question question = ask.getQuestion();
		Attribute attribute = question.getAttribute();

		if (attribute.getCode().startsWith(Prefix.LNK)) {

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
					selection = beUtils.privacyFilter(selection,
							Collections.singleton(Attribute.PRI_NAME));
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
							.add("parentCode", parentCode)
							.add("value", "")
							.add("processId", ask.getProcessId()))
					.add("attributeCode", attribute.getCode())
					.add("token", userToken.getToken())
					.build();

			KafkaUtils.writeMsg(KafkaTopic.EVENTS, json.toString());
		}
	}

	/**
	 * @param msg
	 */
	public void sendData(QBulkMessage msg) {

		if (!msg.getEntities().isEmpty())
			sendBaseEntities(msg.getEntities());
		else
			log.error("No BEs to send!");

		if (!msg.getAsks().isEmpty())
			sendAsks(msg.getAsks());
		else
			log.error("No Asks to send!");
	}

	/**
	 * @param baseEntities
	 */
	public void sendBaseEntities(List<BaseEntity> baseEntities) {

		Map<String, Object> contexts = new HashMap<>();
		contexts.put("USER", beUtils.getUserBaseEntity());

		baseEntities.forEach(entity -> {
			MergeUtils.mergeBaseEntity(entity, contexts);
		});

		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(baseEntities);
		msg.setReplace(true);
		msg.setToken(userToken.getToken());

		KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
	}

	/**
	 * @param asks
	 */
	public void sendAsks(List<Ask> asks) {

		QDataAskMessage msg = new QDataAskMessage(asks);
		msg.setReplace(true);
		msg.setToken(userToken.getToken());

		KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
	}

}
