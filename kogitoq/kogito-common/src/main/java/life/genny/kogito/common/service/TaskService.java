package life.genny.kogito.common.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import life.genny.kogito.common.core.Dispatch;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.PCM;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.graphql.ProcessData;
import life.genny.qwandaq.message.QBulkMessage;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.utils.KafkaUtils;

@ApplicationScoped
public class TaskService extends KogitoService {

	@Inject
	Logger log;

	/**
	 * @param processData
	 */
	public void doesTaskExist(String sourceCode, String targetCode, String questionCode) {
		// check if task exists
		log.info("Checking if task exists...");

		// TODO: re-questions if it does
	}

	/**
	 * Build a question group and assign to the PCM before dispatching a PCM tree
	 * update.
	 *
	 * @param sourceCode
	 * @param targetCode
	 * @param questionCode
	 * @param processId
	 * @param pcmCode
	 * @param parent
	 * @param location
	 * @param events
	 * @return
	 */
	public ProcessData dispatchTask(String sourceCode, String targetCode, String questionCode, String processId,
			String pcmCode, String parent, String location, String buttonEvents) {
		if (sourceCode == null) {
			throw new NullParameterException("sourceCode");
		}
		if (targetCode == null) {
			throw new NullParameterException("targetCode");
		}
		if (processId == null) {
			throw new NullParameterException("processId");
		}
		if (pcmCode == null) {
			throw new NullParameterException("pcmCode");
		}
		// defaults
		if (parent == null) {
			parent = PCM.PCM_CONTENT;
		}
		if (location == null) {
			location = PCM.location(1);
		}

		log.info("[ ========== ProcessId : " + processId + " ========== ]");
		log.info("[  sourceCode : " + sourceCode + " || targetCode : " + targetCode + "  ]");
		log.info("[  pcmCode : " + pcmCode + " || parent : " + parent + " || location : " + location + "  ]");
		log.info("[  buttonEvents : " + buttonEvents + " || questionCode : " + questionCode + "  ]");
		log.info("[ ================================================================== ]");

		// init process data
		ProcessData processData = new ProcessData();
		processData.setQuestionCode(questionCode);
		processData.setSourceCode(sourceCode);
		processData.setTargetCode(targetCode);

		// pcm data
		processData.setPcmCode(pcmCode);
		processData.setParent(parent);
		processData.setLocation(location);

		processData.setButtonEvents(buttonEvents);
		processData.setProcessId(processId);
		processData.setAnswers(new ArrayList<>());

		String processEntityCode = Prefix.QBE_.concat(targetCode.substring(4));
		processData.setProcessEntityCode(processEntityCode);

		String userCode = userToken != null ? userToken.getUserCode() : null;

		// find target and target definition
		BaseEntity target = beUtils.getBaseEntity(targetCode);
		BaseEntity definition = defUtils.getDEF(target);
		processData.setDefinitionCode(definition.getCode());

		// update cached process data
		qwandaUtils.storeProcessData(processData);

		// TODO: Not every task has a userCode
		if (!sourceCode.equals(userCode)) {
			log.info("Task on hold: User is not source");
			return processData;
		}

		// build data
		QBulkMessage msg = dispatch.build(processData);
		List<Ask> asks = msg.getAsks();
		Map<String, Ask> flatMapOfAsks = QwandaUtils.buildAskFlatMap(asks);

		// perform basic checks on attribute codes
		processData.setAttributeCodes(
			flatMapOfAsks.values().stream()
					.map(ask -> ask.getQuestion().getAttribute().getCode())
					.filter(code -> QwandaUtils.attributeCodeMeetsBasicRequirements(code))
					.collect(Collectors.toList())
		);
		log.info("Current Scope Attributes: " + processData.getAttributeCodes());

		boolean readonly = flatMapOfAsks.values().stream()
			.allMatch(ask -> ask.getReadonly());

		processData.setReadonly(readonly);

		// handle non-readonly if necessary
		// use dispatch.containsNonReadonly(flatMapOfAsks) if this does not work
		if (!readonly) {
			BaseEntity processEntity = dispatch.handleNonReadonly(processData, asks, flatMapOfAsks, msg);
			msg.add(processEntity);

			qwandaUtils.storeProcessData(processData);
			// only cache for non-readonly invocation
			qwandaUtils.cacheAsks(processData, asks);
			// ProcessEntity essentially becomes our target
			target = processEntity;
		} else {
			msg.add(target);
		}
		// handle initial dropdown selections
		// TODO: change to use flatMap
		for (Ask ask : asks) {
			dispatch.handleDropdownAttributes(ask, ask.getQuestion().getCode(), target, msg);
		}
		// send asks and BEs
		dispatch.sendData(msg);
		// send searches
		for (String code : processData.getSearches()) {
			log.debug("Sending search: " + code);
			searchUtils.searchTable(code);
		}

		return processData;
	}

	/**
	 * Save incoming answer to the process baseentity.
	 *
	 * @param answerJson    The incoming answer
	 * @param processBEJson The process entity to store the answer data
	 * @return The updated process baseentity
	 */
	public ProcessData answer(Answer answer, ProcessData processData) {

		// validate answer
		if (!processAnswers.isValid(answer, processData))
			return processData;

		// remove previous answers for this attribute
		List<Answer> answers = processData.getAnswers();
		for (int i = 0; i < answers.size();) {
			Answer a = answers.get(i);
			if (a.getAttributeCode().equals(answer.getAttributeCode()) 
				&& a.getTargetCode().equals(answer.getTargetCode())) {
				log.info("Found duplicate : " + a.getAttributeCode());
				answers.remove(i);
			} else {
				i++;
			}
		}
		// add new answer
		answers.add(answer);
		processData.setAnswers(answers);

		List<Ask> asks = qwandaUtils.fetchAsks(processData);
		Map<String, Ask> flatMapOfAsks = QwandaUtils.buildAskFlatMap(asks);

		QBulkMessage msg = new QBulkMessage();
		dispatch.handleNonReadonly(processData, asks, flatMapOfAsks, msg);

		//check duplicate records
		if (!processAnswers.checkUniqueness(processData)) {
			disableButtons(processData);

			return processData;
		}

		// send data to FE
		dispatch.sendData(msg);

		// update cached process data
		qwandaUtils.storeProcessData(processData);

		return processData;
	}

	/**
	 * @param processData
	 * @return
	 */
	public Boolean submit(ProcessData processData) {
		// construct bulk message
		List<Ask> asks = qwandaUtils.fetchAsks(processData);
		Map<String, Ask> flatMapOfAsks = QwandaUtils.buildAskFlatMap(asks);

		// check mandatory fields
		BaseEntity processEntity = qwandaUtils.generateProcessEntity(processData);
		if (!QwandaUtils.mandatoryFieldsAreAnswered(flatMapOfAsks, processEntity))
			return false;

		// check uniqueness in answers
		if (!processAnswers.checkUniqueness(processData))
			return false;

		// save answer
		processAnswers.saveAllAnswers(processData);

		// clear cache entry
		qwandaUtils.clearProcessData(processData.getProcessId());

		return true;
	}

	/**
	 * @param processData
	 * @return
	 */
	public ProcessData reset(ProcessData processData) {
		// delete stored answers
		processData.setAnswers(new ArrayList<Answer>());
		qwandaUtils.storeProcessData(processData);

		// resend BaseEntities
		dispatch.build(processData);

		return processData;
	}

	/**
	 * @param processData
	 */
	public void cancel(ProcessData processData) {
		// clear cache entry
		qwandaUtils.clearProcessData(processData.getProcessId());
		// default redirect
		navigationService.redirect();
	}

	/**
	 * Disable buttons if it is not valid data
	 * @param processData Process Data
	 */
	public void disableButtons(ProcessData processData) {
		List<Ask> asks = qwandaUtils.fetchAsks(processData);
		Map<String, Ask> flatMapOfAsks = QwandaUtils.buildAskFlatMap(asks);

		for (String event : Dispatch.BUTTON_EVENTS) {
			Ask evt = flatMapOfAsks.get(event);
			if (evt != null)
				evt.setDisabled(true);
		}

		QDataAskMessage msg = new QDataAskMessage(asks);
		msg.setReplace(true);
		msg.setToken(userToken.getToken());

		KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
	}
}
