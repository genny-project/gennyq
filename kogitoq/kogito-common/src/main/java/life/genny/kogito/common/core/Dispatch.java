package life.genny.kogito.common.core;

import static life.genny.kogito.common.utils.KogitoUtils.UseService.GADAQ;
import static life.genny.qwandaq.entity.PCM.PCM_TREE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

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
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.graphql.ProcessData;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.message.QBulkMessage;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.AttributeUtils;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.EntityAttributeUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.MergeUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.utils.SearchUtils;

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

	@Inject
	AttributeUtils attributeUtils;

	private Set<String> processedTargetCodes = new HashSet<>();

	private Set<String> traversedPCMs = new HashSet<>();

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
	 */
	public QBulkMessage build(ProcessData processData, PCM pcm) {
		// fetch source and target entities
		String productCode = userToken.getProductCode();
		String sourceCode = processData.getSourceCode();
		String targetCode = processData.getTargetCode();
		BaseEntity source = beUtils.getBaseEntity(productCode, sourceCode, false);
		BaseEntity target = beUtils.getBaseEntity(productCode, targetCode, false);
		CapabilitySet userCapabilities = capMan.getUserCapabilities(source);

		pcm = (pcm == null ? beUtils.getPCM(processData.getPcmCode()) : pcm);
		// ensure target codes match
		pcm.setTargetCode(targetCode);
		QBulkMessage msg = new QBulkMessage();
		// generate events if specified
		String buttonEvents = processData.getButtonEvents();
		if (buttonEvents != null) {
			Ask eventsAsk = createButtonEvents(buttonEvents, sourceCode, targetCode);
			msg.add(eventsAsk);
		}
		// init if null to stop null pointers
		// TODO: This should be moved into the constructor of ProcessData if this is a problem
		if (processData.getAttributeCodes() == null) {
			processData.setAttributeCodes(new ArrayList<>());
		}
		if (processData.getSearches() == null) {
			processData.setSearches(new ArrayList<>());
		}

		String parent = processData.getParent();
		String location = processData.getLocation();
		// traverse pcm to build data
		processedTargetCodes.clear();
		traversedPCMs.clear();
		traversePCM(userCapabilities, pcm, source, target, parent, location, msg, processData);

		/**
		 * update parent pcm
		 * NOTE: the null check protects from cases where
		 * dispatch is called with a null parent and location.
		 * This occurs when triggering dispatch for a pcm
		 * that requires a different target code.
		 */
		if (parent != null && !PCM_TREE.equals(parent)) {
			PCM parentPCM = beUtils.getPCM(parent);
			Integer loc = PCM.findLocation(location);
			log.debug("Updating " + parentPCM.getCode() + " : Location " + loc + " -> " + processData.getPcmCode());
			parentPCM.setLocation(loc, processData.getPcmCode());
			msg.add(parentPCM);
		}

		return msg;
	}

	/**
	 * @param flatMapOfAsks
	 * @return True if the map contains a non-readonly ask, False otherwise
	 */
	public boolean containsNonReadonly(Map<String, Ask> flatMapOfAsks) {
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

		log.debug("Target will be: " + code);

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
			if (evt != null) {
				evt.setDisabled(!answered);
			}
		}
		sendButtonEvents(asks);

		// this is ok since flatmap is referencing asks
		msg.setAsks(asks);
		// filter unwanted attributes
		log.debug("ProcessEntity contains " + processEntity.getBaseEntityAttributesMap().size() + " attributes");

		return processEntity;
	}

	/**
	 * Send the button events
	 * @param asks
	 */
	public void sendButtonEvents(Set<Ask> asks) {
		for (Ask ask : asks) {
			if (Question.QUE_EVENTS.equals(ask.getQuestionCode())) {
				QDataAskMessage msg = new QDataAskMessage(ask);
				msg.setReplace(true);
				msg.setToken(userToken.getToken());
				KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
			}
		}
	}

	/**
	 * Traverse a PCM looking for a non-readonly question.
	 *
	 * @param pcm The PCM to Traverse
	 * @param source The source baseEntity
	 * @param target The target baseEntity
	 * @param msg The bulk message to store data
	 * @param processData The ProcessData used to init the task
	 */
	public boolean traversePCM(CapabilitySet userCapabilities, PCM pcm, BaseEntity source, BaseEntity target,
							String parent, String location, QBulkMessage msg, ProcessData processData) {
		// TODO: This is too many arguments

		// check capability requirements are met
		String pcmCode = pcm.getCode();
		log.debug("Traversing " + pcmCode);
		if (!pcm.requirementsMet(userCapabilities)) {
			log.debug("User " + source.getCode() + " Capability requirements not met for pcm: " + pcmCode);
			return false;
		}

		// use pcm target if one is specified
		try {
			String targetCode = beaUtils.getValue(pcm, Attribute.PRI_TARGET_CODE);
            log.debugf("pcmCode: %s, sourceCode: %s,  targetCode: %s, target.getCode(): %s", pcmCode, source.getCode(), targetCode, target.getCode());

			if (!StringUtils.isBlank(targetCode) && !targetCode.equals(target.getCode()) && !processedTargetCodes.contains(targetCode+target.getCode())) {
				// merge targetCode
				log.debugf("Target code combo already processed? : %s", processedTargetCodes.contains(targetCode + target.getCode()));
				Map<String, Object> ctxMap = new HashMap<>();
				ctxMap.put("TARGET", target);
				targetCode = mergeUtils.merge(targetCode, ctxMap);
				// update targetCode so it does not re-trigger merging
				JsonObject payload = Json.createObjectBuilder()
						.add("sourceCode", source.getCode())
						.add("targetCode", targetCode)
						.add("pcmCode", pcmCode)
						.add("parent", parent)
						.add("location", location)
						.build();
				kogitoUtils.triggerWorkflow(GADAQ, "processQuestions", payload);
				processedTargetCodes.add(targetCode+target.getCode());
				return true;
			}
		} catch (ItemNotFoundException e) {
			log.trace("No target code found for " + pcm.getCode());
		}

		Set<Map.Entry<String, EntityAttribute>> pcmAttributeMap = pcm.getBaseEntityAttributesMap().entrySet();
		pcmAttributeMap.removeIf(loc -> {
			if(!loc.getValue().requirementsMet(userCapabilities))
				return true;
			String value = loc.getValue().getAsString();
			if(!value.startsWith(Prefix.PCM_)) {
				return false;
			}
			// check the base entity as well
			PCM childPCM = beUtils.getPCM(value);

			if(!childPCM.requirementsMet(userCapabilities)) {
				log.debug("PCM capability requirements not met for location: " + loc.getKey() + " (" + loc.getValue().getValueString() + ")");
				return true;
			}
			return false;
		});
		
		/**
		 * Iterate through all entity attributes
		 * mail merge required fields
		 * if attribute code does not refer to a location do not traverse / do further processing
		 */
		for(Map.Entry<String, EntityAttribute> entityAttributeEntry : pcmAttributeMap) {
			EntityAttribute entityAttribute = entityAttributeEntry.getValue();
		// }
		// List<EntityAttribute> locations = pcmAttributeMap.stream().filter(val -> val.getKey().startsWith(Prefix.PRI_LOC)).map(Map.Entry::getValue).collect(Collectors.toList());
		// for(EntityAttribute entityAttribute : locations) {
			log.debug("Passed Capabilities check for: " + entityAttribute.getBaseEntityCode() + ":" + entityAttribute.getAttributeCode());

			String value = entityAttribute.getValueString();
			// mail merging
			if(value.contains("[[")) {
				BaseEntity user = beUtils.getUserBaseEntity();
				Map<String, Object> ctxMap = Map.of("USER", user, "USER_CODE", user);
				value = mergeUtils.merge(pcmCode, ctxMap);
				entityAttribute.setValueString(value);
			}

			// Strict location processing past this point
			if(!entityAttribute.getAttributeCode().startsWith("PRI_LOC"))
				continue;


			Attribute attribute = attributeUtils.getAttribute(entityAttribute.getRealm(), entityAttribute.getAttributeCode(), true);
			entityAttribute.setAttribute(attribute);
			value = entityAttribute.getAsString();

			// recursively check PCM fields
			if (value.startsWith(Prefix.PCM_)) {
				parent = pcmCode;
				location = entityAttribute.getAttributeCode();
				if (traversedPCMs.contains(value)) {
					log.debugf("The PCM %s already traversed for parent %s, skipping traversal.", value, processData.getParent());
				} else {
					traversedPCMs.add(value);
					PCM childPcm = beUtils.getPCM(value);
					traversePCM(userCapabilities, childPcm, source, target, parent, location, msg, processData);
				}
			} else if (value.startsWith(Prefix.SBE_)) {
				processData.getSearches().add(value);
			}
		}

		msg.add(pcm);

		// check for a question code
		String questionCode = null;
		try {
			questionCode = beaUtils.getValue(pcm, Attribute.PRI_QUESTION_CODE);
		} catch (ItemNotFoundException e) {
			log.warn("Question Code is null for " + pcmCode + ". Checking ProcessData");
		}
		if (!Question.QUE_EVENTS.equals(questionCode) && !StringUtils.isBlank(questionCode)) {
			// add ask to bulk message
			Ask ask = qwandaUtils.generateAskFromQuestionCode(questionCode, source, target, userCapabilities, new ReqConfig());
			ask.setTestTargetCode(target.getCode());
			msg.add(ask);
		}

		return true;
	}

	/**
	 * Create the events ask group.
	 * 
	 * @param buttonEvents     The events string
	 * @param sourceCode The source entity code
	 * @param targetCode The target entity code
	 * @return The events ask group
	 */
	public Ask createButtonEvents(String buttonEvents, String sourceCode, String targetCode) {

		// fetch attributes and create group
		Attribute groupAttribute = attributeUtils.getAttribute(Attribute.QQQ_QUESTION_GROUP, true);
		Question groupQuestion = new Question(Question.QUE_EVENTS, "", groupAttribute);

		// init ask
		Ask ask = new Ask(groupQuestion, sourceCode, targetCode);
		ask.setRealm(userToken.getProductCode());

		// split events string by comma
		for (String name : buttonEvents.split(",")) {
			String code = name.toUpperCase().replaceAll(" ", "_");
			// create child and add to ask
			Attribute attribute = qwandaUtils.createButtonEvent(code, name);
			Question question = new Question(Prefix.QUE_ + code, name, attribute);
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
	 * @param ask   The ask to traverse
	 * @param target The target entity used in finding values
	 * @param msg   The msg to add entities to
	 */
	public void handleDropdownAttributes(Ask ask, String parentCode, BaseEntity target, QBulkMessage msg) {

		String questionCode = ask.getQuestion().getCode();

		// recursion
		if (ask.hasChildren()) {
			for (Ask child : ask.getChildAsks())
				handleDropdownAttributes(child, questionCode, target, msg);
		}

		// check for dropdown attribute
		if (ask.getQuestion().getAttribute().getCode().startsWith(Prefix.LNK_)) {

			List<String> codes = new ArrayList<>(0);
			if (target.getCode().startsWith(Prefix.QBE_)) {
				// get list from target
				String value = target.getValueAsString(ask.getQuestion().getAttributeCode());
				if (value != null) {
					codes = Arrays.asList(CommonUtils.getArrayFromString(value));
				}
			} else {
				// get list of value codes from cache
				codes = beUtils.getBaseEntityCodeArrayFromLinkAttribute(target,
						ask.getQuestion().getAttributeCode());
			}

			if (codes.isEmpty())
				sendDropdownItems(ask, target, parentCode);
			else
				collectSelections(codes, msg);
		}
	}

	/**
	 * Collect the already selected entities for a dropdown
	 * and add them to the bulk message.
	 *
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

			if(!code.isEmpty()) {
				BaseEntity be = beUtils.getBaseEntity(code);
				msg.add(be);
			}
		}
	}

	/**
	 * Recursively traverse the ask to find any already selected dropdown
	 * items to send, and trigger dropdown searches.
	 *
	 * @param ask      The Ask to traverse
	 * @param target   The target entity used in processing
	 * @param parentCode The code of the root question used in sending DD messages
	 */
	public void sendDropdownItems(Ask ask, BaseEntity target, String parentCode) {

		Question question = ask.getQuestion();
		Attribute attribute = question.getAttribute();

		if (attribute.getCode().startsWith(Prefix.LNK_)) {

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

		Attribute priName = attributeUtils.getAttribute(Attribute.PRI_NAME, true);

		baseEntities.stream()
				.filter(b -> !b.getCode().startsWith(Prefix.QBE_))
				.forEach(entity -> {
					entity.addAttribute(new EntityAttribute(entity, priName, 1.0, entity.getName()));
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
