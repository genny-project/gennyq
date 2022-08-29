package life.genny.kogito.common.service;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.graphql.ProcessData;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DefUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;

@ApplicationScoped
public class ProcessAnswerService {

	private static final Logger log = Logger.getLogger(ProcessAnswerService.class);

	Jsonb jsonb = JsonbBuilder.create();

	@Inject UserToken userToken;

	@Inject QwandaUtils qwandaUtils;
	@Inject BaseEntityUtils beUtils;
	@Inject DefUtils defUtils;

	@Inject FrontendService frontendService;
	@Inject TaskService taskService;

	/**
	 * Save incoming answer to the process baseentity.
	 *
	 * @param answerJson The incoming answer
	 * @param processBEJson The process entity to store the answer data
	 * @return The updated process baseentity
	 */
	public ProcessData storeIncomingAnswer(Answer answer, ProcessData processData) {

		// ensure targetCode is correct
		if (!answer.getTargetCode().equals(processData.getProcessEntityCode())) {
			log.warn("Bad targetCode in answer!");
			return processData;
		}

		// check if the answer is valid for the target
		BaseEntity definition = beUtils.getBaseEntity(processData.getDefinitionCode());
		if (!defUtils.answerValidForDEF(definition, answer)) {
			log.error("Bad incoming answer... Not saving!");
			return processData;
		}

		processData.getAnswers().add(answer);
		qwandaUtils.storeProcessData(processData);

		return processData;
	}

	/**
	 * @param processData
	 * @return
	 */
	public ProcessData deleteStoredAnswers(ProcessData processData) {

		processData.setAnswers(new ArrayList<>());

		return processData;
	}

	/**
	 * Check if all mandatory questions have been answered.
	 *
	 * @param askMessageJson The ask message representing the questions
	 * @param processBEJson The process entity storing the answer data
	 * @return Boolean representing whether all mandatory questions have been answered
	 */
	public Boolean checkMandatory(ProcessData processData) {

		Ask ask = taskService.fetchAsk(processData);

		// update ask target
		BaseEntity processEntity = qwandaUtils.generateProcessEntity(processData);
		frontendService.recursivelyUpdateAskTarget(ask, processEntity);

		// find the submit ask
		List<Answer> answers = processData.getAnswers();
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
	public Boolean checkUniqueness(ProcessData processData, Boolean acceptSubmission) {

		BaseEntity definition = beUtils.getBaseEntity(processData.getDefinitionCode());
		List<Answer> answers = processData.getAnswers();

		BaseEntity processEntity = qwandaUtils.generateProcessEntity(processData);
		BaseEntity originalTarget = beUtils.getBaseEntity(processData.getTargetCode());

		// send error for last answer in the list
		// NOTE: This should be reconsidered
		Answer answer = answers.get(answers.size()-1);
		String attributeCode = answer.getAttributeCode();

		if (qwandaUtils.isDuplicate(definition, null, processEntity, originalTarget)) {
			String feedback = "Error: This value already exists and must be unique.";

			String parentCode = processData.getQuestionCode();
			String questionCode = answer.getCode();

			qwandaUtils.sendAttributeErrorMessage(parentCode, questionCode, attributeCode, feedback);
			acceptSubmission = false;
		}

		Ask ask = taskService.fetchAsk(processData);

		// disable submit button if not unique
		qwandaUtils.sendSubmit(ask, acceptSubmission);

		return acceptSubmission;
	}

	/**
	 * Save all answers gathered in the processBE.
	 * @param targetCode The target of the answers
	 * @param processBEJson The process entity that is storing the answer data
	 */
	public void saveAllAnswers(ProcessData processData) {

		String targetCode = processData.getTargetCode();
		BaseEntity target = beUtils.getBaseEntity(targetCode);

		// iterate our stored process updates and create an answer
		for (Answer answer : processData.getAnswers()) {

			// find the attribute
			String attributeCode = answer.getAttributeCode();
			Attribute attribute = qwandaUtils.getAttribute(attributeCode);
			answer.setAttribute(attribute);

			// debug log the value before saving
			String currentValue = target.getValueAsString(attributeCode);
			log.debug("Overwriting Value -> " + answer.getAttributeCode() + " = " + currentValue);

			// update the baseentity
			target.addAnswer(answer);
			String value = target.getValueAsString(answer.getAttributeCode());
			log.info("Value Saved -> " + answer.getAttributeCode() + " = " + value);
		}

		// save these answrs to db and cache
		beUtils.updateBaseEntity(target);
		log.info("Saved answers for target " + targetCode);

		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(target);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);

		KafkaUtils.writeMsg("webdata", msg);
	}

	/**
	 * Clear completed or canceled process Cache Entries.
	 *
	 * @param productCode 
	 * @param processBEcode
	 * @return Boolean existed
	 */
	public Boolean clearProcessCacheEntries(String processId, String targetCode) {

		qwandaUtils.clearProcessData(processId);
		log.infof("Cleared caches for %s",processId);
		return true;
	}

}
