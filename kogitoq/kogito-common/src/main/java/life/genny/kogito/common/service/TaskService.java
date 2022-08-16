package life.genny.kogito.common.service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.graphql.ProcessQuestions;
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

	/**
	 * Get the user code of the current user.
	 * @return The user code
	 */
	public String getCurrentUserCode() {
		return userToken.getUserCode();
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
	public String inputs(String questionCode, String sourceCode, String targetCode,
			String pcmCode, String events, String processId) {

		ProcessQuestions processData = new ProcessQuestions();
		processData.setQuestionCode(questionCode);
		processData.setSourceCode(sourceCode);
		processData.setTargetCode(targetCode);
		processData.setPcmCode(pcmCode);
		processData.setEvents(events);
		processData.setProcessId(processId);

		return jsonb.toJson(processData);
	}

	/**
	 * Generate the asks and save them to the cache
	 * @param processJson The process data json
	 */
	public void generateAndCacheAsks(String processJson) {

		ProcessQuestions processData = jsonb.fromJson(processJson, ProcessQuestions.class);

		String questionCode = processData.getQuestionCode();
		String sourceCode = processData.getSourceCode();
		String targetCode = processData.getTargetCode();
		String processId = processData.getProcessId();

		log.info("==========================================");
		log.info("processId : " + processId);
		log.info("questionCode : " + questionCode);
		log.info("sourceCode : " + sourceCode);
		log.info("targetCode : " + targetCode);
		log.info("pcmCode : " + processData.getPcmCode());
		log.info("events : " + processData.getEvents());
		log.info("==========================================");

		BaseEntity source = beUtils.getBaseEntity(sourceCode);
		BaseEntity target = beUtils.getBaseEntity(targetCode);

		log.info("Fetching asks -> " + questionCode + ":" + source.getCode() + ":" + target.getCode());

		// fetch question from DB
		Ask ask = qwandaUtils.generateAskFromQuestionCode(questionCode, source, target);
		Ask events = createEvents(processData.getEvents(), sourceCode, targetCode);
		ask.addChildAsk(events);
		qwandaUtils.recursivelySetProcessId(ask, processId);

		String key = String.format("%s:%s", processId, questionCode);
		CacheUtils.putObject(userToken.getProductCode(), key, ask);
	}

	/**
	 * Create the events ask group.
	 * 
	 * @param events     The events string
	 * @param sourceCode The source entity code
	 * @param targetCode The target entity code
	 * @return The events ask group
	 */
	public Ask createEvents(String events, String sourceCode, String targetCode) {

		if (events == null)
			throw new NullParameterException("events");

		// fetch attributes and create group
		Attribute submit = qwandaUtils.getAttribute("EVT_SUBMIT");
		Attribute groupAttribute = qwandaUtils.getAttribute("QQQ_QUESTION_GROUP");
		Question groupQuestion = new Question("QUE_EVENTS", "", groupAttribute);

		// init ask
		Ask ask = new Ask(groupQuestion, sourceCode, targetCode);
		ask.setRealm(userToken.getProductCode());

		// split events string by comma
		for (String event : events.split(",")) {
			// create child and add to ask
			Attribute attribute = new Attribute("EVT_" + event, event, submit.getDataType());
			Question question = new Question("QUE_" + event, event, attribute);
			Ask child = new Ask(question, sourceCode, targetCode);
			ask.addChildAsk(child);
		}

		return ask;
	}

	/**
	 * Update the ask target to use the process entity.
	 * @param processJson The process data json
	 */
	public void updateAskTarget(String processJson) {

		ProcessQuestions processData = jsonb.fromJson(processJson, ProcessQuestions.class);
		BaseEntity processEntity = processData.getProcessEntity();
		String processId = processData.getProcessId();
		String questionCode = processData.getQuestionCode();

		// fetch asks from cache
		String key = String.format("%s:%s", processId, questionCode);
		Ask ask = CacheUtils.getObject(userToken.getProductCode(), key, Ask.class);
		
		// update ask target
		recursivelyUpdateAskTarget(ask, processEntity);
		CacheUtils.putObject(userToken.getProductCode(), key, ask);
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
	public String getDEF(String processJson) {

		ProcessQuestions processData = jsonb.fromJson(processJson, ProcessQuestions.class);
		String targetCode = processData.getTargetCode();

		// Find the DEF
		BaseEntity target = beUtils.getBaseEntity(targetCode);
		BaseEntity definition = defUtils.getDEF(target);
		log.info("ProcessBE identified as a " + definition);

		processData.setDefinitionCode(definition.getCode());

		return jsonb.toJson(processData);
	}

	/**
	 * Find the involved attribute codes.
	 * @param processJson The process data json
	 * @return process json
	 */
	public String findAttributeCodes(String processJson) {

		ProcessQuestions processData = jsonb.fromJson(processJson, ProcessQuestions.class);
		String processId = processData.getProcessId();
		String questionCode = processData.getQuestionCode();

		String key = String.format("%s:%s", processId, questionCode);
		Ask ask = CacheUtils.getObject(userToken.getProductCode(), key, Ask.class);

		// find all allowed attribute codes
		Set<String> attributeCodes = new HashSet<>();
		attributeCodes.addAll(qwandaUtils.recursivelyGetAttributeCodes(attributeCodes, ask));
		processData.setAttributeCodes(Arrays.asList(attributeCodes.toArray(new String[attributeCodes.size()])));

		return jsonb.toJson(processData);
	}

	/**
	 * Setup the process entity used to store task data.
	 *
	 * @param processJson The process data json
	 * @return The updated process entity
	 */
	public String setupProcessBE(String processJson) {

		ProcessQuestions processData = jsonb.fromJson(processJson, ProcessQuestions.class);
		String targetCode = processData.getTargetCode();
		// QDataAskMessage askMessage = processData.getAskMessage();
		List<String> attributeCodes = processData.getAttributeCodes();

		// init entity and force the realm
		String processEntityCode = "QBE_" + targetCode.substring(4);
		log.info("Creating Process Entity " + processEntityCode + "...");

		BaseEntity processEntity = new BaseEntity(processEntityCode, "QuestionBE");
		processEntity.setRealm(userToken.getProductCode());

		BaseEntity target = beUtils.getBaseEntity(targetCode);

		log.info("Found " + attributeCodes.size() + " active attributes in asks");

		// add an entityAttribute to process entity for each attribute
		for (String code : attributeCodes) {

			// check for existing attribute in target
			EntityAttribute ea = target.findEntityAttribute(code).orElseGet(() -> {

				// otherwise create new attribute
				Attribute attribute = qwandaUtils.getAttribute(code);
				Object value = null;
				// default toggles to false
				String className = attribute.getDataType().getClassName();
				if (className.contains("Boolean") || className.contains("bool"))
					value = false;

				return new EntityAttribute(processEntity, attribute, 1.0, value);
			});

			processEntity.addAttribute(ea);
		}

		log.info("ProcessBE contains " + processEntity.getBaseEntityAttributes().size() + " entity attributes");
		processData.setProcessEntity(processEntity);

		// TODO, until cacheUtils supprots BEs
		CacheUtils.putObject(userToken.getProductCode(), processEntityCode, processEntity);

		return jsonb.toJson(processData);
	}

}
