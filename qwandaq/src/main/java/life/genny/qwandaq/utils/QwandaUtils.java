package life.genny.qwandaq.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Answers;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataAttributeMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.UserToken;

/**
 * A utility class to assist in any Qwanda Engine Question
 * and Answer operations.
 * 
 * @author Jasper Robison
 */
@ApplicationScoped
public class QwandaUtils {

	static final Logger log = Logger.getLogger(QwandaUtils.class);

	private final ExecutorService executorService = Executors.newFixedThreadPool(200);

	Map<String, Map<String, Attribute>> attributes = new ConcurrentHashMap<>();

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

	public QwandaUtils() { }

	/**
	 * Get an attribute from the in memory attribute map. If productCode not found, it
	 * will try to fetch attributes from the DB.
	 *
	 * @param attributeCode the code of the attribute to get
	 * @return Attribute
	 */
	public Attribute getAttribute(final String attributeCode) {

		String productCode = userToken.getProductCode();
		Attribute attribute = CacheUtils.getObject(productCode, attributeCode, Attribute.class);

		if (attribute == null) {
			log.error("Could not find attribute " + attributeCode + " in cache: " + productCode);
			loadAllAttributesIntoCache(productCode);
		}

		attribute = CacheUtils.getObject(productCode, attributeCode, Attribute.class);

		if (attribute == null) {
			log.error("Bad Attribute in Cache for productCode " + productCode + " and code " + attributeCode);
		}

		return attribute;
	}

	/**
	 * Load all attributes into the cache from the database.
	 *
	 * @param productCode The product of the attributes to initialize
	 */
	public void loadAllAttributesIntoCache(String productCode) {

		if(StringUtils.isBlank(productCode)) {
			log.error("RECEIVED NULL PRODUCT CODE WHILE LOADING ATTRIBUTES INTO CACHE!");
		}

		Long attributeCount = databaseUtils.countAttributes(productCode);
		final Integer CHUNK_LOAD_SIZE = 200;

		final int TOTAL_PAGES = (int) Math.ceil(attributeCount / CHUNK_LOAD_SIZE);

		Long totalAttribsCached = 0L;

		log.info("About to load all attributes for productCode " + productCode);
		log.info("Found " + attributeCount + " attributes");

		QDataAttributeMessage msg = new QDataAttributeMessage();

		try {
			for (int currentPage = 0; currentPage < TOTAL_PAGES + 1; currentPage++) {


				int attributesLoaded = currentPage * CHUNK_LOAD_SIZE;

				// Correctly determine how many more attributes we need to load in
				int nextLoad = CHUNK_LOAD_SIZE;
				if (attributeCount - attributesLoaded < CHUNK_LOAD_SIZE) {
					nextLoad = (int) (attributeCount - attributesLoaded);
				}

				List<Attribute> attributeList = databaseUtils.findAttributes(productCode, attributesLoaded, nextLoad, null);
				log.info("Loading in page " + currentPage + " of " + TOTAL_PAGES + " containing " + nextLoad + " attributes");

				for (Attribute attribute : attributeList) {
					String key = attribute.getCode();
					CacheUtils.putObject(productCode, key, attribute);
					totalAttribsCached++;
				}

				// NOTE: Warning, this may cause OOM errors.
				msg.add(attributeList);

				if (attributeList.size() > 0) {
					log.debug("Start AttributeID:" 
							+ attributeList.get(0).getId() + ", End AttributeID:" 
							+ attributeList.get(attributeList.size() - 1).getId());
				}
			}

			CacheUtils.putObject(productCode, "ALL_ATTRIBUTES", msg);

			log.info("Cached " + totalAttribsCached + " attributes");
		} catch (Exception e) {
			log.error("Error loading attributes for productCode: " + productCode);
			e.printStackTrace();
		}
	}

	/**
	 * Load all attributes into the in memory map from the database.
	 */
	public void loadAllAttributes() {

		String productCode = userToken.getProductCode();
		List<Attribute> attributeList = null;

		log.info("About to load all attributes for productCode " + productCode);

		try {
			log.info("Fetching Attributes from database...");
			attributeList = databaseUtils.findAttributes(productCode, 0, 0, null);

			log.info("Loaded all attributes for productCode " + productCode);
			if (attributeList == null) {
				log.error("Null attributeList, not putting in map!!!");
				return;
			}

			// check for existing map
			if (!attributes.containsKey(productCode)) {
				attributes.put(productCode, new ConcurrentHashMap<String, Attribute>());
			}
			Map<String, Attribute> attributeMap = attributes.get(productCode);

			// insert attributes into map
			for (Attribute attribute : attributeList) {
				attributeMap.put(attribute.getCode(), attribute);
			}

			log.info("All attributes have been loaded: " + attributeMap.size() + " attributes");
		} catch (Exception e) {
			log.error("Error loading attributes for productCode " + productCode);
		}
	}

	/**
	 * Remove an atttribute from the in memory set using the code.
	 *
	 * @param code the code of the attribute to remove.
	 */
	public void removeAttributeFromMemory(String code) {

		attributes.get(userToken.getProductCode()).remove(code);
	}

	/**
	* Generate an ask for a question using the question code, the 
	* source and the target. This operation is recursive if the 
	* question is a group.
	*
	* @param code The code of the question
	* @param source The source entity
	* @param target The target entity
	* @return The generated Ask
	 */
	public Ask generateAskFromQuestionCode(final String code, final BaseEntity source, final BaseEntity target) {

		if (code == null) {
			log.error("Code must not be null");
			return null;
		}

		if (source == null) {
			log.error("Source must not be null");
			return null;
		}

		if (target == null) {
			log.error("Target must not be null");
			return null;
		}

		String productCode = userToken.getProductCode();

		log.info("Fetching Question from database: " + code);

		// find the question in the database
		Question question = databaseUtils.findQuestionByCode(productCode, code);
		Boolean mandatory = question.getMandatory();

		// init new parent ask
		Ask ask = new Ask(question);
		ask.setSourceCode(source.getCode());
		ask.setTargetCode(target.getCode());
		ask.setMandatory(mandatory);
		ask.setCreateOnTrigger(mandatory);
		ask.setRealm(productCode);

		// check if it is a question group
		if (question.getAttributeCode().startsWith(Question.QUESTION_GROUP_ATTRIBUTE_CODE)) {

			// fetch questionQuestions from the DB
			List<QuestionQuestion> questionQuestions = databaseUtils.findQuestionQuestionsBySourceCode(productCode, question.getCode());

			// recursively operate on child questions
			for (QuestionQuestion questionQuestion : questionQuestions) {

				log.info(" [*] Found Child Question in database: " + questionQuestion.getTargetCode());

				Ask child = generateAskFromQuestionCode(questionQuestion.getTargetCode(), source, target);
				child.setDisabled(questionQuestion.getDisabled());
				ask.addChildAsk(child);
			}
		}

		return ask;
	}

	/**
	* Recursively set the processId down through an ask tree.
	*
	* @param ask The ask to traverse
	* @param processId The processId to set
	 */
	public void recursivelySetProcessId(Ask ask, String processId) {

		ask.setProcessId(processId);

		if (ask.getChildAsks() != null) {
			for (Ask child : ask.getChildAsks()) {
				child.setProcessId(processId);
			}
		}
	}

	/**
	* Get all attribute codes active within an ask using recursion.
	*
	* @param codes The set of codes to add to
	* @param ask The ask to traverse
	* @return The udpated set of codes
	 */
	public Set<String> recursivelyGetAttributeCodes(Set<String> codes, Ask ask) {

		String[] excludes = new String[]{ 
			Question.QUESTION_GROUP_ATTRIBUTE_CODE, 
			"PRI_SUBMIT"
		};

		// grab attribute code of current ask
		if (!Arrays.asList(excludes).contains(ask.getAttributeCode())) {
			codes.add(ask.getAttributeCode());
		}

		if ((ask.getChildAsks() != null) && (ask.getChildAsks().length > 0)) {

			// grab all child ask attribute codes
			for (Ask childAsk : ask.getChildAsks()) {

				codes.addAll(recursivelyGetAttributeCodes(codes, childAsk));
			}
		}
		return codes;
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
	 * Save {@link Answers}.
	 * 
	 * @param answers The answers to save
	 * @return The target BaseEntitys
	 */
	public List<BaseEntity> saveAnswers(Answers answers) {

		return saveAnswers(answers.getAnswers());
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

			// check if target is valid
			BaseEntity target = beUtils.getBaseEntityByCode(targetCode);
			if (target == null) {
				log.error(targetCode + " does not exist!");
				continue;
			}

			// fetch the DEF for this target
			BaseEntity defBE = defUtils.getDEF(target);

			// filter Non-valid answers using def
			List<Answer> group = answersPerTargetCodeMap.get(targetCode);
			List<Answer> validAnswers = group.stream()
					.filter(item -> defUtils.answerValidForDEF(defBE, item))
					.collect(Collectors.toList());

			// update target using valid answers
			for (Answer answer : validAnswers) {
				try {
					target.addAnswer(answer);
				} catch (BadDataException e) {
					log.error(e);
				}
			}

			// update target in the cache and DB
			CacheUtils.putObject(userToken.getProductCode(), target.getCode(), target);
			databaseUtils.saveBaseEntity(target);

			targets.add(target);
		}

		return targets;
	}


	/**
	 * Delete a currently scheduled message via shleemy.
	 *
	 * @param code      the code of the schedule message to delete
	 */
	public void deleteSchedule(String code) {

		String uri = GennySettings.shleemyServiceUrl() + "/api/schedule/code/" + code;
		HttpUtils.delete(uri, userToken.getToken());
	}

	/**
	 * Generate Question group for a baseEntity
	 *
	 * @param baseEntity the baseEntity to create for
	 * @return Ask
	 */
	public Ask generateAskGroupUsingBaseEntity(BaseEntity baseEntity) {

		// grab def entity
		BaseEntity defBE = defUtils.getDEF(baseEntity);

		// create GRP ask
		Attribute questionAttribute = getAttribute("QQQ_QUESTION_GROUP");
		Question question = new Question("QUE_EDIT_GRP", "Edit " + baseEntity.getCode() + " : " + baseEntity.getName(),
				questionAttribute);
		Ask ask = new Ask(question, userToken.getUserCode(), baseEntity.getCode());

		List<Ask> childAsks = new ArrayList<>();
		QDataBaseEntityMessage entityMessage = new QDataBaseEntityMessage();
		entityMessage.setToken(userToken.getToken());
		entityMessage.setReplace(true);

		// create a child ask for every valid atribute
		baseEntity.getBaseEntityAttributes().stream()
				.filter(ea -> defBE.containsEntityAttribute("ATT_" + ea.getAttributeCode()))
				.forEach((ea) -> {

					String questionCode = "QUE_"
							+ StringUtils.removeStart(StringUtils.removeStart(ea.getAttributeCode(), "PRI_"), "LNK_");

					Question childQues = new Question(questionCode, ea.getAttribute().getName(), ea.getAttribute());
					Ask childAsk = new Ask(childQues, userToken.getUserCode(), baseEntity.getCode());

					childAsks.add(childAsk);

					if (ea.getAttributeCode().startsWith("LNK_")) {
						if (ea.getValueString() != null) {

							String[] codes = BaseEntityUtils.cleanUpAttributeValue(ea.getValueString()).split(",");

							for (String code : codes) {
								BaseEntity link = beUtils.getBaseEntityByCode(code);
								entityMessage.add(link);
							}
						}
					}

					if (defBE.containsEntityAttribute("SER_" + ea.getAttributeCode())) {
						searchUtils.performDropdownSearch(childAsk);
					}
				});

		// set child asks
		ask.setChildAsks(childAsks.toArray(new Ask[childAsks.size()]));

		KafkaUtils.writeMsg("webdata", entityMessage);

		return ask;
	}

	/**
	 * Update the status of the disabled field for an Ask on the web.
	 *
	 * @param ask       the ask to update
	 * @param disabled  the disabled status to set
	 */
	public void updateAskDisabled(Ask ask, Boolean disabled) {

		ask.setDisabled(disabled);

		QDataAskMessage askMsg = new QDataAskMessage(ask);
		askMsg.setToken(userToken.getToken());
		askMsg.setReplace(true);
		String json = jsonb.toJson(askMsg);
		KafkaUtils.writeMsg("webcmds", json);
	}

	/**
	 * Send an updated entity for each unique target in answers.
	 *
	 * @param answers   the answers to send entities for
	 */
	public void sendToFrontEnd(Answer... answers) {

		if (answers == null) {
			log.error("Answers is null!");
			return;
		}

		if (answers.length == 0) {
			log.error("Number of Answers is 0!");
			return;
		}

		String productCode = userToken.getProductCode();

		// sort answers into target BaseEntitys
		Map<String, List<Answer>> answersPerTargetCodeMap = Stream.of(answers)
				.collect(Collectors.groupingBy(Answer::getTargetCode));

		for (String targetCode : answersPerTargetCodeMap.keySet()) {

			// find the baseentity
			BaseEntity target = CacheUtils.getObject(productCode, targetCode, BaseEntity.class);
			if (target != null) {

				BaseEntity be = new BaseEntity(target.getCode(), target.getName());
				be.setRealm(userToken.getProductCode());

				for (Answer answer : answers) {

					try {
						Attribute att = getAttribute(answer.getAttributeCode());
						be.addAttribute(att);
						be.setValue(att, answer.getValue());
					} catch (BadDataException e) {
						e.printStackTrace();
					}
				}
				QDataBaseEntityMessage msg = new QDataBaseEntityMessage(be);
				msg.setReplace(true);
				msg.setToken(userToken.getToken());
				KafkaUtils.writeMsg("webcmds", msg);
			}
		}
	}

	/**
	 * Send feedback for answer data. ERROR, WARN, SUSPICIOUS or HINT.
	 *
	 * @param answer    the answer to send for
	 * @param prefix    the prefix to send
	 * @param message   the message to send
	 */
	public void sendFeedback(Answer answer, String prefix, String message) {

		// find the baseentity
		BaseEntity target = CacheUtils.getObject(userToken.getProductCode(), answer.getTargetCode(), BaseEntity.class);

		BaseEntity be = new BaseEntity(target.getCode(), target.getName());
		be.setRealm(userToken.getProductCode());

		try {

			Attribute att = getAttribute(answer.getAttributeCode());
			be.addAttribute(att);
			be.setValue(att, answer.getValue());
			Optional<EntityAttribute> ea = be.findEntityAttribute(answer.getAttributeCode());

			if (ea.isPresent()) {
				ea.get().setFeedback(prefix + ":" + message);

				QDataBaseEntityMessage msg = new QDataBaseEntityMessage(be);
				msg.setReplace(true);
				msg.setToken(userToken.getToken());
				KafkaUtils.writeMsg("webcmds", msg);
			}
		} catch (BadDataException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Is the number a valid ABN.
	 *
	 * @param abn the abn to check
	 * @return boolean
	 */
	public boolean isValidAbnFormat(final String abn) {
		if (NumberUtils.isDigits(abn) && abn.length() != 11) {
			return false;
		}
		final int[] weights = { 10, 1, 3, 5, 7, 9, 11, 13, 15, 17, 19 };
		// split abn number string by digits to get int array
		int[] abnDigits = Stream.of(abn.split("\\B")).mapToInt(Integer::parseInt).toArray();
		// reduce by applying weight[index] * abnDigits[index] (NOTE: substract 1 for
		// the first digit in abn number)
		int sum = IntStream.range(0, weights.length).reduce(0,
				(total, idx) -> total + weights[idx] * (idx == 0 ? abnDigits[idx] - 1 : abnDigits[idx]));
		return (sum % 89 == 0);
	}
}
