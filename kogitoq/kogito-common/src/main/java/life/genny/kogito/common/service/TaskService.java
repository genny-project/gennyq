package life.genny.kogito.common.service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.kogito.common.core.Dispatch;
import life.genny.kogito.common.core.ProcessAnswers;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.graphql.ProcessData;
import life.genny.qwandaq.message.QBulkMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.DefUtils;
import life.genny.qwandaq.utils.QwandaUtils;

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
	NavigationService navigationService;

	@Inject
	Dispatch dispatch;
	@Inject
	ProcessAnswers processAnswers;

	/**
	 * Create processData from inputs.
	 * 
	 * @param questionCode The code of the question to send
	 * @param sourceCode   The source user
	 * @param targetCode   The Target entity
	 * @param pcmCode      The code eof the PCM to use
	 * @return The processData json
	 */
	public ProcessData inputs(String questionCode, String sourceCode, String targetCode,
			String pcmCode, String events, String processId) {

		log.info("==========================================");
		log.info("processId : " + processId);
		log.info("questionCode : " + questionCode);
		log.info("sourceCode : " + sourceCode);
		log.info("targetCode : " + targetCode);
		log.info("pcmCode : " + pcmCode);
		log.info("events : " + events);
		log.info("==========================================");

		ProcessData processData = new ProcessData();
		processData.setQuestionCode(questionCode);
		processData.setSourceCode(sourceCode);
		processData.setTargetCode(targetCode);
		processData.setPcmCode(pcmCode);
		processData.setEvents(events);
		processData.setProcessId(processId);
		processData.setAnswers(new ArrayList<Answer>());

		String processEntityCode = String.format("QBE_%s", targetCode.substring(4));
		processData.setProcessEntityCode(processEntityCode);

		return processData;
	}

	/**
	 * @param processData
	 */
	public void doesTaskExist(ProcessData processData) {
		
		// check if task exists

		// re-questions if it does
	}

	/**
	 * @param processData
	 */
	public ProcessData dispatch(ProcessData processData) {

		String sourceCode = processData.getSourceCode();
		String userCode = userToken.getUserCode();

		// finish building process data
		qwandaUtils.completeProcessData(processData);

		// dispatch data
		if (sourceCode.equals(userCode))
			dispatch.sendData(processData);

		// update cached process data
		qwandaUtils.storeProcessData(processData);

		return processData;
	}

	/**
	 * Save incoming answer to the process baseentity.
	 *
	 * @param answerJson The incoming answer
	 * @param processBEJson The process entity to store the answer data
	 * @return The updated process baseentity
	 */
	public ProcessData answer(Answer answer, ProcessData processData) {

		// validate answer
		if (!processAnswers.isValid(answer, processData))
			return processData;

		processData.getAnswers().add(answer);
		dispatch.sendData(processData);

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
		QBulkMessage msg = dispatch.fetchBulkMessage(processData);
		List<Ask> asks = msg.getAsks();

		// check mandatory fields
		BaseEntity processEntity = qwandaUtils.generateProcessEntity(processData);
		if (qwandaUtils.mandatoryFieldsAreAnswered(asks, processEntity))
			return false;

		// check uniqueness
		if (processAnswers.checkUniqueness(processData))
			return false;

		// clear cache entry
		qwandaUtils.clearProcessData(processData.getProcessId());
		// save answer
		processAnswers.saveAllAnswers(processData);

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
		dispatch.sendData(processData);

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
