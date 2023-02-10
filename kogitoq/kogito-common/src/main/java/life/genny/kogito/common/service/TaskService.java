package life.genny.kogito.common.service;

import life.genny.kogito.common.core.Dispatch;
import life.genny.kogito.common.core.ProcessAnswers;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.PCM;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.graphql.ProcessData;
import life.genny.qwandaq.message.QBulkMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.*;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class TaskService {

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
	SearchUtils search;

	@Inject
	NavigationService navigationService;

	@Inject
	Dispatch dispatch;
	@Inject
	ProcessAnswers processAnswers;

	/**
	 * @param processData
	 */
	public void doesTaskExist(String sourceCode, String targetCode, String questionCode) {
		// check if task exists
		log.info("Checking if task exists...");

		// re-questions if it does
	}

	/**
	 * Fetch PCM and dispatch a readonly PCM tree update.
	 *
	 * @param sourceCode
	 * @param targetCode
	 * @param pcmCode
	 * @param parent
	 * @param location
	 */
	public void dispatch(String sourceCode, String targetCode, String pcmCode, String parent, String location) {

		if (pcmCode == null)
			throw new NullParameterException("pcmCode");
		PCM pcm = beUtils.getPCM(pcmCode);

		dispatch(sourceCode, targetCode, pcm, parent, location);
	}

	/**
	 * Dispatch a readonly PCM tree update.
	 *
	 * @param sourceCode
	 * @param targetCode
	 * @param pcm
	 * @param parent
	 * @param location
	 */
	public void dispatch(String sourceCode, String targetCode, PCM pcm, String parent, String location) {

		if (sourceCode == null)
			throw new NullParameterException("sourceCode");
		if (targetCode == null)
			throw new NullParameterException("targetCode");
		if (pcm == null)
			throw new NullParameterException("pcm");
		/*
		 * no need to check parent and location as they can sometimes be null
		 */

		// construct basic processData
		ProcessData processData = new ProcessData();
		processData.setSourceCode(sourceCode);
		processData.setTargetCode(targetCode);

		// pcm data
		processData.setPcmCode(pcm.getCode());
		processData.setParent(parent);
		processData.setLocation(location);

		// fetch target
		BaseEntity target = beUtils.getBaseEntity(targetCode);

		// build and send data
		QBulkMessage msg = dispatch.build(processData, pcm);
		msg.add(target);
		dispatch.sendData(msg);

		// send searches
		for (String code : processData.getSearches())
			search.searchTable(code);
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
		log.info("Dispatching...");

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

		log.info("==========================================");
		log.info("processId : " + processId);
		log.info("questionCode : " + questionCode);
		log.info("sourceCode : " + sourceCode);
		log.info("targetCode : " + targetCode);
		log.info("pcmCode : " + pcmCode);
		log.info("parent : " + parent);
		log.info("location : " + location);
		log.info("buttonEvents : " + buttonEvents);
		log.info("==========================================");

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

		String processEntityCode = String.format("QBE_%s", targetCode.substring(4));
		processData.setProcessEntityCode(processEntityCode);

		String userCode = userToken != null ? userToken.getUserCode() : null;

		// find target and target definition
		BaseEntity target = beUtils.getBaseEntity(targetCode);
		BaseEntity definition = defUtils.getDEF(target);
		processData.setDefinitionCode(definition.getCode());

		// update cached process data
		qwandaUtils.storeProcessData(processData);

		// dispatch data
		if (!sourceCode.equals(userCode)) { // TODO: Not every task has a userCode
			log.info("Task on hold: User is not source");
			return processData;
		}

		// build data
		QBulkMessage msg = dispatch.build(processData);
		Set<Ask> asks = msg.getAsks();
		Map<String, Ask> flatMapOfAsks = qwandaUtils.buildAskFlatMap(asks);

		// perform basic checks on attribute codes
		processData.setAttributeCodes(
			flatMapOfAsks.values().stream()
					.map(ask -> ask.getQuestion().getAttributeCode())
					.filter(code -> qwandaUtils.attributeCodeMeetsBasicRequirements(code))
					.collect(Collectors.toList())
		);
		log.info("Current Scope Attributes: " + processData.getAttributeCodes());

		// handle non-readonly if necessary
		if (dispatch.containsNonReadonly(flatMapOfAsks)) {
			BaseEntity processEntity = dispatch.handleNonReadonly(processData, asks, flatMapOfAsks, msg);
			msg.add(processEntity);

			qwandaUtils.storeProcessData(processData);
			// only cache for non-readonly invocation
			qwandaUtils.cacheAsks(processData, asks);

			// handle initial dropdown selections
			// TODO: change to use flatMap
			for (Ask ask : asks)
				dispatch.handleDropdownAttributes(ask, ask.getQuestion().getCode(), processEntity, msg);

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
			search.searchTable(code);
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

		processData.getAnswers().add(answer);

		Set<Ask> asks = qwandaUtils.fetchAsks(processData);
		Map<String, Ask> flatMapOfAsks = qwandaUtils.buildAskFlatMap(asks);

		QBulkMessage msg = new QBulkMessage();
		dispatch.handleNonReadonly(processData, asks, flatMapOfAsks, msg);

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
		Set<Ask> asks = qwandaUtils.fetchAsks(processData);
		Map<String, Ask> flatMapOfAsks = qwandaUtils.buildAskFlatMap(asks);

		// check mandatory fields
		BaseEntity processEntity = qwandaUtils.generateProcessEntity(processData);
		if (!qwandaUtils.mandatoryFieldsAreAnswered(flatMapOfAsks, processEntity))
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

}
