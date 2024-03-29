package life.genny.qwandaq.utils;

import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.attribute.HAttribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.datatype.capability.core.CapabilitySet;
import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.Definition;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.exception.runtime.DebugException;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.exception.runtime.QwandaException;
import life.genny.qwandaq.graphql.ProcessData;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataAttributeMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.validation.Validation;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static life.genny.qwandaq.attribute.Attribute.PRI_CODE;
import static life.genny.qwandaq.entity.search.SearchEntity.SBE_COUNT_UNIQUE_PAIRS;

/**
 * A utility class to assist in any Qwanda Engine Question
 * and Answer operations.
 *
 * @author Jasper Robison
 */
@ApplicationScoped
public class QwandaUtils {

	public static final String[] ACCEPTED_PREFIXES = { Prefix.PRI_, Prefix.LNK_ };
	public static final String[] EXCLUDED_ATTRIBUTES = { Attribute.PRI_SUBMIT, Attribute.EVT_SUBMIT,
			Attribute.EVT_CANCEL, Attribute.EVT_NEXT };

	public static String ASK_CACHE_KEY_FORMAT = "%s:ASKS";

	static Jsonb jsonb = JsonbBuilder.create();

	static Logger log = Logger.getLogger(QwandaUtils.class);

	@Inject
	DefUtils defUtils;

	@Inject
	SearchUtils searchUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	EntityAttributeUtils beaUtils;

	@Inject
	QuestionUtils questionUtils;

	@Inject
	CacheManager cm;

	@Inject
	UserToken userToken;

	@Inject
	AttributeUtils attributeUtils;

	public QwandaUtils() {
	}

	/**
	 * Cache an ask for a processId and questionCode combination.
	 *
	 * @param processData The processData to cache for
	 * @param asks        ask to cache
	 */
	public void cacheAsks(ProcessData processData, Set<Ask> asks) {

		String key = String.format(QwandaUtils.ASK_CACHE_KEY_FORMAT, processData.getProcessId());
		cm.putObject(userToken.getProductCode(), key, asks.toArray());
		log.info("Asks cached for " + processData.getProcessId());
	}

	/**
	 * Fetch an ask from cache for a processId and questionCode combination.
	 *
	 * @param processData The processData to fetch for
	 * @return
	 */
	public Set<Ask> fetchAsks(ProcessData processData) {
		String key = String.format(QwandaUtils.ASK_CACHE_KEY_FORMAT, processData.getProcessId());
		Ask[] asksArr = cm.getObject(userToken.getProductCode(), key, Ask[].class);
		return Arrays.stream(asksArr).collect(Collectors.toSet());
	}

	public Attribute saveAttribute(final Attribute attribute) {
		return saveAttribute(userToken.getProductCode(), attribute);
	}

	public Attribute saveAttribute(final String productCode, final Attribute attribute) {
		HAttribute existingAttrib = cm.getObject(productCode, attribute.getCode(), HAttribute.class);

		if (existingAttrib != null) {
			if (CommonUtils.compare(attribute, existingAttrib)) {
				log.warn("Attribute already exists with same fields: " + existingAttrib.getCode());
				return existingAttrib.toAttribute();
			}
			log.info("Updating existing attribute!: " + existingAttrib.getCode());
		}

		cm.putObject(productCode, attribute.getCode(), attribute);
		attribute.setRealm(productCode);
		attributeUtils.saveAttribute(attribute);

		return cm.getObject(productCode, attribute.getCode(), HAttribute.class).toAttribute();
	}

	/**
	 * Get an attribute from the cache
	 *
	 * @param attributeCode the code of the attribute to get
	 * @return Attribute
	 */
	public Attribute getAttribute(final String attributeCode) {
		return getAttribute(userToken.getProductCode(), attributeCode);
	}

	/**
	 * Get an attribute from the cache. If it is missing, system will check the database and
	 * repopulate the cache with it if it exists
	 *
	 * @param attributeCode the code of the attribute to get
	 * @param productCode   the product code
	 * @return Attribute
	 */
	public Attribute getAttribute(final String productCode, final String attributeCode) {
		return attributeUtils.getAttribute(productCode, attributeCode);
	}

	/**
	 * Create a button event.
	 *
	 * @param code
	 * @param name
	 * @return
	 */
	public Attribute createButtonEvent(String code, final String name) {
		if (!code.startsWith(Prefix.EVT_))
			code = Prefix.EVT_.concat(code);
		code = code.toUpperCase();
		DataType DTT_EVENT = attributeUtils.getAttribute(userToken.getProductCode(), Attribute.EVT_SUBMIT, true).getDataType();
		return new Attribute(code, name.concat(" Event"), DTT_EVENT);
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
	public Ask generateAskFromQuestionCode(final String code, final BaseEntity source, final BaseEntity target, final CapabilitySet capSet, ReqConfig requirementsConfig) {

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
		Question question = questionUtils.getQuestionFromQuestionCode(productCode, code);
		Attribute attribute = attributeUtils.getAttribute(productCode, question.getAttributeCode(), true);
		question.setAttribute(attribute);

		return generateAskFromQuestion(question, source, target, capSet, requirementsConfig);
	}

	/**
	 * Generate an ask for a question, the
	 * source and the target. This operation is recursive if the
	 * question is a group.
	 *
	 * @param code   The code of the question
	 * @param source The source entity
	 * @param target The target entity
	 * @return The generated Ask
	 */
	public Ask generateAskFromQuestion(final Question question, final BaseEntity source, final BaseEntity target, final CapabilitySet capSet, ReqConfig requirementsConfig) {
		if (question == null)
			throw new NullParameterException("question");
		if (source == null)
			throw new NullParameterException("source");
		if (target == null)
			throw new NullParameterException("target");

		String productCode = userToken.getProductCode();
		// init new parent ask
		Ask ask = new Ask(question, source.getCode(), target.getCode(), 0.0);
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
		if (question.getAttributeCode() != null && question.getAttributeCode().startsWith(Attribute.QQQ_QUESTION_GROUP)) {

			log.info("[*] Parent Question: " + question.getCode());
			// groups always readonly
			ask.setReadonly(true);
			// fetch questionQuestions from the cache
			Set<QuestionQuestion> questionQuestions = cm.getQuestionQuestionsForParentQuestion(question);
			if (questionQuestions.isEmpty()) {
				log.debugf("No child questions found for the question: [%s,%s]", question.getRealm(), question.getCode());
			} else {
				// recursively operate on child questions
				questionQuestions.forEach(questionQuestion -> {
					log.debug("   [-] Found Child Question:  " + questionQuestion.getParentCode() + ":"
							+ questionQuestion.getChildCode() + ". Weight: " + questionQuestion.getWeight());

					if (!questionQuestion.requirementsMet(capSet, requirementsConfig)) { // For now all caps are needed. I'll make this more comprehensive later
						return;
					}
					Ask child = generateAskFromQuestionCode(questionQuestion.getChildCode(), source, target, capSet, requirementsConfig);

					// Do not include PRI_SUBMIT
					if (Attribute.PRI_SUBMIT.equals(child.getQuestion().getAttribute().getCode())) {
						return;
					}

					// set boolean fields
					child.setMandatory(questionQuestion.getMandatory());
					child.setDisabled(questionQuestion.getDisabled());
					child.setHidden(questionQuestion.getHidden());
					child.setDisabled(questionQuestion.getDisabled());
					child.setReadonly(questionQuestion.getReadonly());
					child.setWeight(questionQuestion.getWeight());

					// override with QuestionQuestion icon if exists
					if (questionQuestion.getIcon() != null) {
						child.getQuestion().setIcon(questionQuestion.getIcon());
					}
					ask.add(child);
				});
			}
		} else {
			ask.setReadonly(question.getReadonly());
		}

		return ask;
	}

	/**
	 * Recursively set the processId and target down through an ask tree.
	 *
	 * @param ask       The ask to traverse
	 * @param processId The processId to set
	 * @param targetCode    The target code to set
	 * @param targetCode    The target code to set
	 */
	public void recursivelySetInformation(Ask ask, String processId, String targetCode) {
		ask.setProcessId(processId);
		ask.setTargetCode(targetCode);
		if (ask.getChildAsks() != null) {
			for (Ask child : ask.getChildAsks()) {
				recursivelySetInformation(child, processId, targetCode);
			}
		}
	}

	/**
	 * Perform basic code checks on attribute code.
	 *
	 * @param code An attribute code
	 * @return <b>True</b> if the code prefix is in {@link QwandaUtils#ACCEPTED_PREFIXES the Accepted Prefixes} and 
	 * 				the code is not in {@link QwandaUtils#EXCLUDED_ATTRIBUTES the Excluded Attributes}. <b>False</b> otherwise
	 */
	public static boolean attributeCodeMeetsBasicRequirements(String code) {
		if(!CommonUtils.isInArray(ACCEPTED_PREFIXES, code.substring(0, 4)))
			return false;
		if(CommonUtils.isInArray(EXCLUDED_ATTRIBUTES, code))
			return false;

		return true;
	}

	/**
	 * @param asks
	 */
	public static Map<String, Ask> buildAskFlatMap(Set<Ask> asks) {
		return buildAskFlatMap(new HashMap<String, Ask>(), asks);
	}

	/**
	 * @param map
	 * @param asks
	 */
	public static Map<String, Ask> buildAskFlatMap(Map<String, Ask> map, Set<Ask> asks) {

		if (asks == null)
			return map;

		for (Ask ask : asks) {
			if (ask.hasChildren())
				buildAskFlatMap(map, ask.getChildAsks());
			else
				map.put(ask.getQuestion().getAttributeCode(), ask);
		}

		return map;
	}

	/**
	 * @param target
	 * @param dependencies
	 * @return
	 */
	public boolean hasDepsAnswered(BaseEntity target, String[] dependencies) {
		for (String d : dependencies) {
			if (!target.getValue(d).isPresent()) {
				return false;
			}
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

		Set<EntityAttribute> dependentAsks = beaUtils.getBaseEntityAttributesForBaseEntityWithAttributeCodePrefix(definition.getRealm(), definition.getCode(), Prefix.DEP_);

		for (EntityAttribute dep : dependentAsks) {
			String attributeCode = StringUtils.removeStart(dep.getAttributeCode(), Prefix.DEP_);
			Ask targetAsk = flatMapAsks.get(attributeCode);
			if (targetAsk == null)
				continue;

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
	 * @param asks       The ask to check
	 * @param baseEntity The BaseEntity to check against
	 * @return Boolean
	 */
	public Boolean mandatoryFieldsAreAnswered(Map<String, Ask> map, BaseEntity baseEntity) {
// find all the mandatory booleans
		Boolean complete = true;

		// iterate asks to see if mandatorys are answered
		for (Ask ask : map.values()) {

			String attributeCode = ask.getQuestion().getAttribute().getCode();
			if (!attributeCodeMeetsBasicRequirements(attributeCode)) {
				continue;
			}

			boolean readonly = ask.getReadonly();
			if (readonly) {
				continue;
			}

			boolean mandatory = ask.getMandatory();
			EntityAttribute entityAttribute = baseEntity.getBaseEntityAttributesMap().get(attributeCode);

			String value = null;
			if (entityAttribute != null) {
				value = entityAttribute.getAsString();
			}

			// if any are blank, mandatory and non-readonly, then task is not complete
			Boolean answered = false;
			if ((mandatory && !readonly)) {
				answered = acceptableAnswer(value);

				// not complete if any mandatories are not answered
				if (!answered) {
					complete = false;
				}
			}

			String resultLine = (mandatory ? "[M]" : "[O]") + " : " + attributeCode + " : " + value;
			log.debug("===> " + resultLine + " (" + answered + ")");
		}

		log.debug("Mandatory fields are " + (complete ? "ALL" : "not") + " complete");

		return complete;
	}

	/**
	 * Is an acceptable answer.
	 * @param value
	 * @return
	 */
	private static boolean acceptableAnswer(String value) {
		if(value == null) {
			return false;
		}
		// block whitespace
		value = value.trim();
		if(StringUtils.isBlank(value)) {
			return false;
		}
		if("null".equalsIgnoreCase(value)) {
			return false;
		}

		return true;
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
		map.put(ask.getQuestion().getAttributeCode(), ask.getMandatory());
		// ensure child asks is not null
		if (ask.getChildAsks() == null)
			return map;
		// recursively add child ask attribute codes
		for (Ask child : ask.getChildAsks())
			map = recursivelyFillMandatoryMap(map, child);

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

		String code = ask.getQuestion().getAttributeCode();
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
		if (ask.hasChildren())
			return set;
		// recursively add child ask attribute codes
		for (Ask child : ask.getChildAsks())
			set = recursivelyFillFlatSet(set, child);

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

		cm.putObject(productCode, key, processData);
		log.tracef("ProcessData cached to %s", key);
	}

	/**
	 * clear process data from cache.
	 *
	 * @param processId The id of the data to clear
	 */
	public void clearProcessData(String processId) {

		String productCode = userToken.getProductCode();
		String key = String.format("%s:PROCESS_DATA", processId);

		cm.removeEntry(productCode, key);
		log.tracef("ProcessData removed from cache: %s", key);
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

		return cm.getObject(productCode, key, ProcessData.class);
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
		log.debug("Creating Process Entity " + processData.getProcessEntityCode() + "...");
		BaseEntity processEntity = new BaseEntity(processData.getProcessEntityCode(), target.getName());
		String productCode = userToken.getProductCode();
		processEntity.setRealm(productCode);

		List<String> attributeCodes = processData.getAttributeCodes();
		log.debug("Found " + attributeCodes.size() + " active attributes in asks");

		// add an entityAttribute to process entity for each attribute
		for (String code : attributeCodes) {

			// check for existing attribute in target
			Attribute attribute;
			EntityAttribute ea;
			try {
				ea = beaUtils.getEntityAttribute(productCode, targetCode, code, true, true);
				attribute = ea.getAttribute();
			} catch (ItemNotFoundException e) {
				attribute = attributeUtils.getAttribute(productCode, code, true);
				// otherwise create new attribute
				ea = new EntityAttribute(processEntity, attribute, 1.0, null);
			}

			if(attribute.getDataType() == null) {
				log.error("[!] Detected Null DataType for attribute: " + attribute.getCode());
				continue;
			}

			String component = attribute.getDataType().getComponent();
			Object value = ea.getValue();

			// default false if flag
			if (value == null && component.contains("flag")) {
				ea.setValue(false);
			}
			processEntity.addAttribute(attribute, ea.getWeight(), ea.getValue());
		}

		// now apply all incoming answers
		processData.getAnswers().forEach(answer -> {
			// ensure the attribute is set
			String attributeCode = answer.getAttributeCode();
			Attribute attribute = attributeUtils.getAttribute(attributeCode, true, true);
			EntityAttribute ea = new EntityAttribute(processEntity, attribute, 1.0, answer.getValue());
			// add answer to entity
			processEntity.addAttribute(ea);
		});

		log.debug("ProcessBE contains " + processEntity.getBaseEntityAttributesMap().size() + " entity attributes");

		return processEntity;
	}

	/**
	 * Save an Answer.
	 *
	 * @param answer The answer to save
	 * @return The updated BaseEntity
	 */
	public BaseEntity saveAnswer(Answer answer) {
		BaseEntity target = beUtils.getBaseEntity(answer.getTargetCode());
		return saveAnswer(answer, target);
	}

	/**
	 * Save an Answer to a target.
	 *
	 * @param answer The answer to save
	 * @param target The target to save to
	 * @return The updated BaseEntity
	 */
	public BaseEntity saveAnswer(Answer answer, BaseEntity target) {
		if (answer == null) {
			throw new NullParameterException("answer");
		}
		if (target == null) {
			throw new NullParameterException("target");
		}
		return saveAnswers(Collections.singleton(answer), target);
	}

	/**
	 * Save a Collection of Answers.
	 *
	 * @param answers The answers to save
	 * @return The updated BaseEntitys
	 */
	public Set<BaseEntity> saveAnswers(Collection<Answer> answers) {
		// find concerned targets
		Set<BaseEntity> targets = answers.stream()
				.map(a -> a.getTargetCode())
				.distinct()
				.map(code -> beUtils.getBaseEntity(code))
				.collect(Collectors.toSet());
		// save answers
		return saveAnswers(answers, targets);
	}

	/**
	 * Save a Collection of Answers.
	 *
	 * @param answers The answers to save
	 * @param target The target to save to
	 * @return The updated BaseEntity
	 */
	public BaseEntity saveAnswers(Collection<Answer> answers, BaseEntity target) {
		Set<BaseEntity> results = saveAnswers(answers, Collections.singleton(target));
		if (results.size() != 1) {
			throw new DebugException("Error returning updated BaseEntity. Results size was " + results.size());
		}
		return results.iterator().next();
	}

	/**
	 * Save a Collection of Answers.
	 *
	 * @param answers The list of answers to save
	 * @return The target BaseEntitys
	 */
	public Set<BaseEntity> saveAnswers(Collection<Answer> answers, Collection<BaseEntity> targets) {
		if (answers == null) {
			throw new NullParameterException("answers");
		}
		if (targets == null) {
			throw new NullParameterException("targets");
		}
		// build map of targets
		Map<String, BaseEntity> targetMap = targets.stream()
				.collect(Collectors.toMap(BaseEntity::getCode, Function.identity(), (prev, next) -> next, HashMap::new));
		// sort answers into target BaseEntitys
		Map<String, Set<Answer>> answersPerTargetCodeMap = answers.stream()
				.collect(Collectors.groupingBy(Answer::getTargetCode, Collectors.toSet()));

		for (String targetCode : answersPerTargetCodeMap.keySet()) {
			// fetch target and target DEF
			BaseEntity target = targetMap.get(targetCode);
			if (target == null) {
				throw new QwandaException("Target " + targetCode + " not in answer target map");
			}
			Definition definition = defUtils.getDEF(target);
			// filter Non-valid answers using def
			Set<Answer> group = answersPerTargetCodeMap.get(targetCode);
			Set<Answer> validAnswers = group.stream()
					.filter(item -> defUtils.answerValidForDEF(definition, item))
					.collect(Collectors.toSet());
			// update target using valid answers
			for (Answer answer : validAnswers) {
				// find the attribute
				String attributeCode = answer.getAttributeCode();
				Attribute attribute = attributeUtils.getAttribute(attributeCode);
				// check if name needs updating
				if (Attribute.PRI_NAME.equals(attributeCode)) {
					String name = answer.getValue();
					log.debug("Updating BaseEntity Name Value -> " + name);
					target.setName(name);
					continue;
				}
				// update the baseentity
				EntityAttribute newEntityAttribute = new EntityAttribute(target, attribute, answer.getWeight(), answer.getValue());
				beaUtils.updateEntityAttribute(newEntityAttribute);
			}
			// update target in the cache and DB
			beUtils.updateBaseEntity(target);
		}

		return targetMap.values().stream().collect(Collectors.toSet());
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
		String sourceCode = userToken.getUserCode();
		String targetCode = baseEntity.getCode();

		String name = baseEntity.getName();
		EntityAttribute entityAttribute = beaUtils.getEntityAttribute(baseEntity.getRealm(), targetCode, Attribute.PRI_NAME, true, true);
		if (entityAttribute != null) {
			String value = entityAttribute.getValueString();
			if (!StringUtils.isBlank(value))
				name = value;
		}

		// create GRP ask
		Attribute questionAttribute = attributeUtils.getAttribute(Attribute.QQQ_QUESTION_GROUP);
		Question question = new Question(Question.QUE_BASEENTITY_GRP,
				"Edit " + baseEntity.getName() + " : " + name,
				questionAttribute);
		Ask ask = new Ask(question, sourceCode, targetCode, 0.0);

		LinkedHashSet<Ask> childAsks = new LinkedHashSet<>();
		QDataBaseEntityMessage entityMessage = new QDataBaseEntityMessage();
		entityMessage.setToken(userToken.getToken());
		entityMessage.setReplace(true);

		// grab def entity
		Definition definition = defUtils.getDEF(baseEntity);
		// create a child ask for every valid attribute
		double weight = 0.0;
		for(EntityAttribute ea : definition.getBaseEntityAttributes()) {
			String attributeCode = ea.getAttributeCode();
			if (!attributeCode.startsWith(Prefix.ATT_)) {
				continue;
			}
			String strippedAttributeCode = StringUtils.removeStart(attributeCode, Prefix.ATT_);
			Attribute attribute = attributeUtils.getAttribute(baseEntity.getRealm(), strippedAttributeCode, true);

					String questionCode = Prefix.QUE_
							+ StringUtils.removeStart(StringUtils.removeStart(attribute.getCode(),
									Prefix.PRI_), Prefix.LNK_);

			Question childQues = new Question(questionCode, attribute.getName(), attribute);
			Ask childAsk = new Ask(childQues, sourceCode, targetCode, weight++);

			childAsks.add(childAsk);
		}

		// set child asks
		ask.setChildAsks(childAsks);

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
	 * @param definitions The list of definitions to check against
	 * @param answer     An incoming answer
	 * @param targets    The target entities to check, usually processEntity and
	 *                   original target
	 * @return Boolean
	 */
	public Boolean isDuplicate(List<Definition> definitions, Answer answer, BaseEntity... targets) {
		for (Definition definition : definitions) {
			if (isDuplicate(definition, answer, targets)) {
				return true;
			}
		}
		return false;
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
		String productCode = definition.getRealm();
		String definitionCode = definition.getCode();
		Set<EntityAttribute> uniques = beaUtils.getBaseEntityAttributesForBaseEntityWithAttributeCodePrefix(productCode, definitionCode, Prefix.UNQ_);
		log.info("Found " + uniques.size() + " UNQ attributes");

		String prefix = beaUtils.getEntityAttribute(productCode, definitionCode, Attribute.PRI_PREFIX).getValueString();

		for (EntityAttribute entityAttribute : uniques) {

			// fetch list of unique code combo
			List<String> codes = beUtils.getBaseEntityCodeArrayFromLinkAttribute(definition,
					entityAttribute.getAttributeCode());

			// skip if no value found
			if (codes == null)
				continue;

			SearchEntity searchEntity = new SearchEntity(SBE_COUNT_UNIQUE_PAIRS, "Count Unique Pairs")
					.add(new Filter(PRI_CODE, Operator.STARTS_WITH, prefix.concat("_")))
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

				// value has not yet been answered, not a duplicate
				if (value == null) {
					return false;
				}

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
			if (count != 0) {
				return true;
			}
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
			log.debug("Checking Validation: " + validation.getCode() + " = " + regex);
			boolean regexOk = Pattern.compile(regex).matcher(value).matches();

			if (!regexOk) {
				log.error("Regex FAILED! " + attribute.getCode() + ":" + regex + " ... [" + value + "] "
						+ validation.getErrormsg());
				return false;
			}
			log.debug("	- regex passed");
		}
		return true;
	}
}
