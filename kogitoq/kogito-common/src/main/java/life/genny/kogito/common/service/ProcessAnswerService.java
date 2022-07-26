package life.genny.kogito.common.service;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.graphql.ProcessQuestions;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
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
	public String storeIncomingAnswer(String answerJson, String processJson) {

		ProcessQuestions processData = jsonb.fromJson(processJson, ProcessQuestions.class);
		String processId = processData.getProcessId();
		String targetCode = processData.getTargetCode();

		BaseEntity processEntity = processData.getProcessEntity();
		Answer answer = jsonb.fromJson(answerJson, Answer.class);

		// ensure targetCode is correct
		if (!answer.getTargetCode().equals(processEntity.getCode())) {
			log.warn("Bad targetCode in answer!");
			return jsonb.toJson(processData);
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
				return jsonb.toJson(processData);
			}
		}

		// find the attribute
		String attributeCode = answer.getAttributeCode();
		Attribute attribute = qwandaUtils.getAttribute(attributeCode);
		answer.setAttribute(attribute);

		// debug log the value before saving
		String currentValue = processEntity.getValueAsString(attributeCode);
		log.debug("Overwriting Value -> " + answer.getAttributeCode() + " = " + currentValue);

		// update the baseentity
		processEntity.addAnswer(answer);
		String value = processEntity.getValueAsString(answer.getAttributeCode());
		log.info("Value Saved -> " + answer.getAttributeCode() + " = " + value);

		// update the cached process data object
		String productCode = userToken.getProductCode();
		String key = String.format("%s:PROCESS_DATA", processId); 
		processData.setProcessEntity(processEntity);
		CacheUtils.putObject(productCode, key, processData);
		log.infof("ProcessData cached to %s", key);

		return jsonb.toJson(processData);
	}

	/**
	 * Check if all mandatory questions have been answered.
	 *
	 * @param askMessageJson The ask message representing the questions
	 * @param processBEJson The process entity storing the answer data
	 * @return Boolean representing whether all mandatory questions have been answered
	 */
	public Boolean checkMandatory(String processJson) {

		ProcessQuestions processData = jsonb.fromJson(processJson, ProcessQuestions.class);
		BaseEntity processEntity = processData.getProcessEntity();
		QDataAskMessage askMessage = processData.getAskMessage();

		// NOTE: We only ever check the first ask in the message
		Ask ask = askMessage.getItems().get(0);

		// find the submit ask
		Boolean answered = qwandaUtils.mandatoryFieldsAreAnswered(ask, processEntity);
		qwandaUtils.recursivelyFindAndUpdateSubmitDisabled(ask, !answered);

		QDataAskMessage msg = new QDataAskMessage(ask);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);
		KafkaUtils.writeMsg("webcmds", msg);

		return answered;
	}

	/**
	 * Check that uniqueness of BE  (if required) is satisifed .
	 *
	 * @param processBE. The target BE containing the answer data
	 * @param defCode. The baseentity type code of the processBE
	 * @param acceptSubmission. This is modified to reflect whether the submission is valid or not.
	 * @return Boolean representing whether uniqueness is satisifed
	 */
	public Boolean checkUniqueness(String processJson, Boolean acceptSubmission) {
		ProcessQuestions processData = jsonb.fromJson(processJson, ProcessQuestions.class);
		BaseEntity target = beUtils.getBaseEntity(processData.getTargetCode());
		BaseEntity definition = beUtils.getBaseEntity(processData.getDefinitionCode());
		BaseEntity processEntity = processData.getProcessEntity();
		QDataAskMessage askMessage = processData.getAskMessage();

		// Check if attribute code exists as a UNQ for the DEF
		List<EntityAttribute> uniqueAttributes = definition.findPrefixEntityAttributes("UNQ");
		log.info("Found " + uniqueAttributes.size() + " UNQ attributes");
		
		for (EntityAttribute uniqueAttribute : uniqueAttributes) {
			// Convert to non def attribute Code
			String attributeCode = uniqueAttribute.getAttributeCode();
			attributeCode = StringUtils.removeStart(attributeCode, "UNQ_");
			log.info("Checking UNQ attribute " + attributeCode);

			String uniqueValue = processEntity.getValueAsString(attributeCode);
			acceptSubmission &= qwandaUtils.checkDuplicateAttribute(target, definition, attributeCode, uniqueValue);
		}

		// disable submit button if not unique
		qwandaUtils.sendSubmit(askMessage, acceptSubmission);

		return acceptSubmission;
	}

	/**
	 * Save all answers gathered in the processBE.
	 *
	 * @param targetCode The target of the answers
	 * @param processBEJson The process entity that is storing the answer data
	 */
	public void saveAllAnswers(String processJson) {

		ProcessQuestions processData = jsonb.fromJson(processJson, ProcessQuestions.class);
		BaseEntity processEntity = processData.getProcessEntity();
		String targetCode = processData.getTargetCode();

		// only copy the entityAttributes used in the Asks
		if ("NON_EXISTENT".equals(targetCode)) {
			log.info("Not saving to NON_EXISTENT");
			return;
		}

		BaseEntity target = beUtils.getBaseEntity(targetCode);

		// iterate our stored process updates and create an answer
		for (EntityAttribute ea : processEntity.getBaseEntityAttributes()) {

			if (ea.getAttribute() == null) {
				log.warn("Attribute is null, fetching " + ea.getAttributeCode());

				Attribute attribute = qwandaUtils.getAttribute(ea.getAttributeCode());
				ea.setAttribute(attribute);
			}
			ea.setBaseEntity(target);
			target.addAttribute(ea);

			// set name
			if ("PRI_NAME".equals(ea.getAttributeCode())) {
				target.setName(ea.getValue());
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
