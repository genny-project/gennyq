package life.genny.kogito.common.service;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.serviceq.Service;

@ApplicationScoped
public class ProcessAnswerService {

	private static final Logger log = Logger.getLogger(ProcessAnswerService.class);

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	TaskService taskService;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	Service service;

	@Inject
	EntityManager entityManager;

	/**
	 * Save incoming answer to the process baseentity.
	 *
	 * @param answerJson The incoming answer
	 * @param processBEJson The process entity to store the answer data
	 * @return The updated process baseentity
	 */
	public String storeIncomingAnswer(String answerJson, String processBEJson, String targetCode) {

		BaseEntity processBE = jsonb.fromJson(processBEJson, BaseEntity.class);
		Answer answer = jsonb.fromJson(answerJson, Answer.class);

		// ensure targetCode is correct
		if (!answer.getTargetCode().equals(processBE.getCode())) {
			log.warn("Bad targetCode in answer!");
			return processBEJson;
		}

		// find the attribute
		String attributeCode = answer.getAttributeCode();
		Attribute attribute = qwandaUtils.getAttribute(attributeCode);
		answer.setAttribute(attribute);

		// debug log the value before saving
		String currentValue = processBE.getValueAsString(attributeCode);
		log.debug("Overwriting Value -> " + answer.getAttributeCode() + " = " + currentValue);

		// update the baseentity
		try {
			processBE.addAnswer(answer);
		} catch (BadDataException e) {
			e.printStackTrace();
		}

		// log the new value
		String savedValue = processBE.getValueAsString(answer.getAttributeCode());
		log.info("Value Saved -> " + answer.getAttributeCode() + " = " + savedValue);

		return jsonb.toJson(processBE);
	}

	/**
	 * Check if all mandatory questions have been answered.
	 *
	 * @param askMessageJson The ask message representing the questions
	 * @param processBEJson The process entity storing the answer data
	 * @return Boolean representing whether all mandatory questions have been answered
	 */
	public Boolean checkMandatory(String askMessageJson, String processBEJson) {

		BaseEntity processBE = jsonb.fromJson(processBEJson, BaseEntity.class);
		QDataAskMessage askMessage = jsonb.fromJson(askMessageJson, QDataAskMessage.class);

		// NOTE: We only ever check the first ask in the message
		Ask ask = askMessage.getItems().get(0);

		// find the submit ask
		Boolean answered = qwandaUtils.mandatoryFieldsAreAnswered(ask, processBE);
		qwandaUtils.recursivelyFindAndUpdateSubmitDisabled(ask, !answered);

		QDataAskMessage msg = new QDataAskMessage(ask);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);
		KafkaUtils.writeMsg("webdata", msg);

		return answered;
	}

	/**
	 * Save all answers gathered in the processBE.
	 *
	 * @param sourceCode The source of the answers
	 * @param targetCode The target of the answers
	 * @param processBEJson The process entity that is storing the answer data
	 */
	// @Transactional
	public void saveAllAnswers(String sourceCode, String targetCode, String processBEJson) {

		BaseEntity processBE = jsonb.fromJson(processBEJson, BaseEntity.class);
		BaseEntity target = beUtils.getBaseEntityByCode(targetCode);

		// iterate our stored process updates and create an answer
		for (EntityAttribute ea : processBE.getBaseEntityAttributes()) {

			if (ea.getAttribute() == null) {
				log.warn("Attribute is null, fetching " + ea.getAttributeCode());

				Attribute attribute = qwandaUtils.getAttribute(ea.getAttributeCode());
				ea.setAttribute(attribute);
			}
			if (ea.getPk().getBaseEntity() == null) {
				log.info("Attribute: " + ea.getAttributeCode() + ", ENTITY is NULL");
			}

			ea.setBaseEntity(target);
			if (ea.getPk().getBaseEntity() == null) {
				log.info("Attribute: " + ea.getAttributeCode() + ", ENTITY is STILLLLLLL NULL");
			}
			try {
				target.addAttribute(ea);
			} catch (BadDataException e) {
				e.printStackTrace();
			}
		}

		// save these answrs to db and cache
		beUtils.updateBaseEntity(target);
		log.info("Saved answers for target " + targetCode);

		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(target);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);

		KafkaUtils.writeMsg("webdata", msg);
	}

}
