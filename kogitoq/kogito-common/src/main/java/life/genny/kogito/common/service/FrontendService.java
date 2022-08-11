package life.genny.kogito.common.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.graphql.ProcessQuestions;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;

import org.apache.commons.lang3.StringUtils;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.DefUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;

@ApplicationScoped
public class FrontendService {

	private static final Logger log = Logger.getLogger(FrontendService.class);

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	DefUtils defUtils;

	@Inject
	NavigationService navigationService;

	public String getCurrentUserCode() {
		return userToken.getUserCode();
	}

	/**
	 * Create processData from inputs.
	 * 
	 * @param questionCode The code of the question to send
	 * @param sourceCode   The source user
	 * @param targetCode   The Target entity
	 * @param pcmCode      The code eof the PCM to use
	 * @return The processData json
	 */
	public String inputs(String questionCode, String sourceCode, String targetCode,
			String pcmCode, String events, String processId) {

		ProcessQuestions processData = new ProcessQuestions();
		processData.setQuestionCode(questionCode);
		processData.setSourceCode(sourceCode);
		processData.setTargetCode(targetCode);
		processData.setPcmCode(pcmCode);
		processData.setEvents(events);
		processData.setProcessId(processId);

		return jsonb.toJson(processData);
	}

	/**
	 * Get asks using a question code, for a given source and target.
	 *
	 * @param questionCode The question code used to fetch asks
	 * @param sourceCode   The code of the source entity
	 * @param targetCode   The code of the target entity
	 * @param processId    The processId to set in the asks
	 * @return The ask message
	 */
	public String getAsks(String processJson) {

		ProcessQuestions processData = jsonb.fromJson(processJson, ProcessQuestions.class);

		String questionCode = processData.getQuestionCode();
		String sourceCode = processData.getSourceCode();
		String targetCode = processData.getTargetCode();
		String processId = processData.getProcessId();

		log.info("==========================================");
		log.info("processId : " + processId);
		log.info("questionCode : " + questionCode);
		log.info("sourceCode : " + sourceCode);
		log.info("targetCode : " + targetCode);
		log.info("pcmCode : " + processData.getPcmCode());
		log.info("events : " + processData.getEvents());
		log.info("==========================================");

		BaseEntity source = beUtils.getBaseEntity(sourceCode);
		BaseEntity target = beUtils.getBaseEntity(targetCode);

		log.info("Fetching asks -> " + questionCode + ":" + source.getCode() + ":" + target.getCode());

		// fetch question from DB
		Ask ask = qwandaUtils.generateAskFromQuestionCode(questionCode, source, target);
		Ask events = createEvents(processData.getEvents(), sourceCode, targetCode);
		ask.addChildAsk(events);
		qwandaUtils.recursivelySetProcessId(ask, processId);

		// create ask msg from asks
		log.info("Creating ask Message...");
		QDataAskMessage msg = new QDataAskMessage(ask);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);
		processData.setAskMessage(msg);

		// put targetCode in cache
		// NOTE: This is mainly only necessary for initial dropdown items
		log.info("Caching targetCode " + processId + ":TARGET_CODE=" + targetCode);
		CacheUtils.putObject(userToken.getProductCode(), processId + ":TARGET_CODE", targetCode);

		return jsonb.toJson(processData);
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

		if (events == null)
			throw new NullParameterException("events");

		// fetch attributes and create group
		Attribute submit = qwandaUtils.getAttribute("EVT_SUBMIT");
		Attribute groupAttribute = qwandaUtils.getAttribute("QQQ_QUESTION_GROUP");
		Question groupQuestion = new Question("QUE_EVENTS", "", groupAttribute);

		// init ask
		Ask ask = new Ask(groupQuestion, sourceCode, targetCode);
		ask.setRealm(userToken.getProductCode());

		// split events string by comma
		for (String event : events.split(",")) {
			// create child and add to ask
			Attribute attribute = new Attribute("EVT_" + event, event, submit.getDataType());
			Question question = new Question("QUE_" + event, event, attribute);
			Ask child = new Ask(question, sourceCode, targetCode);
			ask.addChildAsk(child);
		}

		return ask;
	}

	/**
	 * Work out the DEF for the baseentity .
	 *
	 * @param targetCode The code of the target entity
	 * @return The DEF
	 */
	public String getDEF(String processJson) {

		ProcessQuestions processData = jsonb.fromJson(processJson, ProcessQuestions.class);
		String targetCode = processData.getTargetCode();

		// Find the DEF
		BaseEntity target = beUtils.getBaseEntity(targetCode);
		BaseEntity definition = defUtils.getDEF(target);
		log.info("ProcessBE identified as a " + definition);

		processData.setDefinitionCode(definition.getCode());

		return jsonb.toJson(processData);
	}

	/**
	 * Setup the process entity used to store task data.
	 *
	 * @param targetCode     The code of the target entity
	 * @param askMessageJson The ask message to use in setup
	 * @return The updated process entity
	 */
	public String setupProcessBE(String processJson) {

		ProcessQuestions processData = jsonb.fromJson(processJson, ProcessQuestions.class);
		String targetCode = processData.getTargetCode();
		QDataAskMessage askMessage = processData.getAskMessage();

		// init entity and force the realm
		String processEntityCode = "QBE_" + targetCode.substring(4);
		log.info("Creating Process Entity " + processEntityCode + "...");

		BaseEntity processEntity = new BaseEntity(processEntityCode, "QuestionBE");
		processEntity.setRealm(userToken.getProductCode());

		BaseEntity target = beUtils.getBaseEntity(targetCode);

		// find all allowed attribute codes
		Set<String> attributeCodes = new HashSet<>();
		for (Ask ask : askMessage.getItems()) {
			attributeCodes.addAll(qwandaUtils.recursivelyGetAttributeCodes(attributeCodes, ask));
		}

		log.info("Found " + attributeCodes.size() + " active attributes in asks");

		// add an entityAttribute to process entity for each attribute
		for (String code : attributeCodes) {

			// check for existing attribute in target
			EntityAttribute ea = target.findEntityAttribute(code).orElseGet(() -> {

				// otherwise create new attribute
				Attribute attribute = qwandaUtils.getAttribute(code);
				Object value = null;
				// default toggles to false
				if (attribute.getDataType().getComponent().equals("flag"))
					value = false;

				return new EntityAttribute(processEntity, attribute, 1.0, value);
			});

			processEntity.addAttribute(ea);
		}

		log.info("ProcessBE contains " + processEntity.getBaseEntityAttributes().size() + " entity attributes");
		processData.setProcessEntity(processEntity);

		// TODO, until cacheUtils supprots BEs
		CacheUtils.putObject(userToken.getProductCode(), processEntityCode, processEntity);

		return jsonb.toJson(processData);
	}

	/**
	 * Update the ask target to match the process entity code.
	 *
	 * @param processBEJson  The json of the process entity
	 * @param askMessageJson The ask message to use in setup
	 * @return The updated ask message
	 */
	public String updateAskTarget(String processJson) {

		ProcessQuestions processData = jsonb.fromJson(processJson, ProcessQuestions.class);

		BaseEntity processEntity = processData.getProcessEntity();
		QDataAskMessage askMessage = processData.getAskMessage();
		log.info("Updating Ask Target " + askMessage.getItems().get(0));

		recursivelyUpdateAskTarget(askMessage.getItems().get(0), processEntity);
		processData.setAskMessage(askMessage);

		return jsonb.toJson(processData);
	}

	/**
	 * Recursively update the ask target.
	 *
	 * @param ask    The ask to traverse
	 * @param target The target entity to set
	 */
	public void recursivelyUpdateAskTarget(Ask ask, BaseEntity target) {

		ask.setTargetCode(target.getCode());

		// recursively update children
		if (ask.getChildAsks() != null) {
			for (Ask child : ask.getChildAsks()) {
				recursivelyUpdateAskTarget(child, target);
			}
		}
	}

	/**
	 * Control main content navigation using a pcm and a question
	 */
	public void navigateContent(String processJson) {

		log.info("Navigating to form...");
		ProcessQuestions processData = jsonb.fromJson(processJson, ProcessQuestions.class);
		String pcmCode = processData.getPcmCode();
		String questionCode = processData.getQuestionCode();

		navigationService.navigateContent(pcmCode, questionCode);
	}

	/**
	 * Send a baseentity after filtering the entity attributes
	 * based on the questions in the ask message.
	 *
	 * @param code      The code of the baseentity to send
	 * @param askMsg    The ask message used to filter attributes
	 * @param processId The process id to use for the baseentity cache
	 * @param defCode   . The type of processBE (to save calculating it again)
	 */
	public void sendBaseEntitys(String processJson) {

		ProcessQuestions processData = jsonb.fromJson(processJson, ProcessQuestions.class);
		BaseEntity processEntity = processData.getProcessEntity();
		QDataAskMessage askMessage = processData.getAskMessage();
		String processId = processData.getProcessId();

		// find all allowed attribute codes
		Set<String> attributeCodes = new HashSet<>();
		for (Ask ask : askMessage.getItems()) {
			attributeCodes.addAll(qwandaUtils.recursivelyGetAttributeCodes(attributeCodes, ask));
		}

		// grab all entityAttributes from the entity
		Set<EntityAttribute> entityAttributes = ConcurrentHashMap
				.newKeySet(processEntity.getBaseEntityAttributes().size());
		for (EntityAttribute ea : processEntity.getBaseEntityAttributes()) {
			entityAttributes.add(ea);
		}

		// delete any attribute that is not in the allowed Set
		for (EntityAttribute ea : entityAttributes) {
			if (!attributeCodes.contains(ea.getAttributeCode())) {
				processEntity.removeAttribute(ea.getAttributeCode());
			}
		}

		// send entity front end
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage();
		msg.add(processEntity);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);
		msg.setTotal(Long.valueOf(msg.getItems().size()));
		msg.setTag("SendBaseEntities");

		log.info("Sending " + processEntity.getBaseEntityAttributes().size() + " processBE attributes");

		// check cache first
		String key = String.format("%s:PROCESS_DATA", processId);
		CacheUtils.putObject(userToken.getProductCode(), key, processData);
		log.infof("processData cached to %s", key);

		// NOTE: only using first ask item
		Ask ask = askMessage.getItems().get(0);

		// handle initial dropdown selections
		recuresivelyFindAndSendDropdownItems(ask, processEntity, ask.getQuestion().getCode());

		KafkaUtils.writeMsg("webcmds", jsonb.toJson(msg));
	}

	/**
	 * Recursively traverse the ask to find any already selected dropdown
	 * items to send, and trigger dropdown searches.
	 *
	 * @param ask      The Ask to traverse
	 * @param target   The target entity used in processing
	 * @param rootCode The code of the root question used in sending DD messages
	 */
	public void recuresivelyFindAndSendDropdownItems(Ask ask, BaseEntity target, String rootCode) {

		Question question = ask.getQuestion();
		Attribute attribute = question.getAttribute();
		Attribute nameAttribute = qwandaUtils.getAttribute("PRI_NAME");

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
					if ((code.startsWith("{startDate")) || (code.startsWith("endDate"))) {
						log.error(
								"BE:" + target.getCode() + ":attribute :" + attribute.getCode() + ":BAD code " + code);
						continue;
					}
					BaseEntity selection = null;
					try {
						selection = beUtils.getBaseEntity(code);
					} catch (ItemNotFoundException e) {
						log.error(
								code + " IS NOT IN DATABASE , but present in target " + target.getCode() + " attribute "
										+ attribute.getCode());
						// throw new ItemNotFoundException(code);
						continue;
					}
					if (selection == null) {
						log.error(
								code + " IS NOT IN DATABASE , but present in target " + target.getCode() + " attribute "
										+ attribute.getCode());
						// throw new ItemNotFoundException(code);
						continue;
					}

					// Ensure only the PRI_NAME attribute exists in the selection
					selection = beUtils.addNonLiteralAttributes(selection);
					selection = beUtils.privacyFilter(selection, Collections.singletonList("PRI_NAME"));
					selectionMsg.add(selection);
				}

				// send selections
				if (selectionMsg.getItems() != null) {
					selectionMsg.setToken(userToken.getToken());
					selectionMsg.setReplace(true);
					log.info("Sending selection items with " + selectionMsg.getItems().size() + " items");
					KafkaUtils.writeMsg("webdata", selectionMsg);
				} else {
					log.info("No selection items found for " + attribute.getCode());
				}
			}

			// trigger dropdown search in dropkick
			JsonObject json = Json.createObjectBuilder()
					.add("event_type", "DD")
					.add("data", Json.createObjectBuilder()
							.add("parentCode", rootCode)
							.add("questionCode", question.getCode())
							.add("sourceCode", ask.getSourceCode())
							.add("targetCode", ask.getTargetCode())
							.add("value", "")
							.add("processId", ask.getProcessId()))
					.add("attributeCode", attribute.getCode())
					.add("token", userToken.getToken())
					.build();

			KafkaUtils.writeMsg("events", json.toString());
		}

		// recursively run on children
		if ((ask.getChildAsks() != null) && (ask.getChildAsks().length > 0)) {
			for (Ask child : ask.getChildAsks()) {
				recuresivelyFindAndSendDropdownItems(child, target, rootCode);
			}
		}
	}

	/**
	 * Send an ask message to the frontend through the webdata topic.
	 *
	 * @param askMsgJson The ask message to send
	 */
	public void sendQDataAskMessage(String processJson) {

		ProcessQuestions processData = jsonb.fromJson(processJson, ProcessQuestions.class);
		QDataAskMessage askMessage = processData.getAskMessage();

		BaseEntity processEntity = processData.getProcessEntity();

		// NOTE: We only ever check the first ask in the message
		Ask ask = askMessage.getItems().get(0);

		Boolean answered = qwandaUtils.mandatoryFieldsAreAnswered(ask, processEntity);

		log.info("Mandatory fields are " + (answered ? "answered" : "not answered"));

		ask = qwandaUtils.recursivelyFindAndUpdateSubmitDisabled(ask, !answered);
		askMessage.getItems().set(0, ask);
		askMessage.setToken(userToken.getToken());
		askMessage.setTag("sendQDataAskMessage");
		KafkaUtils.writeMsg("webcmds", askMessage);
	}

	/**
	 * Send dropdown items for the asks.
	 *
	 * @param askMsgJson The ask message to send
	 */
	public void sendDropdownItems(String processJson) {

		ProcessQuestions processData = jsonb.fromJson(processJson, ProcessQuestions.class);
		QDataAskMessage askMessage = processData.getAskMessage();

		// NOTE: We only ever check the first ask in the message
		Ask ask = askMessage.getItems().get(0);
		BaseEntity target = beUtils.getBaseEntity(ask.getTargetCode());

		QDataBaseEntityMessage msg = new QDataBaseEntityMessage();
		recursivelyHandleDropdownAttributes(ask, target, msg);
		msg.setTag("SendDropDownItems");

		KafkaUtils.writeMsg("webdata", msg);
	}

	/**
	 * Recursively traverse the asks and add any entity selections to the msg.
	 *
	 * @param ask    The ask to traverse
	 * @param target The target entity used in finding values
	 * @param ask    The msg to add entities to
	 */
	public void recursivelyHandleDropdownAttributes(Ask ask, BaseEntity target, QDataBaseEntityMessage msg) {

		// recursively handle any child asks
		if (ask.getChildAsks() != null) {
			for (Ask child : ask.getChildAsks()) {
				recursivelyHandleDropdownAttributes(child, target, msg);
			}
		}

		// check for dropdown attribute
		if (ask.getAttributeCode().startsWith("LNK_")) {

			// get list of value codes
			List<String> codes = beUtils.getBaseEntityCodeArrayFromLinkAttribute(target, ask.getAttributeCode());

			// fetch entity for each and add to msg
			for (String code : codes) {
				BaseEntity be = beUtils.getBaseEntityOrNull(code);

				if (be != null) {
					msg.add(be);
				}
			}
		}
	}
}
