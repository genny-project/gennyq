package life.genny.kogito.common.core;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.kogito.common.service.TaskService;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.graphql.ProcessData;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DefUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;

/**
 * ProcessAnswers
 */
@ApplicationScoped
public class ProcessAnswers {

	private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	Jsonb jsonb = JsonbBuilder.create();

	@Inject UserToken userToken;

	@Inject QwandaUtils qwandaUtils;
	@Inject BaseEntityUtils beUtils;
	@Inject DefUtils defUtils;

	@Inject Dispatch dispatch;
	@Inject TaskService taskService;

	/**
	 * @param answer
	 * @param processData
	 * @return
	 */
	public Boolean isValid(Answer answer, ProcessData processData) {

		// ensure targetCode is correct
		if (!answer.getTargetCode().equals(processData.getProcessEntityCode())) {
			log.warn("Bad targetCode in answer!");
			return false;
		}

		// check if the answer is valid for the target
		BaseEntity definition = beUtils.getBaseEntity(processData.getDefinitionCode());
		if (!defUtils.answerValidForDEF(definition, answer)) {
			log.error("Bad incoming answer... Not saving!");
			return false;
		}

		return true;
	}

	/**
	 * Check that uniqueness of BE  (if required) is satisifed .
	 *
	 * @param processBE. The target BE containing the answer data
	 * @param defCode. The baseentity type code of the processBE
	 * @param acceptSubmission. This is modified to reflect whether the submission is valid or not.
	 * @return Boolean representing whether uniqueness is satisifed
	 */
	public Boolean checkUniqueness(ProcessData processData) {

		BaseEntity definition = beUtils.getBaseEntity(processData.getDefinitionCode());
		List<Answer> answers = processData.getAnswers();

		BaseEntity processEntity = qwandaUtils.generateProcessEntity(processData);
		BaseEntity originalTarget = beUtils.getBaseEntity(processData.getTargetCode());

		// send error for last answer in the list
		// NOTE: This should be reconsidered

		Answer answer = answers.get(answers.size()-1);
		String attributeCode = answer.getAttributeCode();

		Boolean acceptSubmission = true;
		if (qwandaUtils.isDuplicate(definition, null, processEntity, originalTarget)) {
			String feedback = "Error: This value already exists and must be unique.";

			String parentCode = processData.getQuestionCode();
			String questionCode = answer.getCode();

			qwandaUtils.sendAttributeErrorMessage(parentCode, questionCode, attributeCode, feedback);
			acceptSubmission = false;
		}

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

		KafkaUtils.writeMsg(KafkaTopic.WEBDATA, msg);
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

	/**
	 * @param processData
	 * @return
	 */
	public ProcessData deleteStoredAnswers(ProcessData processData) {

		processData.setAnswers(new ArrayList<>());

		return processData;
	}
}
