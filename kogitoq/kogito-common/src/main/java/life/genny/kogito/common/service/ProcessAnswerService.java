package life.genny.kogito.common.service;

import java.util.ArrayList;
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
import life.genny.qwandaq.graphql.ProcessData;
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

	@Inject
	FrontendService frontendService;

	/**
	 * Save incoming answer to the process baseentity.
	 *
	 * @param answerJson The incoming answer
	 * @param processBEJson The process entity to store the answer data
	 * @return The updated process baseentity
	 */
	public String storeIncomingAnswer(String answerJson, String processJson) {

		ProcessData processData = jsonb.fromJson(processJson, ProcessData.class);
		String processId = processData.getProcessId();
		Answer answer = jsonb.fromJson(answerJson, Answer.class);

		// ensure targetCode is correct
		if (!answer.getTargetCode().equals(processData.getProcessEntityCode())) {
			log.warn("Bad targetCode in answer!");
			return jsonb.toJson(processData);
		}

		// check if the answer is valid for the target
		BaseEntity definition = beUtils.getBaseEntity(processData.getDefinitionCode());
		if (!defUtils.answerValidForDEF(definition, answer)) {
			log.error("Bad incoming answer... Not saving!");
			return jsonb.toJson(processData);
		}

		processData.getAnswers().add(answer);
		qwandaUtils.storeProcessData(processData);

		return jsonb.toJson(processData);
	}

	/**
	 * @param processJson
	 * @return
	 */
	public String deleteStoredAnswers(String processJson) {

		ProcessData processData = jsonb.fromJson(processJson, ProcessData.class);
		processData.setAnswers(new ArrayList<>());

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

		ProcessData processData = jsonb.fromJson(processJson, ProcessData.class);
		List<Answer> answers = processData.getAnswers();
		String processId = processData.getProcessId();
		String targetCode = processData.getTargetCode();
		String questionCode = processData.getQuestionCode();

		String key = String.format("%s:%s", processId, questionCode);
		Ask ask = CacheUtils.getObject(userToken.getProductCode(), key, Ask.class);

		// update ask target
		BaseEntity processEntity = qwandaUtils.generateProcessEntity(processData);
		frontendService.recursivelyUpdateAskTarget(ask, processEntity);

		// find the submit ask
		Boolean answered = qwandaUtils.mandatoryFieldsAreAnswered(ask, answers);
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

		ProcessData processData = jsonb.fromJson(processJson, ProcessData.class);
		BaseEntity target = beUtils.getBaseEntity(processData.getTargetCode());
		BaseEntity definition = beUtils.getBaseEntity(processData.getDefinitionCode());
		List<Answer> answers = processData.getAnswers();
		String processId = processData.getProcessId();
		String questionCode = processData.getQuestionCode();

		// Check if attribute code exists as a UNQ for the DEF
		List<EntityAttribute> uniqueAttributes = definition.findPrefixEntityAttributes("UNQ");
		log.info("Found " + uniqueAttributes.size() + " UNQ attributes");
		
		for (EntityAttribute uniqueAttribute : uniqueAttributes) {
			// Convert to non def attribute Code
			final String attributeCode = StringUtils.removeStart(uniqueAttribute.getAttributeCode(), "UNQ_");
			log.info("Checking UNQ attribute " + attributeCode);

			String uniqueValue = answers.stream()
				.filter(a -> a.getAttributeCode().equals(attributeCode))
				.findFirst().get().getValue();

			if (qwandaUtils.isDuplicate(target, definition, attributeCode, uniqueValue))
				acceptSubmission = false;
		}

		String key = String.format("%s:%s", processId, questionCode);
		Ask ask = CacheUtils.getObject(userToken.getProductCode(), key, Ask.class);

		// disable submit button if not unique
		qwandaUtils.sendSubmit(ask, acceptSubmission);

		return acceptSubmission;
	}

	/**
	 * Save all answers gathered in the processBE.
	 * @param targetCode The target of the answers
	 * @param processBEJson The process entity that is storing the answer data
	 */
	public void saveAllAnswers(String processJson) {

		ProcessData processData = jsonb.fromJson(processJson, ProcessData.class);
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
