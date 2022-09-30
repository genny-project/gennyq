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
	 * @param processData
	 */
	public void doesTaskExist(String sourceCode, String targetCode, String questionCode) {
		
		// check if task exists

		// re-questions if it does
	}

	/**
	 * @param sourceCode
	 * @param targetCode
	 * @param pcmCode
	 * @param parent
	 * @param location
	 */
	public void dispatch(String sourceCode, String targetCode, String pcmCode, String parent, String location) {

		// construct basic processData
		ProcessData processData = new ProcessData();
		processData.setSourceCode(sourceCode);
		processData.setTargetCode(targetCode);

		// pcm data
		processData.setPcmCode(pcmCode);
		processData.setParent(parent);
		processData.setLocation(location);

		// build and send data
		dispatch.buildAndSend(processData);
	}

	/**
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
	public ProcessData dispatch(String sourceCode, String targetCode, String questionCode, String processId, 
			String pcmCode, String parent, String location, String events) {

		log.info("==========================================");
		log.info("processId : " + processId);
		log.info("questionCode : " + questionCode);
		log.info("sourceCode : " + sourceCode);
		log.info("targetCode : " + targetCode);
		log.info("pcmCode : " + pcmCode);
		log.info("events : " + events);
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

		processData.setEvents(events);
		processData.setProcessId(processId);
		processData.setAnswers(new ArrayList<Answer>());

		String processEntityCode = String.format("QBE_%s", targetCode.substring(4));
		processData.setProcessEntityCode(processEntityCode);

		String userCode = userToken.getUserCode();

		// find target and target definition
		BaseEntity target = beUtils.getBaseEntity(targetCode);
		BaseEntity definition = defUtils.getDEF(target);
		processData.setDefinitionCode(definition.getCode());

		// dispatch data
		if (sourceCode.equals(userCode))
			dispatch.buildAndSend(processData);

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
		dispatch.buildAndSend(processData);

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
		dispatch.buildAndSend(processData);

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
