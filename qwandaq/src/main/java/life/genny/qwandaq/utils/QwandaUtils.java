package life.genny.qwandaq.utils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.message.QEventMessage;
import life.genny.qwandaq.message.QScheduleMessage;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.GennyToken;

/**
 * A utility class to assist in any Qwanda Engine Question
 * and Answer operations.
 * <p>
 * The class should be initialized with a GennyToken
 * to ensure successful operation
 * <p>
 * 
 * @author Jasper Robison
 */
@RegisterForReflection
@ApplicationScoped
public class QwandaUtils {

	static final Logger log = Logger.getLogger(QwandaUtils.class);

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	DefUtils defUtils;

	@Inject
	SearchUtils searchUtils;

	private final ExecutorService executorService = Executors.newFixedThreadPool(200);

	Map<String, Map<String, Attribute>> attributes = new ConcurrentHashMap<>();

	static Jsonb jsonb = JsonbBuilder.create();

	GennyToken gennyToken;

	public QwandaUtils() {
	}

	/**
	 * @param token the token to set
	 */

	public void init(GennyToken token) {
		gennyToken = token;
		// loadAllAttributes();
		loadAllAttributesIntoCache();
	}

	/**
	 * @param token         the token to set
	 * @param attributeList the attributeList to set
	 */
	public void init(GennyToken token, List<Attribute> attributeList) {
		gennyToken = token;

		attributes.put(token.getRealm(), new ConcurrentHashMap<String, Attribute>());
		Map<String, Attribute> attributeMap = attributes.get(token.getRealm());

		for (Attribute attribute : attributeList) {
			attributeMap.put(attribute.getCode(), attribute);
		}
	}

	/**
	 * Get an attribute from the in memory attribute map. If realm not found, it
	 * will try to fetch attributes from the DB.
	 *
	 * @param attributeCode the code of the attribute to get
	 * @return Attribute
	 */
	public Attribute getAttribute(final String attributeCode) {

		String realm = gennyToken.getRealm();
		Attribute attribute = CacheUtils.getObject(realm, attributeCode, Attribute.class);

		if (attribute == null) {
			log.error("Could not find attribute " + attributeCode + " in cache: " + realm);
			loadAllAttributesIntoCache();
		}

		attribute = CacheUtils.getObject(realm, attributeCode, Attribute.class);

		if (attribute == null) {
			log.error("Bad Attribute in Cache for realm " + realm + " and code " + attributeCode);
		}

		return attribute;
	}

	/**
	 * Load all attributes into the cache from the database.
	 */
	public void loadAllAttributesIntoCache() {

		String realm = gennyToken.getRealm();
		List<Attribute> attributeList = null;

		Long attributeCount = databaseUtils.countAttributes(realm);
		final Integer CHUNK_LOAD_SIZE = 200;

		final int TOTAL_PAGES = (int) Math.ceil(attributeCount / CHUNK_LOAD_SIZE);

		Long totalAttribsCached = 0L;

		log.info("About to load all attributes for realm " + realm);
		log.info("Found " + attributeCount + " attributes");

		try {
			for (int currentPage = 0; currentPage < TOTAL_PAGES + 1; currentPage++) {

				int attributesLoaded = currentPage * CHUNK_LOAD_SIZE;

				// Correctly determine how many more attributes we need to load in
				int nextLoad = CHUNK_LOAD_SIZE;
				if (attributeCount - attributesLoaded < CHUNK_LOAD_SIZE) {
					nextLoad = (int) (attributeCount - attributesLoaded);
				}

				attributeList = databaseUtils.findAttributes(realm, attributesLoaded, nextLoad, null);

				for (Attribute attribute : attributeList) {
					String key = attribute.getCode();
					CacheUtils.putObject(realm, key, attribute);
					totalAttribsCached++;
				}

				log.info("Loading in page " + currentPage + " of " + TOTAL_PAGES + " containing " + nextLoad + " attributes");

				if (attributeList.size() > 0) {
					log.debug("Start AttributeID:" 
							+ attributeList.get(0).getId() + ", End AttributeID:" 
							+ attributeList.get(attributeList.size() - 1).getId());
				}
			}

			log.info("Cached " + totalAttribsCached + " attributes");
		} catch (Exception e) {
			log.error("Error loading attributes for realm " + realm);
			e.printStackTrace();
		}
	}

	/**
	 * Load all attributes into the in memory map from the database.
	 */
	public void loadAllAttributes() {

		String realm = gennyToken.getRealm();
		List<Attribute> attributeList = null;

		log.info("About to load all attributes for realm " + realm);

		try {
			log.info("Fetching Attributes from database...");
			attributeList = databaseUtils.findAttributes(realm, 0, 0, null);

			log.info("Loaded all attributes for realm " + realm);
			if (attributeList == null) {
				log.error("Null attributeList, not putting in map!!!");
				return;
			}

			// check for existing map
			if (!attributes.containsKey(realm)) {
				attributes.put(realm, new ConcurrentHashMap<String, Attribute>());
			}
			Map<String, Attribute> attributeMap = attributes.get(realm);

			// insert attributes into map
			for (Attribute attribute : attributeList) {
				attributeMap.put(attribute.getCode(), attribute);
			}

			log.info("All attributes have been loaded: " + attributeMap.size() + " attributes");
		} catch (Exception e) {
			log.error("Error loading attributes for realm " + realm);
		}
	}

	/**
	 * Remove an atttribute from the in memory set using the code.
	 *
	 * @param code the code of the attribute to remove.
	 */
	public void removeAttributeFromMemory(String code) {

		String realm = gennyToken.getRealm();
		attributes.get(realm).remove(code);
	}

	/**
	 * Get a Question using a code.
	 *
	 * @param code      the code of the question to get
	 * @param userToken the userToken to use
	 * @return Question
	 */
	public Question getQuestion(String code, GennyToken userToken) {

		if (code == null) {
			log.error("Code must not be null!");
			return null;
		}

		// fetch from cache
		Question question = CacheUtils.getObject(userToken.getRealm(), code, Question.class);

		if (question == null) {

			// fetch from database if not found in cache
			log.warn("Could NOT read " + code + " from cache! Checking Database...");
			question = databaseUtils.findQuestionByCode(userToken.getRealm(), code);

			if (question == null) {
				log.error("Could not find question " + code + " in database!");
				return null;
			}

			// cache the fetched question for quicker access
			CacheUtils.writeCache(userToken.getRealm(), code, jsonb.toJson(question));
			log.info(question.getCode() + " written to cache!");
		}

		return question;
	}

	/**
	 * Send a {@link QEventMessage} to shleemy for scheduling.
	 *
	 * @param userToken       the userToken to schedule for
	 * @param eventMsgCode    the eventMsgCode to set
	 * @param scheduleMsgCode the scheduleMsgCode to set
	 * @param triggertime     the triggertime to set
	 * @param targetCode      the targetCode to set
	 */
	public void scheduleEvent(GennyToken userToken, String eventMsgCode, String scheduleMsgCode,
			LocalDateTime triggertime, String targetCode) {

		// create the event message
		QEventMessage evtMsg = new QEventMessage("SCHEDULE_EVT", eventMsgCode);
		evtMsg.setToken(userToken.getToken());
		evtMsg.getData().setTargetCode(targetCode);

		// create a recipient list
		String[] rxList = new String[2];
		rxList[0] = "SUPERUSER";
		rxList[1] = userToken.getUserCode();
		evtMsg.setRecipientCodeArray(rxList);

		log.info("Scheduling event: " + eventMsgCode + ", Trigger time: " + triggertime.toString());

		// create schedule message
		QScheduleMessage scheduleMessage = new QScheduleMessage(scheduleMsgCode, jsonb.toJson(evtMsg),
				userToken.getUserCode(), "project", triggertime, userToken.getRealm());
		log.info("Sending message " + scheduleMessage.getCode() + " to shleemy for scheduling");

		// send msg to shleemy
		String json = jsonb.toJson(scheduleMessage);
		KafkaUtils.writeMsg("schedule", json);
	}

	/**
	 * Delete a currently scheduled message via shleemy.
	 *
	 * @param userToken the userToken to delete with
	 * @param code      the code of the schedule message to delete
	 */
	public void deleteSchedule(GennyToken userToken, String code) {

		String uri = GennySettings.shleemyServiceUrl() + "/api/schedule/code/" + code;
		HttpUtils.delete(uri, userToken.getToken());

	}

	/**
	 * Generate Question group for a baseEntity
	 *
	 * @param baseEntity the baseEntity to create for
	 * @param beUtils    the utility to use
	 * @return Ask
	 */
	public Ask generateAskGroupUsingBaseEntity(BaseEntity baseEntity, BaseEntityUtils beUtils) {

		// init tokens
		GennyToken userToken = beUtils.getGennyToken();
		String token = userToken.getToken();

		// grab def entity
		BaseEntity defBE = defUtils.getDEF(baseEntity);

		// create GRP ask
		Attribute questionAttribute = getAttribute("QQQ_QUESTION_GROUP");
		Question question = new Question("QUE_EDIT_GRP", "Edit " + baseEntity.getCode() + " : " + baseEntity.getName(),
				questionAttribute);
		Ask ask = new Ask(question, userToken.getUserCode(), baseEntity.getCode());

		List<Ask> childAsks = new ArrayList<>();
		QDataBaseEntityMessage entityMessage = new QDataBaseEntityMessage();
		entityMessage.setToken(token);
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
						searchUtils.performDropdownSearch(childAsk, userToken);
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
	 * @param userToken the userToken to use
	 */
	public void updateAskDisabled(Ask ask, Boolean disabled, GennyToken userToken) {

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
	 * @param userToken the userToken to use
	 * @param answers   the answers to send entities for
	 */
	public void sendToFrontEnd(GennyToken userToken, Answer... answers) {

		if (answers == null) {
			log.error("Answers is null!");
			return;
		}

		if (answers.length == 0) {
			log.error("Number of Answers is 0!");
			return;
		}

		String realm = userToken.getRealm();

		// sort answers into target BaseEntitys
		Map<String, List<Answer>> answersPerTargetCodeMap = Stream.of(answers)
				.collect(Collectors.groupingBy(Answer::getTargetCode));

		for (String targetCode : answersPerTargetCodeMap.keySet()) {

			// find the baseentity
			BaseEntity target = CacheUtils.getObject(realm, targetCode, BaseEntity.class);
			if (target != null) {

				BaseEntity be = new BaseEntity(target.getCode(), target.getName());
				be.setRealm(userToken.getRealm());

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
	 * @param userToken the userToken to use
	 * @param answer    the answer to send for
	 * @param prefix    the prefix to send
	 * @param message   the message to send
	 */
	public void sendFeedback(GennyToken userToken, Answer answer, String prefix, String message) {

		// find the baseentity
		BaseEntity target = CacheUtils.getObject(userToken.getRealm(), answer.getTargetCode(), BaseEntity.class);

		BaseEntity be = new BaseEntity(target.getCode(), target.getName());
		be.setRealm(userToken.getRealm());

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
