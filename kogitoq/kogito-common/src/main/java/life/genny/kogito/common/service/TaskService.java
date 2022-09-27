package life.genny.kogito.common.service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.DebugException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.graphql.ProcessData;
import life.genny.qwandaq.message.QBulkMessage;
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
	 * @param asks         The ask to cache
	 */
	public void cacheBulkMessage(ProcessData processData, QBulkMessage msg) {

		String key = String.format(ASK_CACHE_KEY_FORMAT, processData.getProcessId(), "MSG");
		CacheUtils.putObject(userToken.getProductCode(), key, msg);
	}

	/**
	 * Fetch an ask from cache for a processId and questionCode combination.
	 * 
	 * @param processData The processData to fetch for
	 * @return
	 */
	public QBulkMessage fetchBulkMessage(ProcessData processData) {

		String key = String.format(ASK_CACHE_KEY_FORMAT, processData.getProcessId(), "MSG");
		QBulkMessage msg = CacheUtils.getObject(userToken.getProductCode(), key, QBulkMessage.class);

		if (msg == null)
			msg = generateBulkMessage(processData);

		return msg;
	}

	/**
	 * Generate the asks and save them to the cache
	 * 
	 * @param processData The process data
	 */
	public QBulkMessage generateBulkMessage(ProcessData processData) {

		String questionCode = processData.getQuestionCode();
		String sourceCode = processData.getSourceCode();
		String targetCode = processData.getTargetCode();
		String processId = processData.getProcessId();

		BaseEntity source = beUtils.getBaseEntity(sourceCode);
		BaseEntity target = beUtils.getBaseEntity(targetCode);

		log.info("Generating asks -> " + questionCode + ":" + source.getCode() + ":" + target.getCode());

		QBulkMessage msg = new QBulkMessage();

		if (questionCode != null) {
			// fetch question from DB
			Ask ask = qwandaUtils.generateAskFromQuestionCode(questionCode, source, target);
			Ask events = createEvents(processData.getEvents(), sourceCode, targetCode);
			ask.addChildAsk(events);
			msg.add(ask);
		}

		// find first level PCM
		traversePCM(processData.getPcmCode(), source, target, msg);

		for (Ask child : msg.getAsks())
			qwandaUtils.recursivelySetProcessId(child, processId);

		// cache them for fast fetching
		cacheBulkMessage(processData, msg);

		return msg;
	}

	/**
	 * Traverse a PCM looking for a non-readonly question.
	 * 
	 * @param pcm
	 * @param source
	 * @param target
	 * @return
	 */
	public Boolean traversePCM(String pcmCode, BaseEntity source, BaseEntity target, QBulkMessage msg) {

		if ("PCM_DASHBOARD".equals(pcmCode))
			return false;
		// add pcm to bulk message
		BaseEntity pcm = beUtils.getBaseEntity(pcmCode);
		msg.add(pcm);

		Ask ask = null;
		String questionCode = pcm.getValueAsString("PRI_QUESTION_CODE");
		if (questionCode == null) {
			log.warn("Question Code is null for " + pcm.getCode());
		} else {
			// add ask to bulk message
			ask = qwandaUtils.generateAskFromQuestionCode(questionCode, source, target);
			msg.add(ask);
		}

		Boolean expecting = false;

		// iterate fields recursively
		List<EntityAttribute> locations = pcm.getBaseEntityAttributes().stream()
				.filter(ea -> ea.getAttribute() != null && ea.getAttribute().getCode() != null)
				.filter(ea -> ea.getAttribute().getCode().startsWith("PRI_LOC"))
				.collect(Collectors.toList());

		for (EntityAttribute entityAttribute : locations) {

			// recursively check PCM fields
			String value = entityAttribute.getAsString();
			if (value.startsWith("PCM_")) {
				if (traversePCM(value, source, target, msg))
					expecting = true;
				continue;
			}

			// cannot check asks if not existant
			if (ask == null)
				continue;

			// find appropriate ask (could use map instead)
			Optional<Ask> child = Stream.of(ask.getChildAsks())
					.filter(a -> a.getAttributeCode().equals(value))
					.findFirst();
			if (child.isEmpty()) {
				log.warn("No corresponding child for pcm location " + value);
				continue;
			}

			// return true if answer expected
			if (!child.get().getReadonly())
				expecting = true;
		}

		return expecting;
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
	 * @param processData
	 * @return
	 */
	public Boolean isExpectingAnswers(ProcessData processData) {

		QBulkMessage msg = fetchBulkMessage(processData);

		for (Ask ask : msg.getAsks())
			if (containsNonReadonly(ask))
				return true;

		return false;
	}

	/**
	 * @param ask
	 * @return
	 */
	public Boolean containsNonReadonly(Ask ask) {

		if (ask.getReadonly())
			return true;

		if (ask.getChildAsks() != null) {
			for (Ask child : ask.getChildAsks())
				if (containsNonReadonly(child))
					return true;
		}

		return false;
	}

	/**
	 * Find the involved attribute codes.
	 * 
	 * @param processData The process data json
	 * @return process json
	 */
	public ProcessData findAttributeCodes(ProcessData processData) {

		QBulkMessage msg = fetchBulkMessage(processData);

		// find all allowed attribute codes
		Set<String> attributeCodes = new HashSet<>();
		for (Ask ask : msg.getAsks())
			attributeCodes.addAll(qwandaUtils.recursivelyGetAttributeCodes(attributeCodes, ask));

		processData.setAttributeCodes(Arrays.asList(attributeCodes.toArray(new String[attributeCodes.size()])));

		return processData;
	}

}
