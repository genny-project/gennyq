package life.genny.kogito.common.core;

import static life.genny.kogito.common.utils.KogitoUtils.UseService.GADAQ;
import static life.genny.qwandaq.entity.PCM.PCM_TREE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import life.genny.qwandaq.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.kogito.common.service.TaskService;
import life.genny.kogito.common.utils.KogitoUtils;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.datatype.capability.core.CapabilitySet;
import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.Definition;
import life.genny.qwandaq.entity.PCM;
import life.genny.qwandaq.graphql.ProcessData;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.message.QBulkMessage;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;

/**
 * Dispatch
 */
@ApplicationScoped
public class Dispatch {

	public static final String[] BUTTON_EVENTS = { Attribute.EVT_SUBMIT, Attribute.EVT_NEXT, Attribute.EVT_UPDATE };

	Jsonb jsonb = JsonbBuilder.create();

	@Inject Logger log;

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
	EntityAttributeUtils beaUtils;
	@Inject
	SearchUtils search;
	@Inject
	KogitoUtils kogitoUtils;

	@Inject
	TaskService tasks;

	@Inject
	MergeUtils mergeUtils;

	/**
	 * Send Asks, PCMs and Searches
	 *
	 * @param processData
	 */
	public QBulkMessage build(ProcessData processData) {
		// fetch source and target entities
		String productCode = userToken.getProductCode();
		String sourceCode = processData.getSourceCode();
		String targetCode = processData.getTargetCode();
		BaseEntity source = beUtils.getBaseEntity(productCode, sourceCode, true);
		BaseEntity target = beUtils.getBaseEntity(productCode, targetCode, true);
		CapabilitySet userCapabilities = capMan.getUserCapabilities(target);

		PCM pcm = beUtils.getPCM(processData.getPcmCode());
		// ensure target codes match
		pcm.setTargetCode(targetCode);
		QBulkMessage msg = new QBulkMessage();
		msg.setTag("Dispatch:build:line93");
		// check for a provided question code
		String questionCode = processData.getQuestionCode();
		if (questionCode != null) {
			// fetch question from DB
			log.info("Generating asks -> " + questionCode + ":" + source.getCode() + ":" + target.getCode());
			Ask ask = qwandaUtils.generateAskFromQuestionCode(questionCode, source, target, userCapabilities, new ReqConfig());
			msg.add(ask);
		}
		// generate events if specified
		String buttonEvents = processData.getButtonEvents();
		if (buttonEvents != null) {
			Ask eventsAsk = createButtonEvents(buttonEvents, sourceCode, targetCode);
			msg.add(eventsAsk);
		}
		// init if null to stop null pointers
		if (processData.getAttributeCodes() == null) {
			processData.setAttributeCodes(new ArrayList<String>());
		}
		if (processData.getSearches() == null) {
			processData.setSearches(new ArrayList<String>());
		}
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
			log.debug("Updating " + parentPCM.getCode() + " : Location " + location + " -> " + processData.getPcmCode());
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
	public Boolean containsNonReadonly(Map<String, Ask> flatMapOfAsks) {
		for (Ask ask : flatMapOfAsks.values()) {
			if (!ask.getReadonly()) {
				return true;
			}
		}
		return false;
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
	public BaseEntity handleNonReadonly(ProcessData processData, Set<Ask> asks, Map<String, Ask> flatMapOfAsks,
			QBulkMessage msg) {
		// update all asks target and processId
		BaseEntity processEntity = qwandaUtils.generateProcessEntity(processData);
		String code = processEntity.getCode();
		String processId = processData.getProcessId();

		log.info("Target will be: " + code);

		// TODO: This should be done with the flat map, but it was causing issues
		for (Ask ask : asks) {
			qwandaUtils.recursivelySetInformation(ask, processId, code);
		}
		// check mandatory fields
		// TODO: change to use flatMap
		Boolean answered = qwandaUtils.mandatoryFieldsAreAnswered(flatMapOfAsks, processEntity);
		// pre-send ask updates
		Definition definition = beUtils.getDefinition(processData.getDefinitionCode());
		qwandaUtils.updateDependentAsks(processEntity, definition, flatMapOfAsks);
		// update any button Events
		for (String event : BUTTON_EVENTS) {
			Ask evt = flatMapOfAsks.get(event);
			if (evt != null)
				evt.setDisabled(!answered);
		}
		// this is ok since flatmap is referencing asks
		//msg.getAsks().addAll(asks);
		msg.setAsks(asks);
		// filter unwanted attributes
		log.debug("ProcessEntity contains " + processEntity.getBaseEntityAttributesMap().size() + " attributes");

		return processEntity;
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
		// check capability requirements are met
		CapabilitySet userCapabilities = capMan.getUserCapabilities(source);
		log.info("userCaps = " + userCapabilities);
		String pcmCode = pcm.getCode();
		if (!pcm.requirementsMet(userCapabilities)) {
			log.warn("User " + source.getCode() + " Capability requirements not met for pcm: " + pcmCode);
			return;
		}
		log.debug("Traversing " + pcmCode);

		// use pcm target if one is specified
		String productCode = pcm.getRealm();
		EntityAttribute targetAttribute = beaUtils.getEntityAttribute(productCode, pcmCode, Attribute.PRI_TARGET_CODE, false);
		if (targetAttribute != null) {
			String targetCode = targetAttribute.getValueString();
			if (!StringUtils.isEmpty(targetCode) && !targetCode.equals(target.getCode())) {
				// merge targetCode
				Map<String, Object> ctxMap = new HashMap<>();
				ctxMap.put("TARGET", target);
				targetCode = mergeUtils.merge(targetCode, ctxMap);
				// update targetCode so it does not re-trigger merging
				pcm.setTargetCode(targetCode);
				// providing a null parent & location since it is already set in the parent
				JsonObject payload = Json.createObjectBuilder()
						.add("sourceCode", source.getCode())
						.add("targetCode", targetCode)
						.add("pcmCode", pcmCode)
						.build();
				kogitoUtils.triggerWorkflow(GADAQ, "processQuestions", payload);
				return;
			}
		}

		// add pcm for sending
		msg.add(pcm);
		// iterate locations
		List<EntityAttribute> locations = beaUtils.getBaseEntityAttributesForBaseEntityWithAttributeCodePrefix(productCode, pcmCode, Prefix.LOCATION);
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

		// check for a question code
		EntityAttribute questionAttribute = beaUtils.getEntityAttribute(productCode, pcmCode, Attribute.PRI_QUESTION_CODE, false);
		if (questionAttribute == null) {
			log.warn("Question attribute is null for " + pcmCode);
		} else {
			String questionCode = questionAttribute.getValueString();
			if (questionCode == null) {
				log.warn("Question Code is null for " + pcmCode);
			} else if (!Question.QUE_EVENTS.equals(questionCode)) {
				// add ask to bulk message
				Ask ask = qwandaUtils.generateAskFromQuestionCode(questionCode, source, target, userCapabilities, new ReqConfig());
				msg.add(ask);
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
			String code = name.toUpperCase().replaceAll(" ", "_");
			// create child and add to ask
			Attribute attribute = qwandaUtils.createButtonEvent(code, name);
			Question question = new Question(Prefix.QUE + code, name, attribute);
			Ask child = new Ask(question, sourceCode, targetCode);
			ask.add(child);
		}

		return ask;
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

		// recursion
		if (ask.hasChildren()) {
			for (Ask child : ask.getChildAsks())
				handleDropdownAttributes(child, questionCode, target, msg);
		}

		// check for dropdown attribute
		if (ask.getQuestion().getAttributeCode().startsWith(Prefix.LNK)) {

			// get list of value codes
			List<String> codes = beUtils.getBaseEntityCodeArrayFromLinkAttribute(target,
					ask.getQuestion().getAttributeCode());

			if (codes == null || codes.isEmpty())
				sendDropdownItems(ask, target, parentCode);
			else
				collectSelections(codes, msg);
		}
	}

	/**
	 * Collect the already selected entities for a dropdown
	 * and add them to the bulk message.
	 *
	 * @param codes
	 * @param msg
	 */
	public void collectSelections(List<String> codes, QBulkMessage msg) {
		// we don't want to overwrite if the entity is already being sent
		Set<String> existing = msg.getEntities().stream()
				.map(e -> e.getCode())
				.collect(Collectors.toSet());
		// fetch entity for each and add to msg
		for (String code : codes) {
			if (existing.contains(code)) {
				continue;
			}
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
			log.debug("No Entities to send!");

		if (!msg.getAsks().isEmpty())
			sendAsks(msg.getAsks());
		else
			log.debug("No Asks to send!");
	}

	/**
	 * @param baseEntities
	 */
	public void sendBaseEntities(List<BaseEntity> baseEntities) {

		Map<String, Object> contexts = new HashMap<>();
		contexts.put("USER_CODE", beUtils.getUserBaseEntity());
		Attribute priName = cm.getAttribute(Attribute.PRI_NAME);

		baseEntities.stream().forEach(entity -> {
			EntityAttribute ea = beaUtils.getEntityAttribute(entity.getRealm(), entity.getCode(), Attribute.PRI_NAME);
			if (ea == null) {
				ea = new EntityAttribute(entity, priName, 1.0, entity.getName());
				beaUtils.updateEntityAttribute(ea);
			}
			entity.addAttribute(ea);
			mergeUtils.mergeBaseEntity(entity, contexts);
		});

		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(baseEntities);
		msg.setReplace(true);
		msg.setToken(userToken.getToken());

		KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
	}

	/**
	 * @param asks
	 */
	public void sendAsks(Set<Ask> asks) {

		QDataAskMessage msg = new QDataAskMessage(asks);
		msg.setReplace(true);
		msg.setToken(userToken.getToken());

		KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
	}

}
