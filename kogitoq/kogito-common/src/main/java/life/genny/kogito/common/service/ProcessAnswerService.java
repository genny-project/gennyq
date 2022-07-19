package life.genny.kogito.common.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.qwandaq.message.QDataMessage;
import life.genny.qwandaq.models.ProcessVariables;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.DefUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;

@ApplicationScoped
public class ProcessAnswerService {

	private static final Logger log = Logger.getLogger(ProcessAnswerService.class);

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	DefUtils defUtils;


	/**
	 * Save incoming answer to the process baseentity.
	 *
	 * @param answerJson The incoming answer
	 * @param processBEJson The process entity to store the answer data
	 * @return The updated process baseentity
	 */
	public String storeIncomingAnswer(String answerJson, String processBEJson, String targetCode, String processId, String defCode) {

		BaseEntity processBE = jsonb.fromJson(processBEJson, BaseEntity.class);
		Answer answer = jsonb.fromJson(answerJson, Answer.class);

		// ensure targetCode is correct
		if (!answer.getTargetCode().equals(processBE.getCode())) {
			log.warn("Bad targetCode in answer!");
			return processBEJson;
		}

		// only copy the entityAttributes used in the Asks
		BaseEntity target = null;
		if ("NON_EXISTENT".equals(targetCode)) {
			log.info("Not Checking validity of answer");
		} else {
			target = beUtils.getBaseEntity(targetCode);
			// check if the answer is valid for the target
			BaseEntity definition = defUtils.getDEF(target);
			if (!defUtils.answerValidForDEF(definition, answer)) {
				log.error("Bad incoming answer... Not saving!");
				return processBEJson;
			}
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

		String productCode = userToken.getProductCode();
		String key = processId+":PROCESS_BE";

		// update the cached ProcessVariables object
		ProcessVariables processVariables = CacheUtils.getObject(productCode, key, ProcessVariables.class);
		processVariables.setProcessEntity(processBE);
		CacheUtils.putObject(productCode, key, processVariables);

		String value = processBE.getValueAsString(answer.getAttributeCode());
		log.info("Value Saved -> " + answer.getAttributeCode() + " = " + value);
		log.info("Process Entity cached to " + key);

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
		QDataMessage<Ask> askMessage = jsonb.fromJson(askMessageJson, QDataMessage.class);

		// NOTE: We only ever check the first ask in the message
		Ask ask = askMessage.getItems().get(0);

		// find the submit ask
		Boolean answered = qwandaUtils.mandatoryFieldsAreAnswered(ask, processBE);
		qwandaUtils.recursivelyFindAndUpdateSubmitDisabled(ask, !answered);

		QDataMessage<Ask> msg = new QDataMessage<Ask>(ask);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);
		KafkaUtils.writeMsg("webcmds", msg);

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

		// only copy the entityAttributes used in the Asks
		if ("NON_EXISTENT".equals(targetCode)) {
			log.info("Not saving to NON_EXISTENT");
			return;
		}

		BaseEntity target = beUtils.getBaseEntity(targetCode);

		// iterate our stored process updates and create an answer
		for (EntityAttribute ea : processBE.getBaseEntityAttributes()) {

			if (ea.getAttribute() == null) {
				log.warn("Attribute is null, fetching " + ea.getAttributeCode());

				Attribute attribute = qwandaUtils.getAttribute(ea.getAttributeCode());
				ea.setAttribute(attribute);
			}

			ea.setBaseEntity(target);

			try {
				target.addAttribute(ea);
			} catch (BadDataException e) {
				e.printStackTrace();
			}
			// set name
			if ("PRI_NAME".equals(ea.getAttributeCode())) {
				target.setName(ea.getValue());
			}
		}

		// save these answrs to db and cache
		beUtils.updateBaseEntity(target);
		log.info("Saved answers for target " + targetCode);

		QDataMessage<BaseEntity> msg = new QDataMessage<BaseEntity>(target);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);

		KafkaUtils.writeMsg("webdata", msg);
	}

}
