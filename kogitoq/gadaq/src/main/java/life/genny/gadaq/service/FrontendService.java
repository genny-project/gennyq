package life.genny.gadaq.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;

import org.jboss.logging.Logger;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.message.QCmdMessage;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QuestionUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.serviceq.Service;

@ApplicationScoped
public class FrontendService {

	private static final Logger log = Logger.getLogger(FrontendService.class);

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	Service service;

	@Inject
	QuestionUtils questionUtils;

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	EntityManager entityManager;

	/**
	 * Get asks using a question code, for a given source and target.
	 *
	 * @param questionCode The question code used to fetch asks
	 * @param sourceCode The code of the source entity
	 * @param targetCode The code of the target entity
	 * @param processId The processId to set in the asks
	 * @return The ask message
	 */
	public QDataAskMessage getAsks(String questionCode, String sourceCode, String targetCode, String processId) {

		log.info("questionCode :" + questionCode);
		log.info("sourceCode :" + sourceCode);
		log.info("targetCode :" + targetCode);
		log.info("processId :" + processId);

		BaseEntity source = beUtils.getBaseEntityByCode(sourceCode);
		BaseEntity target = beUtils.getBaseEntityByCode(targetCode);

		if (source == null) {
			log.error("No Source entity found!");
			return null;
		}

		if (target == null) {
			log.error("No Target entity found!");
			return null;
		}

		log.info("Fetching asks -> " + questionCode + ":" + source.getCode() + ":" + target.getCode());

		Question rootQuestion = questionUtils.getQuestion(questionCode);
		List<Ask> asks = questionUtils.findAsks(rootQuestion, source, target);

		if (asks == null || asks.isEmpty()) {
			log.error("No asks returned for " + questionCode);
			return null;
		}

		// create ask msg from asks
		QDataAskMessage msg = new QDataAskMessage(asks.toArray(new Ask[asks.size()]));
		msg.setToken(userToken.getToken());
		msg.setReplace(true);

		// TODO: make this recursive
		// update the processId
		for (Ask ask : msg.getItems()) {
			ask.setProcessId(processId);
		}

		return msg;
	}

	/**
	 * Setup the process entity used to store task data.
	 *
	 * @param targetCode The code of the target entity
	 * @param processBE The process entity to setup
	 * @param askMsg The ask message to use in setup
	 * @return The updated process entity
	 */
	public BaseEntity setupProcessBE(String targetCode, BaseEntity processBE, QDataAskMessage askMsg) {

		if (processBE == null) {
			log.error("processBE must not be null!");
			return null;
		}

		if (askMsg == null) {
			log.error("askMsg must not be null!");
			return null;
		}

		// force the realm
		processBE.setRealm(userToken.getProductCode());

		// only copy the entityAttributes used in the Asks
		BaseEntity target = beUtils.getBaseEntityByCode(targetCode);

		// find all allowed attribute codes
		Set<String> attributeCodes = new HashSet<>();
		for (Ask ask : askMsg.getItems()) {
			attributeCodes.addAll(questionUtils.recursivelyGetAttributeCodes(attributeCodes, ask));
		}

		log.info("Found " + attributeCodes.size() + " active attributes in asks");

		// add an entityAttribute to process entity for each attribute
		for (String code : attributeCodes) {

			EntityAttribute ea = target.findEntityAttribute(code).orElse(null);
			if (ea == null) {
				Attribute attribute = qwandaUtils.getAttribute(code);
				ea = new EntityAttribute(processBE, attribute, 1.0, null);
			}
			processBE.getBaseEntityAttributes().add(ea);
		}

		log.info("ProcessBE contains " + processBE.getBaseEntityAttributes().size() + " entity attributes");

		return processBE;
	}

	/**
	 * Send a baseentity after filtering the entity attributes 
	 * based on the questions in the ask message.
	 *
	 * @param code The code of the baseentity to send
	 * @param askMsg The ask message used to filter attributes
	 */
	public void sendBaseEntity(final String code, final QDataAskMessage askMsg) {

		// only send the attribute values that are in the questions
		BaseEntity entity = beUtils.getBaseEntityByCode(code);

		// find all allowed attribute codes
		Set<String> attributeCodes = new HashSet<>();
		for (Ask ask : askMsg.getItems()) {
			attributeCodes.addAll(questionUtils.recursivelyGetAttributeCodes(attributeCodes, ask));
		}

		// grab all entityAttributes from the entity
		Set<EntityAttribute> entityAttributes = ConcurrentHashMap.newKeySet(entity.getBaseEntityAttributes().size());
		for (EntityAttribute ea : entity.getBaseEntityAttributes()) {
			entityAttributes.add(ea);
		}

		// delete any attribute that is not in the allowed Set
		for (EntityAttribute ea : entityAttributes) {
			if (!attributeCodes.contains(ea.getAttributeCode())) {
				entity.removeAttribute(ea.getAttributeCode());
			}
		}

		// send entity front end
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(entity);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);
		KafkaUtils.writeMsg("webcmds", msg);
	}

	/**
	 * Send an ask message to the frontend through the webdata topic.
	 *
	 * @param askMsg The ask message to send
	 */
	public void sendQDataAskMessage(final QDataAskMessage askMsg) {
		KafkaUtils.writeMsg("webdata", askMsg);
	}

	/**
	 * Send a command message based on a PCM code.
	 *
	 * @param code The code of the PCM baseentity
	 */
	public void sendPCM(final String code) {

		// default command
		QCmdMessage msg = new QCmdMessage("DISPLAY", "FORM");

		// only change the command if code is not for a PCM
		if (!code.startsWith("PCM")) {
			String[] displayParms = code.split(":");
			msg = new QCmdMessage(displayParms[0], displayParms[1]);
		}

		// send to frontend
		msg.setToken(userToken.getToken());
		KafkaUtils.writeMsg("webcmds", msg);
	}

}
