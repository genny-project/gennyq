package life.genny.kogito.common.service;

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
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.ProcessBeAndDef;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.serialization.baseentity.BaseEntityKey;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import org.apache.commons.lang3.StringUtils;
import life.genny.qwandaq.utils.DefUtils;
import org.jboss.logging.Logger;

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

	public String getCurrentUserCode() {
		return userToken.getUserCode();
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
	public String getAsks(String questionCode, String sourceCode, String targetCode, String processId) {

		log.info("questionCode :" + questionCode);
		log.info("sourceCode :" + sourceCode);
		log.info("targetCode :" + targetCode);
		log.info("processId :" + processId);

		BaseEntity source = beUtils.getBaseEntityByCode(sourceCode);

		BaseEntity target = null;
		if ("NON_EXISTENT".equals(targetCode)) {
			target = new BaseEntity(targetCode, targetCode);
		} else {
			target = beUtils.getBaseEntityByCode(targetCode);
		}

		if (source == null) {
			log.error("No Source entity found!");
			return null;
		}

		if (target == null) {
			log.error("No Target entity found!");
			return null;
		}

		log.info("Fetching asks -> " + questionCode + ":" + source.getCode() + ":" + target.getCode());

		// fetch question from DB
		Ask ask = qwandaUtils.generateAskFromQuestionCode(questionCode, source, target);

		if (ask == null) {
			log.error("No ask returned for " + questionCode);
			return null;
		}

		qwandaUtils.recursivelySetProcessId(ask, processId);

		// create ask msg from asks
		log.info("Creating ask Message...");
		QDataAskMessage msg = new QDataAskMessage(ask);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);

		// put targetCode in cache
		// NOTE: This is mainly only necessary for initial dropdown items
		log.info("Caching targetCode " + processId + ":TARGET_CODE=" + targetCode);
		CacheUtils.putObject(userToken.getProductCode(), processId + ":TARGET_CODE", targetCode);

		return jsonb.toJson(msg);
	}

	/**
	 * Work out the DEF for the baseentity .
	 *
	 * @param targetCode The code of the target entity
	 * @return The DEF
	 */
	public String getDEF(String targetCode) {

		if (targetCode == null) {
			log.error("TargetCode must not be null!");
			return null;
		}

		if ("NON_EXISTENT".equals(targetCode)) {
			return null;
		}

		// Find the DEF
		BaseEntity target = beUtils.getBaseEntityByCode(targetCode);
		BaseEntity defBE = defUtils.getDEF(target);
		log.info("ProcessBE identified as a " + defBE);

		return defBE.getCode();
	}

	/**
	 * Setup the process entity used to store task data.
	 *
	 * @param targetCode     The code of the target entity
	 * @param askMessageJson The ask message to use in setup
	 * @return The updated process entity
	 */
	public String setupProcessBE(String targetCode, String askMessageJson) {

		if (askMessageJson == null) {
			log.error("Ask message Json must not be null!");
			return null;
		}

		QDataAskMessage askMsg = jsonb.fromJson(askMessageJson, QDataAskMessage.class);

		// init entity and force the realm
		log.info("Creating Process Entity...");
		BaseEntity processBE = new BaseEntity("QBE_" + targetCode.substring(4), "QuestionBE");
		processBE.setRealm(userToken.getProductCode());

		// only copy the entityAttributes used in the Asks
		BaseEntity target = null;
		if ("NON_EXISTENT".equals(targetCode)) {
			target = new BaseEntity(targetCode, targetCode);
		} else {
			target = beUtils.getBaseEntityByCode(targetCode);
		}

		// find all allowed attribute codes
		Set<String> attributeCodes = new HashSet<>();
		for (Ask ask : askMsg.getItems()) {
			attributeCodes.addAll(qwandaUtils.recursivelyGetAttributeCodes(attributeCodes, ask));
		}

		log.info("Found " + attributeCodes.size() + " active attributes in asks");

		// add an entityAttribute to process entity for each attribute
		for (String code : attributeCodes) {

			// check for existing attribute in target
			EntityAttribute ea = target.findEntityAttribute(code).orElseGet(() -> {

				// otherwise create new attribute
				Attribute attribute = qwandaUtils.getAttribute(code);
				return new EntityAttribute(processBE, attribute, 1.0, null);
			});

			try {
				processBE.addAttribute(ea);
			} catch (BadDataException e) {
				e.printStackTrace();
			}
		}

		log.info("ProcessBE contains " + processBE.getBaseEntityAttributes().size() + " entity attributes");

		return jsonb.toJson(processBE);
	}

	/**
	 * Update the ask target to match the process entity code.
	 *
	 * @param processBEJson  The json of the process entity
	 * @param askMessageJson The ask message to use in setup
	 * @return The updated ask message
	 */
	public String updateAskTarget(String processBEJson, String askMessageJson) {

		if (processBEJson == null) {
			log.error("Process Entity json must not be null!");
			return null;
		}

		if (askMessageJson == null) {
			log.error("Ask Message json must not be null!");
			return null;
		}

		BaseEntity processBE = jsonb.fromJson(processBEJson, BaseEntity.class);
		QDataAskMessage askMsg = jsonb.fromJson(askMessageJson, QDataAskMessage.class);

		recursivelyUpdateAskTarget(askMsg.getItems().get(0), processBE);

		return jsonb.toJson(askMsg);
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
	 * Send a baseentity after filtering the entity attributes
	 * based on the questions in the ask message.
	 *
	 * @param code      The code of the baseentity to send
	 * @param askMsg    The ask message used to filter attributes
	 * @param processId The process id to use for the baseentity cache
	 * @param defCode   . The type of processBE (to save calculating it again)
	 */
	public void sendBaseEntitys(String processBEJson, String askMessageJson, String processId, String defCode) {

		BaseEntity processBE = null;
		QDataAskMessage askMsg = null;

		try {
			processBE = jsonb.fromJson(processBEJson, BaseEntity.class);
		} catch (java.lang.NullPointerException e) {
			log.error("Process Entity json must not be null or have null entry! -> " + processBEJson);
			return;
		}
		try {
			askMsg = jsonb.fromJson(askMessageJson, QDataAskMessage.class);
		} catch (java.lang.NullPointerException e) {
			log.error("Ask json must not be null or have null entry! -> " + askMessageJson);
			return;
		}

		// find all allowed attribute codes
		Set<String> attributeCodes = new HashSet<>();
		for (Ask ask : askMsg.getItems()) {
			attributeCodes.addAll(qwandaUtils.recursivelyGetAttributeCodes(attributeCodes, ask));
		}

		// grab all entityAttributes from the entity
		Set<EntityAttribute> entityAttributes = ConcurrentHashMap.newKeySet(processBE.getBaseEntityAttributes().size());
		for (EntityAttribute ea : processBE.getBaseEntityAttributes()) {
			entityAttributes.add(ea);
		}

		// delete any attribute that is not in the allowed Set
		for (EntityAttribute ea : entityAttributes) {
			if (!attributeCodes.contains(ea.getAttributeCode())) {
				processBE.removeAttribute(ea.getAttributeCode());
			}
		}

		// send entity front end
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage();
		msg.add(processBE);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);
		msg.setTotal(Long.valueOf(msg.getItems().size()));
		msg.setTag("SendBaseEntities");
		// Sending the BE here has issues with dropdown items...

		// Now save the processBE into cache so that the lauchy and dropkick can
		// recognise it as valid
		// Now update the cached version of the processBE with an expiry (used in
		// dropkick and lauchy)
		// cache the current ProcessBE so that it can be used quickly by lauchy etc
		ProcessBeAndDef processBeAndDef = new ProcessBeAndDef(processBE, defCode);
		String processBeAndDefJson = jsonb.toJson(processBeAndDef);
		CacheUtils.putObject(userToken.getProductCode(), processId + ":PROCESS_BE", processBeAndDefJson);

		log.info("processBE cached to " + processId + ":PROCESS_BE");

		// NOTE: only using first ask item
		Ask ask = askMsg.getItems().get(0);

		// handle initial dropdown selections
		recuresivelyFindAndSendDropdownItems(ask, processBE, ask.getQuestion().getCode());

		// Now send the baseentity to the Frontend so that the 'menu' are already there
		// waiting
		KafkaUtils.writeMsg("webdata", msg);
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

		if (attribute.getCode().startsWith("LNK_")) {

			// check for already selected items
			List<String> codes = beUtils.getBaseEntityCodeArrayFromLinkAttribute(target, attribute.getCode());
			if (codes != null && !codes.isEmpty()) {

				// grab selection baseentitys
				QDataBaseEntityMessage selectionMsg = new QDataBaseEntityMessage();
				for (String code : codes) {
					if (StringUtils.isBlank(code)) {
						log.error("One of the LNKs for target are null");
						continue;
					}
					 BaseEntity selection = beUtils.getBaseEntityByCode(code);
					BaseEntityKey key = new BaseEntityKey(userToken.getProductCode(), code);
					//BaseEntity selection = (BaseEntity) CacheUtils.getEntity(GennyConstants.CACHE_NAME_BASEENTITY, key);
					if (selection != null) {
						selection.setBaseEntityAttributes(new HashSet<EntityAttribute>());
						// log.info("Selected attribute and value:" + code + ":" + selection.getValue(code) + " with "
						// 		+ selection.getBaseEntityAttributes().size() + " attributes");
						// if (selection.getBaseEntityAttributes().size() > 1) {
						// 			for (EntityAttribute ea : selection.getBaseEntityAttributes()) {
						// 				log.info("MULTIPLE ATTRIBUTES:"+selection.getCode()+" "+ea.getAttributeCode()+"="+ea.getValue());
						// 			}
						// 		}
						log.info("Sending the selected BaseEntity "+selection.getCode()+":"+selection.getName());
						selectionMsg.add(selection);
					}
				}

				// send selections
				selectionMsg.setToken(userToken.getToken());
				selectionMsg.setReplace(true);
				log.info("SENDING BASEENTITYS FOR SELECTIONS!");
				KafkaUtils.writeMsg("webdata", selectionMsg);
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
		if (ask.getChildAsks() != null) {
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
	public void sendQDataAskMessage(String askMessageJson, String processBEJson) {

		BaseEntity processBE = jsonb.fromJson(processBEJson, BaseEntity.class);
		QDataAskMessage askMessage = jsonb.fromJson(askMessageJson, QDataAskMessage.class);

		// NOTE: We only ever check the first ask in the message
		Ask ask = askMessage.getItems().get(0);

		Boolean answered = qwandaUtils.mandatoryFieldsAreAnswered(ask, processBE);
		ask = qwandaUtils.recursivelyFindAndUpdateSubmitDisabled(ask, !answered);
		askMessage.getItems().set(0, ask);

		askMessage.setToken(userToken.getToken());
		KafkaUtils.writeMsg("webcmds", askMessage);
	}

	/**
	 * Send dropdown items for the asks.
	 *
	 * @param askMsgJson The ask message to send
	 */
	public void sendDropdownItems(String askMessageJson) {

		QDataAskMessage askMessage = jsonb.fromJson(askMessageJson, QDataAskMessage.class);

		// NOTE: We only ever check the first ask in the message
		Ask ask = askMessage.getItems().get(0);
		BaseEntity target = beUtils.getBaseEntityByCode(ask.getTargetCode());

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
				BaseEntity be = beUtils.getBaseEntityByCode(code);

				if (be != null) {
					msg.add(be);
				}
			}
		}
	}
}
