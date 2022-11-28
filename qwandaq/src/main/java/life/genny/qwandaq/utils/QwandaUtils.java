package life.genny.qwandaq.utils;

import static life.genny.qwandaq.attribute.Attribute.PRI_CODE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.NoResultException;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.Definition;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.graphql.ProcessData;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataAttributeMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.validation.Validation;

/**
 * A utility class to assist in any Qwanda Engine Question
 * and Answer operations.
 * 
 * @author Jasper Robison
 */
@ApplicationScoped
public class QwandaUtils {

	public static final String[] ACCEPTED_PREFIXES = { Prefix.PRI, Prefix.LNK };
	public static final String[] EXCLUDED_ATTRIBUTES = { Attribute.PRI_SUBMIT, Attribute.EVT_SUBMIT, Attribute.EVT_CANCEL, Attribute.EVT_NEXT };

	static final Logger log = Logger.getLogger(QwandaUtils.class);

	static Jsonb jsonb = JsonbBuilder.create();

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	DefUtils defUtils;

	@Inject
	SearchUtils searchUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	UserToken userToken;

	public QwandaUtils() {
	}

	// private static DataType DTT_EVENT;

	public static String ASK_CACHE_KEY_FORMAT = "%s:ASKS";

	@PostConstruct
	private void init() {
		// Attribute submit = getAttribute("EVT_SUBMIT");
		// if (submit == null) {
		// 	log.error("Could not find Attribute: EVT_SUBMIT");
		// 	return;
		// }
		// DTT_EVENT = submit.getDataType();
	}

	/**
	 * Cache an ask for a processId and questionCode combination.
	 * 
	 * @param processData The processData to cache for
	 * @param asks        ask to cache
	 */
	public void cacheAsks(ProcessData processData, List<Ask> asks) {

		String key = String.format(QwandaUtils.ASK_CACHE_KEY_FORMAT, processData.getProcessId());
		CacheUtils.putObject(userToken.getProductCode(), key, asks.toArray());
		log.info("Asks cached for " + processData.getProcessId());
	}

	/**
	 * Fetch an ask from cache for a processId and questionCode combination.
	 * 
	 * @param processData The processData to fetch for
	 * @return
	 */
	public List<Ask> fetchAsks(ProcessData processData) {

		String key = String.format(QwandaUtils.ASK_CACHE_KEY_FORMAT, processData.getProcessId());
		Ask[] asks = CacheUtils.getObject(userToken.getProductCode(), key, Ask[].class);
		return Arrays.asList(asks);
	}

	public Attribute saveAttribute(final Attribute attribute) {
		return saveAttribute(userToken.getProductCode(), attribute);
	}

	public Attribute saveAttribute(final String productCode, final Attribute attribute) {
		Attribute existingAttrib = CacheUtils.getObject(productCode, attribute.getCode(), Attribute.class);

		if (existingAttrib != null) {
			if (CommonUtils.compare(attribute, existingAttrib)) {
				log.warn("Attribute already exists with same fields: " + existingAttrib.getCode());
				return existingAttrib;
			}
			log.info("Updating existing attribute!: " + existingAttrib.getCode());
		}

		CacheUtils.putObject(productCode, attribute.getCode(), attribute);
		attribute.setRealm(productCode);
		databaseUtils.saveAttribute(attribute);

		return CacheUtils.getObject(productCode, attribute.getCode(), Attribute.class);
	}

	/**
	 * Get an attribute from the in memory attribute map. If productCode not found,
	 * it
	 * will try to fetch attributes from the DB.
	 *
	 * @param attributeCode the code of the attribute to get
	 * @return Attribute
	 */
	public Attribute getAttribute(final String attributeCode) {
		return getAttribute(userToken.getProductCode(), attributeCode);
	}

	/**
	 * Get an attribute from the in memory attribute map. If productCode not found,
	 * it
	 * will try to fetch attributes from the DB.
	 *
	 * @param attributeCode the code of the attribute to get
	 * @param productCode   the product code
	 * @return Attribute
	 */
	public Attribute getAttribute(final String productCode, final String attributeCode) {
		Attribute attribute = CacheUtils.getObject(productCode, attributeCode, Attribute.class);

		if (attribute == null) {
			log.error("Could not find attribute " + attributeCode + " in cache: " + productCode);
			loadAllAttributesIntoCache(productCode);
		}

		attribute = CacheUtils.getObject(productCode, attributeCode, Attribute.class);

		if (attribute == null) {
			throw new ItemNotFoundException(productCode, attributeCode);
		}

		return attribute;
	}

	/**
	 * Load all attributes into the cache from the database.
	 *
	 * @param productCode The product of the attributes to initialize
	 */
	public void loadAllAttributesIntoCache(String productCode) {

		if (StringUtils.isBlank(productCode)) {
			log.error("RECEIVED NULL PRODUCT CODE WHILE LOADING ATTRIBUTES INTO CACHE!");
		}

		Long attributeCount = databaseUtils.countAttributes(productCode);
		final Integer CHUNK_LOAD_SIZE = 200;

		final int TOTAL_PAGES = (int) Math.ceil(attributeCount / CHUNK_LOAD_SIZE);

		Long totalAttribsCached = 0L;

		log.info("About to load all attributes for productCode " + productCode);
		log.info("Found " + attributeCount + " attributes");

		CacheUtils.putObject(productCode, "ATTRIBUTE_PAGES", TOTAL_PAGES);

		try {
			for (int currentPage = 0; currentPage < TOTAL_PAGES + 1; currentPage++) {

				QDataAttributeMessage msg = new QDataAttributeMessage();

				int attributesLoaded = currentPage * CHUNK_LOAD_SIZE;

				// Correctly determine how many more attributes we need to load in
				int nextLoad = CHUNK_LOAD_SIZE;
				if (attributeCount - attributesLoaded < CHUNK_LOAD_SIZE) {
					nextLoad = (int) (attributeCount - attributesLoaded);
				}

				List<Attribute> attributeList = databaseUtils.findAttributes(productCode, attributesLoaded, nextLoad,
						null);
				long lastMemory = PerformanceUtils.getMemoryUsage("MEGABYTES");

				log.debug("Loading in page " + currentPage + " of " + TOTAL_PAGES + " containing " + nextLoad
						+ " attributes");
				log.debug("Current memory usage: " + lastMemory + "MB");

				for (Attribute attribute : attributeList) {
					String key = attribute.getCode();
					CacheUtils.putObject(productCode, key, attribute);
					totalAttribsCached++;
				}
				long currentMemory = PerformanceUtils.getMemoryUsage(PerformanceUtils.MemoryMeasurement.MEGABYTES);
				long memoryUsed = currentMemory - lastMemory;

				log.trace("Post load memory usage: " + currentMemory + "MB");
				log.trace("Used up: " + memoryUsed + "MB");
				log.trace("Percentage: " + PerformanceUtils.getPercentMemoryUsed() * 100f);
				log.trace("============================");
				// NOTE: Warning, this may cause OOM errors.
				msg.add(attributeList);

				if (attributeList.size() > 0) {
					log.debug("Start AttributeID:"
							+ attributeList.get(0).getId() + ", End AttributeID:"
							+ attributeList.get(attributeList.size() - 1).getId());
				}

				CacheUtils.putObject(productCode, "ATTRIBUTES_P" + currentPage, msg);
			}

			log.debug("Cached " + totalAttribsCached + " attributes");
		} catch (Exception e) {
			log.error("Error loading attributes for productCode: " + productCode);
			e.printStackTrace();
		}
	}

	/**
	 * Create a button event.
	 *
	 * @param code
	 * @param name
	 * @return
	 */
	public Attribute createButtonEvent(String code, final String name) {
		if (!code.startsWith(Prefix.EVT))
			code = Prefix.EVT.concat(code);
		code = code.toUpperCase();
		DataType DTT_EVENT = getAttribute(userToken.getProductCode(), Attribute.EVT_SUBMIT).getDataType();
		return new Attribute(code, name.concat(" Event"), DTT_EVENT);
	}

	/**
	 * Generate an ask for a question, the
	 * source and the target. This operation is recursive if the
	 * question is a group.
	 *
	 * @param question The question to generate from
	 * @param source The source entity
	 * @param target The target entity
	 * @return The generated Ask
	 */
	public Ask generateAskFromQuestion(final Question question, final BaseEntity source, final BaseEntity target, ReqConfig requirementsConfig) {
		if (question == null)
			throw new NullParameterException("question");
		if (source == null)
			throw new NullParameterException("source");
		if (target == null)
			throw new NullParameterException("target");

		String productCode = userToken.getProductCode();
		// init new parent ask
		Ask ask = new Ask(question, source.getCode(), target.getCode());
		ask.setRealm(productCode);

		if (Question.QUE_BASEENTITY_GRP.equals(question.getCode()))
			return generateAskGroupUsingBaseEntity(target);

		// override with Attribute icon if question icon is null
		Attribute attribute = question.getAttribute();
		if (attribute != null && attribute.getIcon() != null) {
			if (question.getIcon() == null)
				question.setIcon(attribute.getIcon());
		}

		// check if it is a question group
		if (question.getAttributeCode().startsWith(Attribute.QQQ_QUESTION_GROUP)) {

			log.info("[*] Parent Question: " + question.getCode());

			// groups always readonly
			ask.setReadonly(true);

			// fetch questionQuestions from the DB
			List<QuestionQuestion> questionQuestions = databaseUtils.findQuestionQuestionsBySourceCode(productCode,
					question.getCode());

			// recursively operate on child questions
			for (QuestionQuestion questionQuestion : questionQuestions) {

				log.info("   [-] Found Child Question in database:  " + questionQuestion.getSourceCode() + ":"
						+ questionQuestion.getTargetCode());
				if(requirementsConfig != null) {
					if(!questionQuestion.requirementsMet(requirementsConfig)) { // For now all caps are needed. I'll make this more comprehensive later
						continue;
					}
				}
				Ask child = generateAskFromQuestionCode(questionQuestion.getTargetCode(), source, target);

				// Do not include PRI_SUBMIT
				if (Attribute.PRI_SUBMIT.equals(child.getQuestion().getAttribute().getCode())) {
					continue;
				}

				// set boolean fields
				child.setMandatory(questionQuestion.getMandatory());
				child.setDisabled(questionQuestion.getDisabled());
				child.setHidden(questionQuestion.getHidden());
				child.setDisabled(questionQuestion.getDisabled());
				child.setReadonly(questionQuestion.getReadonly());

				// override with QuestionQuestion icon if exists
				if (questionQuestion.getIcon() != null) {
					child.getQuestion().setIcon(questionQuestion.getIcon());
				}
				log.info("Adding: " + child.getQuestionCode());
				ask.add(child);
			}
		} else {
			ask.setReadonly(question.getReadonly());
		}

		return ask;
	}

	public Ask generateAskFromQuestion(final Question question, final BaseEntity source, final BaseEntity target) {
		return generateAskFromQuestion(question, source, target, null);
	}

	/**
	 * Generate an ask for a question using the question code, the
	 * source and the target. This operation is recursive if the
	 * question is a group.
	 *
	 * @param code   The code of the question
	 * @param source The source entity
	 * @param target The target entity
	 * @return The generated Ask
	 */
	public Ask generateAskFromQuestionCode(final String code, final BaseEntity source, final BaseEntity target, ReqConfig requirementsConfig) {

		if (code == null)
			throw new NullParameterException("code");
		// don't need to check source, target since they are checked in generateAskFromQuestion

		// if the code is QUE_BASEENTITY_GRP then display all the attributes
		if (Question.QUE_BASEENTITY_GRP.equals(code)) {
			return generateAskGroupUsingBaseEntity(target);
		}

		String productCode = userToken.getProductCode();
		log.debug("Fetching Question: " + code);

		// find the question in the database
		Question question;
		try {
			question = databaseUtils.findQuestionByCode(productCode, code);
		} catch (NoResultException e) {
			throw new ItemNotFoundException(code, e);
		}

		return generateAskFromQuestion(question, source, target, requirementsConfig);
	}

	public Ask generateAskFromQuestionCode(final String code, final BaseEntity source, final BaseEntity target) {
		return generateAskFromQuestionCode(code, source, target, null);
	}

	/**
	 * Recursively set the processId down through an ask tree.
	 *
	 * @param ask       The ask to traverse
	 * @param processId The processId to set
	 */
	public void recursivelySetProcessId(Ask ask, String processId) {

		ask.setProcessId(processId);

		if (ask.getChildAsks() != null) {
			for (Ask child : ask.getChildAsks()) {
				recursivelySetProcessId(child, processId);
			}
		}
	}

	/**
	 * Perform basic code checks on attribute code.
	 *
	 * @param code An attribute code
	 * @return boolean
	 */
	public static boolean attributeCodeMeetsBasicRequirements(String code) {

		if (!Arrays.asList(ACCEPTED_PREFIXES).contains(code.substring(0, 4)))
			return false;
		else if (Arrays.asList(EXCLUDED_ATTRIBUTES).contains(code))
			return false;

		return true;
	}

	/**
	 * @param asks
	 */
	public static Map<String, Ask> buildAskFlatMap(List<Ask> asks) {
		return buildAskFlatMap(new HashMap<String, Ask>(), asks);
	}

	/**
	 * @param map
	 * @param asks
	 */
	public static Map<String, Ask> buildAskFlatMap(Map<String, Ask> map, List<Ask> asks) {

		if (asks == null)
			return map;

		for (Ask ask : asks) {
			if (ask.hasChildren())
				buildAskFlatMap(map, ask.getChildAsks());
			else
				map.put(ask.getQuestion().getAttribute().getCode(), ask);
		}

		return map;
	}

	/**
	 * @param target
	 * @param dependencies
	 * @return
	 */
	public boolean hasDepsAnswered(BaseEntity target, String[] dependencies) {
		target.getBaseEntityAttributes().stream()
				.forEach(ea -> log.info(ea.getAttributeCode() + " = " + ea.getValue()));
		for (String d : dependencies) {
			if (!target.getValue(d).isPresent())
				return false;
		}
		return true;
	}

	/**
	 * @param asks
	 * @param target
	 * @param definition
	 * @param flatMapAsks
	 * @return
	 */
	public Map<String, Ask> updateDependentAsks(BaseEntity target, Definition definition, Map<String, Ask> flatMapAsks) {

		List<EntityAttribute> dependentAsks = definition.findPrefixEntityAttributes(Prefix.DEP);

		for (EntityAttribute dep : dependentAsks) {
			String attributeCode = StringUtils.removeStart(dep.getAttributeCode(), Prefix.DEP);
			Ask targetAsk = flatMapAsks.get(attributeCode);
			if (targetAsk == null) {
				continue;
			}

			String[] dependencies = CommonUtils.cleanUpAttributeValue(dep.getValueString()).split(",");

			boolean depsAnswered = hasDepsAnswered(target, dependencies);
			targetAsk.setDisabled(!depsAnswered);
			targetAsk.setHidden(!depsAnswered);
		}
		
		return flatMapAsks;
	}

	/**
	 * Check if all Ask mandatory fields are answered for a BaseEntity.
	 *
	 * @param asks        The ask to check
	 * @param baseEntity The BaseEntity to check against
	 * @return Boolean
	 */
	public static Boolean mandatoryFieldsAreAnswered(Map<String, Ask> map, BaseEntity baseEntity) {

		// find all the mandatory booleans
		Boolean answered = true;

		// iterate entity attributes to check which have been answered
		// for (EntityAttribute ea : baseEntity.getBaseEntityAttributes()) {
		for (Ask ask : map.values()) {

			String attributeCode = ask.getQuestion().getAttribute().getCode();

			Boolean mandatory = ask.getMandatory();
			Boolean readonly = ask.getReadonly();

			String value = baseEntity.getValueAsString(attributeCode);

			// if any are blank, mandatory and non-readonly, then task is not complete
			if ((mandatory && !readonly) && StringUtils.isBlank(value))
				answered = false;

			String resultLine = (mandatory ? "[M]" : "[O]") + " : " + attributeCode + " : " + value;
			log.info("===> " + resultLine);
		}

		log.info("Mandatory fields are " + (answered ? "ALL" : "not") + " complete");

		return answered;
	}

	/**
	 * Fill the mandatory map using recursion.
	 *
	 * @param map The map to fill
	 * @param ask The ask to traverse
	 * @return The filled map
	 */
	public Map<String, Boolean> recursivelyFillMandatoryMap(Map<String, Boolean> map, Ask ask) {

		// add current ask attribute code to map
		map.put(ask.getQuestion().getAttribute().getCode(), ask.getMandatory());

		// ensure child asks is not null
		if (ask.getChildAsks() == null) {
			return map;
		}

		// recursively add child ask attribute codes
		for (Ask child : ask.getChildAsks()) {
			map = recursivelyFillMandatoryMap(map, child);
		}

		return map;
	}

	/**
	 * Fill the flat set of asks using recursion.
	 * 
	 * @param set The set to fill
	 * @param ask The ask to traverse
	 * @return The filled set
	 */
	public Set<Ask> recursivelyFillFlatSet(Set<Ask> set, Ask ask) {

		String code = ask.getQuestion().getAttribute().getCode();
		// add current ask attribute code to map
		if (!Arrays.asList(ACCEPTED_PREFIXES).contains(code.substring(0, 4))) {
			log.debugf("Prefix %s not in accepted list", code.substring(0, 4));
		} else if (Arrays.asList(EXCLUDED_ATTRIBUTES).contains(code)) {
			log.debugf("Attribute %s in exclude list", code);
		} else if (ask.getReadonly()) {
			log.debugf("Ask %s is set to readonly", ask.getQuestion().getCode());
		} else {
			set.add(ask);
		}

		// ensure child asks is not null
		if (ask.hasChildren()) {
			return set;
		}

		// recursively add child ask attribute codes
		for (Ask child : ask.getChildAsks()) {
			set = recursivelyFillFlatSet(set, child);
		}

		return set;
	}

	/**
	 * Save process data to cache.
	 * 
	 * @param processData The data to save
	 */
	public void storeProcessData(ProcessData processData) {

		String productCode = userToken.getProductCode();
		String key = String.format("%s:PROCESS_DATA", processData.getProcessId());

		CacheUtils.putObject(productCode, key, processData);
		log.infof("ProcessData cached to %s", key);
	}

	/**
	 * clear process data from cache.
	 * 
	 * @param processId The id of the data to clear
	 */
	public void clearProcessData(String processId) {

		String productCode = userToken.getProductCode();
		String key = String.format("%s:PROCESS_DATA", processId);

		CacheUtils.removeEntry(productCode, key);
		log.infof("ProcessData removed from cache: %s", key);
	}

	/**
	 * Fetch process data from cache.
	 * 
	 * @param processId The id of the data to fetch
	 * @return The saved data
	 */
	public ProcessData fetchProcessData(String processId) {

		String productCode = userToken.getProductCode();
		String key = String.format("%s:PROCESS_DATA", processId);

		return CacheUtils.getObject(productCode, key, ProcessData.class);
	}

	/**
	 * Setup the process entity used to store task data.
	 *
	 * @param processData The process data
	 * @return The updated process entity
	 */
	public BaseEntity generateProcessEntity(ProcessData processData) {

		String targetCode = processData.getTargetCode();
		BaseEntity target = beUtils.getBaseEntity(targetCode);

		// init entity and force the realm
		log.info("Creating Process Entity " + processData.getProcessEntityCode() + "...");
		BaseEntity processEntity = new BaseEntity(processData.getProcessEntityCode(), target.getName());
		processEntity.setRealm(userToken.getProductCode());

		List<String> attributeCodes = processData.getAttributeCodes();
		log.info("Found " + attributeCodes.size() + " active attributes in asks");

		// add an entityAttribute to process entity for each attribute
		for (String code : attributeCodes) {

			// check for existing attribute in target
			EntityAttribute ea = target.findEntityAttribute(code).orElseGet(() -> {

				// otherwise create new attribute
				Attribute attribute = getAttribute(code);
				Object value = null;
				// default toggles to false
				String className = attribute.getDataType().getClassName();
				if (className.contains("Boolean") || className.contains("bool"))
					value = false;

				return new EntityAttribute(processEntity, attribute, 1.0, value);
			});

			processEntity.addAttribute(ea);
		}

		// now apply all incoming answers
		processData.getAnswers().stream().forEach(answer -> {
			// ensure the attribute is set
			String attributeCode = answer.getAttributeCode();
			Attribute attribute = getAttribute(attributeCode);
			answer.setAttribute(attribute);
			// add answer to entity
			processEntity.addAnswer(answer);
		});

		log.info("ProcessBE contains " + processEntity.getBaseEntityAttributes().size() + " entity attributes");

		return processEntity;
	}

	/**
	 * Save an {@link Answer} object.
	 *
	 * @param answer The answer to save
	 * @return The target BaseEntity
	 */
	public BaseEntity saveAnswer(Answer answer) {

		List<BaseEntity> targets = saveAnswers(Arrays.asList(answer));

		if (targets != null && targets.size() > 0) {
			return targets.get(0);
		}

		return null;
	}

	/**
	 * Save a List of {@link Answer} objects.
	 *
	 * @param answers The list of answers to save
	 * @return The target BaseEntitys
	 */
	public List<BaseEntity> saveAnswers(List<Answer> answers) {

		List<BaseEntity> targets = new ArrayList<>();

		// sort answers into target BaseEntitys
		Map<String, List<Answer>> answersPerTargetCodeMap = answers.stream()
				.collect(Collectors.groupingBy(Answer::getTargetCode));

		for (String targetCode : answersPerTargetCodeMap.keySet()) {

			// fetch target and target DEF
			BaseEntity target = beUtils.getBaseEntity(targetCode);
			Definition definition = defUtils.getDEF(target);

			// filter Non-valid answers using def
			List<Answer> group = answersPerTargetCodeMap.get(targetCode);
			List<Answer> validAnswers = group.stream()
					.filter(item -> defUtils.answerValidForDEF(definition, item))
					.collect(Collectors.toList());

			// update target using valid answers
			for (Answer answer : validAnswers) {
				Attribute attribute = getAttribute(answer.getAttributeCode());
				answer.setAttribute(attribute);
				try {
					target.addAnswer(answer);
				} catch (BadDataException e) {
					log.error(e);
				}
			}

			// update target in the cache and DB
			beUtils.updateBaseEntity(target);
			targets.add(target);
		}

		return targets;
	}

	/**
	 * Delete a currently scheduled message via shleemy.
	 *
	 * @param code the code of the schedule message to delete
	 */
	public void deleteSchedule(String code) {

		String uri = GennySettings.shleemyServiceUrl() + "/api/schedule/code/" + code;
		HttpUtils.delete(uri, userToken);
	}

	/**
	 * Generate Question group for a baseEntity
	 *
	 * @param baseEntity the baseEntity to create for
	 * @return Ask
	 */
	public Ask generateAskGroupUsingBaseEntity(BaseEntity baseEntity) {

		// grab def entity
		Definition definition = defUtils.getDEF(baseEntity);

		String sourceCode = userToken.getUserCode();
		String targetCode = baseEntity.getCode();

		// create GRP ask
		Attribute questionAttribute = getAttribute(Attribute.QQQ_QUESTION_GROUP);
		Question question = new Question(Question.QUE_BASEENTITY_GRP,
				"Edit " + targetCode + " : " + baseEntity.getName(),
				questionAttribute);
		Ask ask = new Ask(question, sourceCode, targetCode);

		List<Ask> childAsks = new ArrayList<>();
		QDataBaseEntityMessage entityMessage = new QDataBaseEntityMessage();
		entityMessage.setToken(userToken.getToken());
		entityMessage.setReplace(true);

		// create a child ask for every valid atribute
		definition.getBaseEntityAttributes().stream()
				.filter(ea -> ea.getAttributeCode().startsWith(Prefix.ATT))
				.forEach((ea) -> {
					String attributeCode = StringUtils.removeStart(ea.getAttributeCode(), Prefix.ATT);
					Attribute attribute = getAttributeByBaseEntityAndCode(baseEntity, attributeCode);

					String questionCode = Prefix.QUE
							+ StringUtils.removeStart(StringUtils.removeStart(attribute.getCode(),
									Prefix.PRI), Prefix.LNK);

					Question childQues = new Question(questionCode, attribute.getName(), attribute);
					Ask childAsk = new Ask(childQues, sourceCode, targetCode);

					childAsks.add(childAsk);
				});

		// set child asks
		ask.setChildAsks(childAsks.toArray(new Ask[childAsks.size()]));

		return ask;
	}

	/**
	 * Update the status of the disabled field for an Ask on the web.
	 *
	 * @param ask      the ask to update
	 * @param disabled the disabled status to set
	 */
	public void updateAskDisabled(Ask ask, Boolean disabled) {

		ask.setDisabled(disabled);

		QDataAskMessage askMsg = new QDataAskMessage(ask);
		askMsg.setToken(userToken.getToken());
		askMsg.setReplace(true);
		String json = jsonb.toJson(askMsg);
		KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, json);
	}

	/**
	 * Check if a baseentity satisfies a definitions uniqueness checks.
	 * 
	 * @param definition The definition to check against
	 * @param answer     An incoming answer
	 * @param targets    The target entities to check, usually processEntity and
	 *                   original target
	 * @return Boolean
	 */
	public Boolean isDuplicate(Definition definition, Answer answer, BaseEntity... targets) {

		// Check if attribute code exists as a UNQ for the DEF
		List<EntityAttribute> uniques = definition.findPrefixEntityAttributes("UNQ");
		log.info("Found " + uniques.size() + " UNQ attributes");

		String prefix = definition.getValueAsString(Attribute.PRI_PREFIX);

		for (EntityAttribute entityAttribute : uniques) {
			// fetch list of unique code combo
			List<String> codes = beUtils.getBaseEntityCodeArrayFromLinkAttribute(definition,
					entityAttribute.getAttribute().getCode());

			// skip if no value found
			if (codes == null)
				continue;

			SearchEntity searchEntity = new SearchEntity("SBE_COUNT_UNIQUE_PAIRS", "Count Unique Pairs")
					.add(new Filter(PRI_CODE, Operator.LIKE, prefix + "_%"))
					.setPageStart(0)
					.setPageSize(1);

			// ensure we are not counting any of our targets
			for (BaseEntity target : targets) {
				log.info("adding not equal " + target.getCode());
				searchEntity.add(new Filter(PRI_CODE, Operator.NOT_EQUALS, target.getCode()));
			}

			for (String code : codes) {

				String value = null;
				if (answer != null && answer.getAttributeCode().equals(code)) {
					value = answer.getValue();
				}

				if (value == null) {
					// get the first value in array of target
					for (BaseEntity target : targets) {

						log.info("TARGET = " + target.getCode() + ", EMAIL = " + target.getValueAsString(Attribute.PRI_EMAIL));

						if (target.containsEntityAttribute(code)) {
							value = target.getValueAsString(code);
							if (value.isEmpty())
								value = null;
							if (value != null)
								break;
						}
					}
				}

				// value has not yet been answered, not a duplicate
				if (value == null)
					return false;

				// clean it up if it is a code
				if (value.contains("[") && value.contains("]"))
					value = CommonUtils.cleanUpAttributeValue(value);

				log.info("Adding unique filter: " + code + " like " + value);
				searchEntity.add(new Filter(code, Operator.LIKE, "%" + value + "%"));
			}

			// set realm and count results
			searchEntity.setRealm(userToken.getProductCode());
			Long count = searchUtils.countBaseEntitys(searchEntity);
			log.infof("Found %s entities", count);
			if (count != 0)
				return true;
		}

		return false;
	}

	/**
	 * Send a baseentity with a feedback message to be displayed.
	 * 
	 * @param parentCode    The parentCode of the question group
	 * @param questionCode  The questionCode of the bad answer
	 * @param attributeCode The attributeCode of the bad answer
	 * @param feedback      The feedback to provide the user
	 */
	public void sendAttributeErrorMessage(String parentCode, String questionCode, String attributeCode,
			String feedback) {

		// send a special FIELDMSG
		JsonObject json = Json.createObjectBuilder()
				.add("token", userToken.getToken())
				.add("cmd_type", "FIELDMSG")
				.add("msg_type", "CMD_MSG")
				.add("code", parentCode)
				.add("attributeCode", attributeCode)
				.add("questionCode", questionCode)
				.add("message", Json.createObjectBuilder()
						.add("value", "This field must be unique and not have already been selected"))
				.build();

		// send to commands topic
		KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, json.toString());
		log.info("Sent error message to frontend : " + json.toString());
	}

	/**
	 * Return attribute relied on base entity object and attribute code
	 * 
	 * @param baseEntity    Base entity
	 * @param attributeCode Attribute code
	 * @return Return attribute object
	 */
	public Attribute getAttributeByBaseEntityAndCode(BaseEntity baseEntity, String attributeCode) {
		Optional<EntityAttribute> baseEA = baseEntity.findEntityAttribute(attributeCode);

		if (baseEA.isPresent()) {
			return baseEA.get().getAttribute();
		}

		Attribute attribute = getAttribute(attributeCode);
		return attribute;
	}

	/**
	 * Check if all validations are met for an attribute and value.
	 * 
	 * 
	 * @param attribute The Attribute of the answer
	 * @param value     The value to check
	 * @return Boolean representing whether the validation conditions have been met
	 */
	public Boolean validationsAreMet(Attribute attribute, String value) {

		DataType dataType = attribute.getDataType();

		// check each validation against value
		for (Validation validation : dataType.getValidationList()) {

			String regex = validation.getRegex();
			boolean regexOk = Pattern.compile(regex).matcher(value).matches();

			if (!regexOk) {
				log.error("Regex FAILED! " + attribute.getCode() + ":" + regex + " ... [" + value + "] "
						+ validation.getErrormsg());
				return false;
			}

		}
		return true;
	}
}
