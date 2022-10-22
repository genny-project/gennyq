package life.genny.kogito.common.service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.graphql.ProcessData;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
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

	public static String ASK_CACHE_KEY_FORMAT = "%s:%s";

	/**
	 * Get the user code of the current user.
	 * 
	 * @return The user code
	 */
	public String getCurrentUserCode() {
		return userToken.getUserCode();
	}

	/**
	 * Cache an ask for a processId and questionCode combination.
	 * 
	 * @param processData The processData to cache for
	 * @param ask         The ask to cache
	 */
	public void cacheAsks(ProcessData processData, Ask ask) {

		String key = String.format(ASK_CACHE_KEY_FORMAT, processData.getProcessId(), processData.getQuestionCode());
		CacheUtils.putObject(userToken.getProductCode(), key, ask);
	}

	/**
	 * Fetch an ask from cache for a processId and questionCode combination.
	 * 
	 * @param processData The processData to fetch for
	 * @return
	 */
	public Ask fetchAsk(ProcessData processData) {

		String key = String.format(ASK_CACHE_KEY_FORMAT, processData.getProcessId(), processData.getQuestionCode());
		Ask ask = CacheUtils.getObject(userToken.getProductCode(), key, Ask.class);

		if (ask == null)
			ask = generateAsks(processData);

		return ask;
	}

	/**
	 * Generate the asks and save them to the cache
	 * 
	 * @param processData The process data
	 */
	public Ask generateAsks(ProcessData processData) {

		String questionCode = processData.getQuestionCode();
		String sourceCode = processData.getSourceCode();
		String targetCode = processData.getTargetCode();
		String processId = processData.getProcessId();

		BaseEntity source = beUtils.getBaseEntity(sourceCode);
		BaseEntity target = beUtils.getBaseEntity(targetCode);

		log.info("Generating asks -> " + questionCode + ":" + source.getCode() + ":" + target.getCode());

		// fetch question from DB
		Ask ask = qwandaUtils.generateAskFromQuestionCode(questionCode, source, target);
		Ask events = createEvents(processData.getButtonEvents(), sourceCode, targetCode);
		ask.addChildAsk(events);
		qwandaUtils.recursivelySetProcessId(ask, processId);

		// cache them for fast fetching
		cacheAsks(processData, ask);

		return ask;
	}

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
			String pcmCode, String buttonEvents, String processId) {

		log.info("==========================================");
		log.info("processId : " + processId);
		log.info("questionCode : " + questionCode);
		log.info("sourceCode : " + sourceCode);
		log.info("targetCode : " + targetCode);
		log.info("pcmCode : " + pcmCode);
		log.info("buttonEvents : " + buttonEvents);
		log.info("==========================================");

		ProcessData processData = new ProcessData();
		processData.setQuestionCode(questionCode);
		processData.setSourceCode(sourceCode);
		processData.setTargetCode(targetCode);
		processData.setPcmCode(pcmCode);
		processData.setButtonEvents(buttonEvents);
		processData.setProcessId(processId);
		processData.setAnswers(new ArrayList<Answer>());

		String processEntityCode = String.format("QBE_%s", targetCode.substring(4));
		processData.setProcessEntityCode(processEntityCode);

		return processData;
	}

	/**
	 * Create the events ask group.
	 * 
	 * @param events     The events string
	 * @param sourceCode The source entity code
	 * @param targetCode The target entity code
	 * @return The events ask group
	 */
	public Ask createEvents(String buttonEvents, String sourceCode, String targetCode) {

		if (buttonEvents == null)
			throw new NullParameterException("buttonEvents");

		// fetch attributes and create group
		// Attribute submit = qwandaUtils.getAttribute("EVT_SUBMIT");
		Attribute groupAttribute = qwandaUtils.getAttribute("QQQ_QUESTION_GROUP");
		Question groupQuestion = new Question("QUE_EVENTS", "", groupAttribute);

		// init ask
		Ask ask = new Ask(groupQuestion, sourceCode, targetCode);
		ask.setRealm(userToken.getProductCode());

		// split events string by comma
		for (String buttonEvent : buttonEvents.split(",")) {
			// create child and add to ask
			buttonEvent = buttonEvent.trim();
			Attribute attribute = qwandaUtils.createEvent(buttonEvent, buttonEvent);// new Attribute("EVT_" + event,
																					// event,
			// submit.getDataType());
			Question question = new Question("QUE_" + buttonEvent.toUpperCase(), buttonEvent, attribute);
			Ask child = new Ask(question, sourceCode, targetCode);
			ask.addChildAsk(child);
		}

		return ask;
	}

	/**
	 * Recursively update the ask target.
	 *
	 * @param ask    The ask to traverse
	 * @param target The target entity to set
	 */
	public void recursivelyUpdateAskTarget(Ask ask, BaseEntity target) {

		ask.setTargetCode(target.getCode());

		// recursively update children
		if (ask.getChildAsks() != null) {
			for (Ask child : ask.getChildAsks()) {
				recursivelyUpdateAskTarget(child, target);
			}
		}
	}

	/**
	 * Work out the DEF for the baseentity .
	 *
	 * @param targetCode The code of the target entity
	 * @return The DEF
	 */
	public ProcessData getDefinitionCode(ProcessData processData) {

		String targetCode = processData.getTargetCode();

		// Find the DEF
		BaseEntity target = beUtils.getBaseEntity(targetCode);
		BaseEntity definition = defUtils.getDEF(target);
		log.info("ProcessBE identified as a " + definition);

		processData.setDefinitionCode(definition.getCode());

		return processData;
	}

	/**
	 * Find the involved attribute codes.
	 * 
	 * @param processData The process data json
	 * @return process json
	 */
	public ProcessData findAttributeCodes(ProcessData processData) {

		Ask ask = fetchAsk(processData);

		// find all allowed attribute codes
		Set<String> attributeCodes = new HashSet<>();
		attributeCodes.addAll(qwandaUtils.recursivelyGetAttributeCodes(attributeCodes, ask));
		processData.setAttributeCodes(Arrays.asList(attributeCodes.toArray(new String[attributeCodes.size()])));

		return processData;
	}

}
