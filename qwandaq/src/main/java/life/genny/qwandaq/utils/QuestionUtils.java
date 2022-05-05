package life.genny.qwandaq.utils;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;

import com.google.common.reflect.TypeToken;

import org.apache.http.client.ClientProtocolException;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Context;
import life.genny.qwandaq.ContextList;
import life.genny.qwandaq.Link;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.EntityEntity;
import life.genny.qwandaq.entity.EntityQuestion;
import life.genny.qwandaq.message.QBulkMessage;
import life.genny.qwandaq.message.QCmdMessage;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.message.QwandaMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.validation.Validation;

/**
 * A static utility class used in generating Questions in Genny.
 * 
 * @author Adam Crow
 * @author Jasper Robison
 */
@ApplicationScoped
public class QuestionUtils {

	static final Logger log = Logger.getLogger(QuestionUtils.class);

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	UserToken userToken;

	@Inject
	EntityManager entityManager;

	/**
	 * Check if a Question group exists in the database and cache.
	 *
	 * @param sourceCode   the sourceCode to check
	 * @param targetCode   the targetCode to check
	 * @param questionCode the questionCode to check
	 * @return Boolean
	 */
	public Boolean doesQuestionGroupExist(
			String sourceCode,
			String targetCode,
			final String questionCode) {

		// we grab the question group using the questionCode
		QDataAskMessage questions = getAsks(sourceCode, targetCode, questionCode);

		// we check if the question payload is not empty
		if (questions != null) {

			// we check if the question group contains at least one question
			if (questions.getItems() != null && questions.getItems().length > 0) {

				Ask firstQuestion = questions.getItems()[0];

				// we check if the question is a question group and contains at least one
				// question
				if (firstQuestion.getAttributeCode().contains("QQQ_QUESTION_GROUP_BUTTON_SUBMIT")) {
					return firstQuestion.getChildAsks().length > 0;
				} else {
					// if it is an ask we return true
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Deserialize a json {@link String} to a {@link JsonObject}.
	 *
	 * @param string The string to deserialize.
	 * @return JsonObject The equivalent JsonObject
	 */
	public JsonObject toJson(String string) {
		// open a reader and feed in the string
		JsonReader jsonReader = Json.createReader(new StringReader(string));
		JsonObject jsonObject = jsonReader.readObject();
		jsonReader.close();

		return jsonObject;
	}

	/**
	 * Recuresively run through ask children and update the question
	 * using what is stored in the cache.
	 *
	 * @param ask     the ask to set
	 */
	public void setCachedQuestionsRecursively(Ask ask) {

		// call recursively if ask represents a question group
		if (ask.getAttributeCode().equals("QQQ_QUESTION_GROUP")) {

			for (Ask childAsk : ask.getChildAsks()) {
				setCachedQuestionsRecursively(childAsk);
			}

		} else {

			// otherwise we fetch the question and update the ask
			Question question = ask.getQuestion();

			Question cachedQuestion = CacheUtils.getObject(userToken.getProductCode(), 
					question.getCode(), Question.class);

			if (cachedQuestion != null) {

				// grab the icon too
				if (question.getIcon() != null) {
					cachedQuestion.setIcon(question.getIcon());
				}
				ask.setQuestion(cachedQuestion);
				ask.setContextList(cachedQuestion.getContextList());
			}
		}
	}

	/**
	 * @param rootQuestion the rootQuestion to find with
	 * @param source       the source to use
	 * @param target       the target to use
	 * @return List&lt;Ask&gt;
	 */
	public List<Ask> findAsks(final Question rootQuestion, final BaseEntity source, final BaseEntity target) {

		return findAsks(rootQuestion, source, target, false, false, false, false);
	}

	/**
	 * @param rootQuestion                   the rootQuestion to find with
	 * @param source                         the source to use
	 * @param target                         the target to use
	 * @param childQuestionIsMandatory       the childQuestionIsMandatory to use
	 * @param childQuestionIsReadonly        the childQuestionIsReadonly to use
	 * @param childQuestionIsFormTrigger     the childQuestionIsFormTrigger to use
	 * @param childQuestionIsCreateOnTrigger the childQuestionIsCreateOnTrigger to use
	 * @return List&lt;Ask&gt;
	 */
	public List<Ask> findAsks(final Question rootQuestion, final BaseEntity source, final BaseEntity target,
			Boolean childQuestionIsMandatory, Boolean childQuestionIsReadonly, Boolean childQuestionIsFormTrigger,
			Boolean childQuestionIsCreateOnTrigger) {

		String productCode = userToken.getProductCode();

		if (rootQuestion == null) {
			log.error(
					"rootQuestion for findAsks is null - source=" + source.getCode() + ": target " + target.getCode());
			return new ArrayList<Ask>();
		}

		List<Ask> asks = new ArrayList<>();
		Boolean mandatory = rootQuestion.getMandatory() || childQuestionIsMandatory;
		Boolean readonly = rootQuestion.getReadonly() || childQuestionIsReadonly;
		Ask ask = null;

		// check if this already exists?
		List<Ask> myAsks = databaseUtils.findAsksByQuestionCode(productCode, rootQuestion.getCode(),
				source.getCode(), target.getCode());
		if (!(myAsks == null || myAsks.isEmpty())) {
			ask = myAsks.get(0);
			ask.setMandatory(mandatory);
			ask.setReadonly(readonly);
			ask.setFormTrigger(childQuestionIsFormTrigger);
			ask.setCreateOnTrigger(childQuestionIsCreateOnTrigger);

		} else {
			ask = new Ask(rootQuestion, source.getCode(), target.getCode(), mandatory, 1.0, false, false, readonly);
			ask.setCreateOnTrigger(childQuestionIsMandatory);
			ask.setFormTrigger(childQuestionIsFormTrigger);
			ask.setRealm(productCode);

			// Now merge ask name if required
			ask = performMerge(ask);
		}

		// create one
		if (rootQuestion.getAttributeCode().startsWith(Question.QUESTION_GROUP_ATTRIBUTE_CODE)) {
			// Recurse!
			List<QuestionQuestion> qqList = new ArrayList<>(rootQuestion.getChildQuestions());
			Collections.sort(qqList); // sort by priority
			List<Ask> childAsks = new ArrayList<>();
			for (QuestionQuestion qq : qqList) {
				String qCode = qq.getPk().getTargetCode();
				log.info(qq.getPk().getSourceCode() + " -> Child Question -> " + qCode);
				Question childQuestion = databaseUtils.findQuestionByCode(productCode, qCode);
				// Grab whatever icon the QuestionQuestion has set
				childQuestion.setIcon(qq.getIcon());
				if (childQuestion != null) {
					List<Ask> askChildren = null;
					try {
						askChildren = findAsks(childQuestion, source, target, qq.getMandatory(), qq.getReadonly(),
								qq.getFormTrigger(), qq.getCreateOnTrigger());
						for (Ask child : askChildren) {
							child.setQuestion(childQuestion);
							child.setHidden(qq.getHidden());
							child.setDisabled(qq.getDisabled());
							child.setReadonly(qq.getReadonly());
						}
					} catch (Exception e) {
						log.error("Error with QuestionQuestion: " + rootQuestion.getCode());
						log.error("Problem Question: " + childQuestion.getCode());
						e.printStackTrace();
					}
					childAsks.addAll(askChildren);
				}
			}
			Ask[] asksArray = childAsks.toArray(new Ask[0]);
			ask.setChildAsks(asksArray);

			ask.setRealm(productCode);
		}

		asks.add(ask);
		return asks;
	}

	/**
	 * @param rootQuestion the rootQuestion to create with
	 * @param source       the source to use
	 * @param target       the target to use
	 * @return List&lt;Ask&gt;
	 */
	public List<Ask> createAsksByQuestion(final Question rootQuestion, final BaseEntity source,
			final BaseEntity target) {

		List<Ask> asks = findAsks(rootQuestion, source, target);

		return asks;
	}

	/**
	 * @param questionCode the questionCode to use
	 * @param sourceCode   the sourceCode to use
	 * @param targetCode   the targetCode to use
	 * @return List&lt;Ask&gt;
	 */
	public List<Ask> createAsksByQuestionCode(final String questionCode, final String sourceCode,
			final String targetCode) {

		Question rootQuestion = databaseUtils.findQuestionByCode(userToken.getProductCode(), questionCode);
		BaseEntity source = null;
		BaseEntity target = null;

		if ("PER_SOURCE".equals(sourceCode) && "PER_TARGET".equals(targetCode)) {
			source = new BaseEntity(sourceCode, "SourceCode");
			target = new BaseEntity(targetCode, "TargetCode");
		} else {
			source = beUtils.getBaseEntityByCode(sourceCode);
			target = beUtils.getBaseEntityByCode(targetCode);
		}

		return createAsksByQuestion(rootQuestion, source, target);
	}

	/**
	 * @param sourceCode   the sourceCode to use
	 * @param targetCode   the targetCode to use
	 * @param questionCode the questionCode to use
	 * @return QDataAskMessage
	 */
	public QDataAskMessage getDirectAsks(String sourceCode, String targetCode, String questionCode) {
		List<Ask> asks = null;

		asks = createAsksByQuestionCode(questionCode, sourceCode, targetCode);
		log.debug("Number of asks=" + asks.size());
		log.debug("Number of asks=" + asks);
		final QDataAskMessage askMsgs = new QDataAskMessage(asks.toArray(new Ask[0]));

		return askMsgs;
	}

	/**
	 * @param sourceCode   the sourceCode to use
	 * @param targetCode   the targetCode to use
	 * @param questionCode the questionCode to use
	 * @return QDataAskMessage
	 */
	public QDataAskMessage getAsks(String sourceCode, String targetCode, String questionCode) {

		String productCode = userToken.getProductCode();

		// TODO: Ensure migration from api to Database worked fine
		List<Ask> asks = databaseUtils.findAsksByQuestionCode(productCode, questionCode, sourceCode, targetCode);
		QDataAskMessage msg = new QDataAskMessage(asks.toArray(new Ask[0]));

		// Identify all the attributeCodes and build up a working active Set
		Set<String> activeAttributeCodes = new HashSet<String>();
		for (Ask ask : msg.getItems()) {
			activeAttributeCodes.addAll(getAttributeCodes(ask));

			// Go down through the child asks and get cached questions
			setCachedQuestionsRecursively(ask);
		}
		// Now fetch the set from cache and add it....
		Type type = new TypeToken<Set<String>>() {
		}.getType();

		Set<String> activeAttributesSet = CacheUtils.getObject(productCode, "ACTIVE_ATTRIBUTES", type);

		if (activeAttributesSet == null) {
			activeAttributesSet = new HashSet<String>();
		}
		activeAttributesSet.addAll(activeAttributeCodes);

		CacheUtils.putObject(productCode, "ACTIVE_ATTRIBUTES", activeAttributesSet);

		log.debug("Total Active AttributeCodes = " + activeAttributesSet.size());

		return msg;
	}

	/**
	 * Get all attributes used by an {@link Ask} and its children.
	 *
	 * @param ask The ask to traverse.
	 * @return A set of Strings containing the attribute codes.
	 */
	private Set<String> getAttributeCodes(Ask ask) {

		// grab attribute code of current ask
		Set<String> activeCodes = new HashSet<String>();
		activeCodes.add(ask.getAttributeCode());

		if ((ask.getChildAsks() != null) && (ask.getChildAsks().length > 0)) {

			// grab all child ask attribute codes
			for (Ask childAsk : ask.getChildAsks()) {

				activeCodes.addAll(getAttributeCodes(childAsk));
			}
		}
		return activeCodes;
	}

	/**
	 * @param sourceCode   the sourceCode to use
	 * @param targetCode   the targetCode to use
	 * @param questionCode the questionCode to get
	 * @return QwandaMessage
	 * @throws ClientProtocolException if something went wrong
	 * @throws IOException             if something went wrong
	 */
	public QwandaMessage getQuestions(String sourceCode, String targetCode, String questionCode) throws ClientProtocolException, IOException {

		return getQuestions(sourceCode, targetCode, questionCode, null, true);
	}

	/**
	 * Get Questions for a given souce, target and code.
	 *
	 * @param sourceCode      the sourceCode to use
	 * @param targetCode      the targetCode to use
	 * @param questionCode    the questionCode to get
	 * @param stakeholderCode the stakeholderCode to use
	 * @param pushSelection   push the selection
	 * @return QwandaMessage
	 * @throws ClientProtocolException if something went wrong
	 * @throws IOException             if something went wrong
	 */
	public QwandaMessage getQuestions(
			String sourceCode,
			String targetCode,
			String questionCode,
			String stakeholderCode,
			Boolean pushSelection) throws ClientProtocolException, IOException {

		QBulkMessage bulk = new QBulkMessage();
		QwandaMessage qwandaMessage = new QwandaMessage();

		Instant start = Instant.now();

		// get the ask data
		QDataAskMessage questions = getAsks(sourceCode, targetCode, questionCode);

		Instant middle = Instant.now();
		log.info("getAsks duration = " + Duration.between(start, middle).toMillis() + " ms");

		if (questions == null) {
			log.error("Questions Msg is null " + sourceCode + "/asks2/" + questionCode + "/" + targetCode);
			return null;
		}

		// if we have the questions, loop through the asks and send required data to
		// front end
		Ask[] asks = questions.getItems();
		if (asks != null && pushSelection) {
			QBulkMessage askData = sendAsksRequiredData(asks, stakeholderCode);
			for (QDataBaseEntityMessage message : askData.getMessages()) {
				bulk.add(message);
			}
		}

		Instant end = Instant.now();
		log.info("sendAsksRequiredData duration = " + Duration.between(middle, end).toMillis() + " ms");

		qwandaMessage.askData = bulk;
		qwandaMessage.asks = questions;

		return qwandaMessage;
	}

	/**
	 * @param sourceCode        the sourceCode to use
	 * @param targetCode        the targetCode to use
	 * @param questionGroupCode the questionGroupCode to use
	 * @return QwandaMessage
	 */
	public QwandaMessage askQuestions(final String sourceCode, final String targetCode, final String questionGroupCode) {
		return askQuestions(sourceCode, targetCode, questionGroupCode, null,
				true);
	}

	/**
	 * @param sourceCode        the sourceCode to use
	 * @param targetCode        the targetCode to use
	 * @param questionGroupCode the questionGroupCode to use
	 * @param stakeholderCode   the stakeholderCode to use
	 * @return QwandaMessage
	 */
	public QwandaMessage askQuestions(final String sourceCode, final String targetCode,
			final String questionGroupCode, String stakeholderCode) {
		return askQuestions(sourceCode, targetCode, questionGroupCode, stakeholderCode, true);
	}

	/**
	 * @param sourceCode        the sourceCode to use
	 * @param targetCode        the targetCode to use
	 * @param questionGroupCode the questionGroupCode to use
	 * @param pushSelection     push selection
	 * @return QwandaMessage
	 */
	public QwandaMessage askQuestions(final String sourceCode, final String targetCode,
			final String questionGroupCode, Boolean pushSelection) {
		return askQuestions(sourceCode, targetCode, questionGroupCode, null, pushSelection);
	}

	/**
	 * @param sourceCode        the sourceCode to use
	 * @param targetCode        the targetCode to use
	 * @param questionGroupCode the questionGroupCode to use
	 * @param stakeholderCode   the stakeholderCode to use
	 * @param pushSelection     push selection
	 * @return QwandaMessage
	 */
	public QwandaMessage askQuestions(final String sourceCode, final String targetCode,
			final String questionGroupCode, final String stakeholderCode,
			final Boolean pushSelection) {

		try {

			// if sending the questions worked, we ask user
			return getQuestions(sourceCode, targetCode, questionGroupCode,
					stakeholderCode, pushSelection);

		} catch (Exception e) {
			log.info("Ask questions exception: ");
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * @param questions               the questions to set with
	 * @param questionAttributeCode   the questionAttributeCode to set with
	 * @param customTemporaryQuestion the customTemporaryQuestion to set with
	 * @return QwandaMessage
	 */
	public QwandaMessage setCustomQuestion(QwandaMessage questions, String questionAttributeCode,
			String customTemporaryQuestion) {

		if (questions != null && questionAttributeCode != null) {
			Ask[] askArr = questions.asks.getItems();
			if (askArr != null && askArr.length > 0) {
				for (Ask ask : askArr) {
					Ask[] childAskArr = ask.getChildAsks();
					if (childAskArr != null && childAskArr.length > 0) {
						for (Ask childAsk : childAskArr) {
							log.info("child ask code :: " + childAsk.getAttributeCode() + ", child askname :: "
									+ childAsk.getName());
							if (childAsk.getAttributeCode().equals(questionAttributeCode)) {
								if (customTemporaryQuestion != null) {
									childAsk.getQuestion().setName(customTemporaryQuestion);
									return questions;
								}
							}
						}
					}
				}
			}
		}
		return questions;
	}

	/**
	 * Send out the required entity data for a set of asks.
	 *
	 * @param asks            the asks to send
	 * @param token           the token to send with
	 * @param stakeholderCode the stakeholderCode to send with
	 * @return QBulkMessage
	 */
	private QBulkMessage sendAsksRequiredData(Ask[] asks, String stakeholderCode) {

		QBulkMessage bulk = new QBulkMessage();

		// we loop through the asks and send the required data if necessary
		for (Ask ask : asks) {

			// if attribute code starts with "LNK_", then it is a dropdown selection.
			String attributeCode = ask.getAttributeCode();
			if (attributeCode != null && attributeCode.startsWith("LNK_")) {

				// we get the attribute validation to get the group code
				Attribute attribute = qwandaUtils.getAttribute(attributeCode);

				if (attribute != null) {

					// grab the group in the validation
					DataType attributeDataType = attribute.getDataType();
					if (attributeDataType != null) {

						List<Validation> validations = attributeDataType.getValidationList();

						// we loop through the validations
						for (Validation validation : validations) {

							List<String> validationStrings = validation.getSelectionBaseEntityGroupList();

							if (validationStrings != null) {
								for (String validationString : validationStrings) {

									if (validationString.startsWith("GRP_")) {

										// Grab the parent
										BaseEntity parent = CacheUtils.getObject(userToken.getProductCode(),
												validationString, BaseEntity.class);

										// we have a GRP. we push it to FE
										List<BaseEntity> bes = getChildren(validationString, 2);
										List<BaseEntity> filteredBes = null;

										if (bes != null && bes.isEmpty() == false) {

											// hard coding this for now. sorry
											if ("LNK_LOAD_LISTS".equals(attributeCode) && stakeholderCode != null) {

												// we filter load you only are a stakeholder of
												filteredBes = bes.stream().filter(baseEntity -> {
													return baseEntity.getValue("PRI_AUTHOR", "")
															.equals(stakeholderCode);
												}).collect(Collectors.toList());
											} else {
												filteredBes = bes;
											}

											// create message for base entities required for the validation
											QDataBaseEntityMessage beMessage = new QDataBaseEntityMessage(filteredBes);
											beMessage.setLinkCode("LNK_CORE");
											beMessage.setParentCode(validationString);
											beMessage.setReplace(true);
											bulk.add(beMessage);

											// create message for parent
											QDataBaseEntityMessage parentMessage = new QDataBaseEntityMessage(parent);
											bulk.add(parentMessage);
										}
									}
								}
							}
						}
					}
				}
			}

			// recursive call to add nested entities
			Ask[] childAsks = ask.getChildAsks();
			if (childAsks != null && childAsks.length > 0) {

				QBulkMessage newBulk = sendAsksRequiredData(childAsks, stakeholderCode);

				for (QDataBaseEntityMessage msg : newBulk.getMessages()) {
					bulk.add(msg);
				}
			}
		}

		return bulk;
	}

	/**
	 * Create a question for a BaseEntity.
	 * 
	 * @param be              the be to create for
	 * @param isQuestionGroup the isQuestionGroup status
	 * @return Ask
	 */
	public Ask createQuestionForBaseEntity(BaseEntity be, Boolean isQuestionGroup) {

		// create attribute code using isQuestionGroup and fetch attribute
		String attributeCode = isQuestionGroup ? "QQQ_QUESTION_GROUP_INPUT" : "PRI_EVENT";
		Attribute attribute = qwandaUtils.getAttribute(attributeCode);

		/*
		 * creating suffix according to value of isQuestionGroup. If it is a
		 * question-group, suffix "_GRP" is required"
		 */
		String questionSuffix = isQuestionGroup ? "_GRP" : "";

		// generate question then return ask
		Question newQuestion = new Question("QUE_" + be.getCode() + questionSuffix, be.getName(), attribute, false);

		return new Ask(newQuestion, be.getCode(), be.getCode(), false, 1.0, false, false, true);
	}

	/**
	 * Create a question for a BaseEntity.
	 * 
	 * @param be              the be to create for
	 * @param isQuestionGroup the isQuestionGroup status
	 * @param sourceAlias     the sourceAlias to create with
	 * @param targetAlias     the targetAlias to create with
	 * @return Ask
	 */
	public Ask createQuestionForBaseEntity2(BaseEntity be, Boolean isQuestionGroup,
			final String sourceAlias, final String targetAlias) {

		// create attribute code using isQuestionGroup and fetch attribute
		String attributeCode = isQuestionGroup ? "QQQ_QUESTION_GROUP_INPUT" : "PRI_EVENT";
		Attribute attribute = qwandaUtils.getAttribute(attributeCode);

		// create temporary attribute if not fetched
		if (attribute == null) {
			log.error("Attribute DOES NOT EXIST! " + attributeCode + " creating temp");
			attribute = new Attribute(attributeCode, attributeCode, new DataType("DTT_THEME"));
		}

		// generate the question
		Question newQuestion = new Question(be.getCode(), be.getName(), attribute, false);

		// generate question and return
		Ask ask = new Ask(newQuestion, (sourceAlias != null ? sourceAlias : be.getCode()),
				(targetAlias != null ? targetAlias : be.getCode()), false, 1.0, false, false, true);
		ask.setRealm(userToken.getProductCode());

		return ask;

	}

	/**
	 * Create a virtual link between a {@link BaseEntity} and an {@link Ask}.
	 *
	 * @param source    the source to create with
	 * @param ask       the ask to create with
	 * @param linkCode  the linkCode to create with
	 * @param linkValue the linkValue to create with
	 * @return BaseEntity
	 */
	public BaseEntity createVirtualLink(BaseEntity source, Ask ask, String linkCode, String linkValue) {

		if (source != null) {

			Set<EntityQuestion> entityQuestionList = source.getQuestions();

			Link link = new Link(source.getCode(), ask.getQuestion().getCode(), linkCode,
					linkValue);
			link.setWeight(ask.getWeight());
			EntityQuestion ee = new EntityQuestion(link);
			entityQuestionList.add(ee);

			source.setQuestions(entityQuestionList);
		}
		return source;
	}

	/**
	 * Fetch a question from the cache using the code. This will use
	 * the database as a backup if not found in cache.
	 *
	 * @param code    the code to get
	 * @return Question
	 */
	public Question getQuestion(String code) {

		String productCode = userToken.getProductCode();

		// fetch from cache
		Question question = CacheUtils.getObject(productCode, code, Question.class);

		if (question != null) {
			return question;
		}

		log.warn("No Question in cache for " + code + ", trying to grab from database...");

		// fetch from DB
		question = databaseUtils.findQuestionByCode(productCode, code);

		if (question == null) {
			log.error("No Question found in database for " + code + " either!!!!");
		}

		return question;
	}

	/**
	 * Fetch linked children entities of a given {@link BaseEntity} using its code.
	 *
	 * @param beCode  the beCode to look in
	 * @param level   the level of depth to look
	 * @return List
	 */
	public List<BaseEntity> getChildren(String beCode, Integer level) {

		if (level == 0) {
			return null;
		}

		String productCode = userToken.getProductCode();
		List<BaseEntity> result = new ArrayList<>();

		BaseEntity parent = CacheUtils.getObject(productCode, beCode, BaseEntity.class);

		if (parent != null) {
			for (EntityEntity ee : parent.getLinks()) {
				String childCode = ee.getLink().getTargetCode();
				BaseEntity child = CacheUtils.getObject(productCode, childCode,
						BaseEntity.class);
				result.add(child);
			}
		}

		return result;
	}

	/**
	 * Perform a merge of ask data.
	 *
	 * @param ask the ask to merge
	 * @return Ask
	 */
	private Ask performMerge(Ask ask) {

		if (ask.getName().contains("{{")) {
			// now merge in data
			String name = ask.getName();

			Map<String, Object> templateEntityMap = new HashMap<>();
			ContextList contexts = ask.getContextList();
			for (Context context : contexts.getContexts()) {
				BaseEntity be = context.getEntity();
				templateEntityMap.put(context.getName(), be);
			}
			String mergedName = MergeUtils.merge(name, templateEntityMap);
			ask.setName(mergedName);
		}
		return ask;

	}

	public void sendQuestions(final String rootQuestionCode, BaseEntity recipient) {

		QDataAskMessage askMsg = getAsks(userToken.getUserCode(), 
				recipient.getCode(), rootQuestionCode);
		sendQuestions(askMsg, recipient);
	}

	public void sendQuestions(QDataAskMessage askMsg, BaseEntity target) {

		log.info("AskMsg=" + askMsg);

		QCmdMessage msg = new QCmdMessage("DISPLAY", "FORM");
		msg.setToken(userToken.getToken());

		KafkaUtils.writeMsg("webcmds", msg);

		QDataBaseEntityMessage beMsg = new QDataBaseEntityMessage(target);
		beMsg.setToken(userToken.getToken());

		KafkaUtils.writeMsg("webdata", beMsg);

		askMsg.setToken(userToken.getToken());
		KafkaUtils.writeMsg("webdata", askMsg);
	}

}
