package life.genny.kogito.common.service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
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
import life.genny.qwandaq.graphql.ProcessData;
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

	private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

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

	/**
	 * Get asks using a question code, for a given source and target.
	 *
	 * @param questionCode The question code used to fetch asks
	 * @param sourceCode   The code of the source entity
	 * @param targetCode   The code of the target entity
	 * @param processId    The processId to set in the asks
	 * @return The ask message
	 */
	public void sendAsks(ProcessData processData) {

		// ProcessData processData = jsonb.fromJson(processJson, ProcessData.class);
		String processId = processData.getProcessId();
		String questionCode = processData.getQuestionCode();

		String key = String.format("%s:%s", processId, questionCode);
		Ask ask = CacheUtils.getObject(userToken.getProductCode(), key, Ask.class);

		String targetCode = processData.getTargetCode();

		// update ask target
		BaseEntity processEntity = processData.getProcessEntity();
		recursivelyUpdateAskTarget(ask, processEntity);

		// put targetCode in cache
		// NOTE: This is mainly only necessary for initial dropdown items
		log.info("Caching targetCode " + processId + ":TARGET_CODE=" + targetCode);
		CacheUtils.putObject(userToken.getProductCode(), processId + ":TARGET_CODE", targetCode);

		// check mandatories and update submit
		Boolean answered = qwandaUtils.mandatoryFieldsAreAnswered(ask, processEntity);
		log.info("Mandatory fields are " + (answered ? "answered" : "not answered"));
		ask = qwandaUtils.recursivelyFindAndUpdateSubmitDisabled(ask, !answered);

		// send to user
		QDataAskMessage msg = new QDataAskMessage(ask);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);
		msg.setTag("sendAsks");
		KafkaUtils.writeMsg("webcmds", msg);
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
	public void navigateContent(ProcessData processData) {

		log.info("Navigating to form...");
		// ProcessData processData = jsonb.fromJson(processJson, ProcessData.class);
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
	public void sendBaseEntitys(ProcessData processData) {

		// ProcessData processData = jsonb.fromJson(processJson, ProcessData.class);
		BaseEntity processEntity = processData.getProcessEntity();
		List<String> attributeCodes = processData.getAttributeCodes();
		String processId = processData.getProcessId();
		String questionCode = processData.getQuestionCode();

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

		key = String.format("%s:%s", processId, questionCode);
		Ask ask = CacheUtils.getObject(userToken.getProductCode(), key, Ask.class);

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
	 * Send dropdown items for the asks.
	 *
	 * @param askMsgJson The ask message to send
	 */
	public void sendDropdownItems(ProcessData processData) {

		// ProcessData processData = jsonb.fromJson(processJson, ProcessData.class);
		String processId = processData.getProcessId();
		String questionCode = processData.getQuestionCode();

		String key = String.format("%s:%s", processId, questionCode);
		Ask ask = CacheUtils.getObject(userToken.getProductCode(), key, Ask.class);
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
