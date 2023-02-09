package life.genny.lauchy;
 
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.utils.EntityAttributeUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.Definition;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.graphql.ProcessData;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.message.QDataAnswerMessage;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;

@ApplicationScoped
public class Validator {
    @Inject
    private Logger log;

	private Jsonb jsonb = JsonbBuilder.create();

	@ConfigProperty(name = "genny.enable.blacklist", defaultValue = "true")
	private Boolean enableBlacklist;

	@Inject
	UserToken userToken;

	@Inject
	CacheManager cm;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	EntityAttributeUtils beaUtils;

    /**
	 * @param data
	 * @return
	 */
	public String handleDependentDropdowns(String data) {

		QDataAnswerMessage answers = jsonb.fromJson(data, QDataAnswerMessage.class);
		Set<Ask> asksToSend = new HashSet<>();

		Arrays.stream(answers.getItems())
				.filter(answer -> answer.getAttributeCode() != null && answer.getAttributeCode().startsWith(Prefix.LNK))
				.forEach(answer -> {
					String processId = answer.getProcessId();
					// TODO: Wondering if we can just get the processData from the first processId we get
					ProcessData processData = qwandaUtils.fetchProcessData(processId);
					Set<Ask> asks = qwandaUtils.fetchAsks(processData);

					Definition definition = beUtils.getDefinition(processData.getDefinitionCode());
					BaseEntity processEntity = qwandaUtils.generateProcessEntity(processData);

					Map<String, Ask> flatMapAsks = QwandaUtils.buildAskFlatMap(asks);

					qwandaUtils.updateDependentAsks(processEntity, definition, flatMapAsks);
					asksToSend.addAll(asks);
				});

		QDataAskMessage msg = new QDataAskMessage(asksToSend);
		msg.setReplace(true);
		msg.setToken(userToken.getToken());
		KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);

		return data;
	}

	/**
	 * Function for validating a data message.
	 * 
	 * @param data the data to validate
	 * @return Boolean representing whether the msg is valid
	 */
	public Boolean validateData(String data) {

		QDataAnswerMessage msg = jsonb.fromJson(data, QDataAnswerMessage.class);
		if (msg.getItems().length == 0) {
			log.warn("Detected a payload with empty items.. ignoring & proceding..");
			return false;
		}

		try {
			for (Answer answer : msg.getItems()) {
				if (!validateAnswer(answer))
					break;

				return true;
			}
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		}

		return false;
	}

	/**
	 * Function for validating an asnwer.
	 * 
	 * @param answer the answer to validate
	 * @return Boolean representing whether the answer is valid
	 */
	public Boolean validateAnswer(Answer answer) {

		// TODO: check questionCode by fetching from Questions
		// TODO: check askID by fetching from Tasks

		String attributeCode = answer.getAttributeCode();

		// name search
		if (Attribute.PRI_SEARCH_TEXT.equals(attributeCode)) {
			return true;
		}

		// check that user is the source of message
		if (!(userToken.getUserCode()).equals(answer.getSourceCode()))
			return blacklist(String.format("UserCode %s does not match answer source %s", userToken.getUserCode(), answer.getSourceCode()));

		// check processId is not blank
		String processId = answer.getProcessId();
		log.info("CHECK Integrity of processId [" + processId + "]");
		if (StringUtils.isBlank(processId))
			return blacklist("ProcessId is blank");

		// Check if inferredflag is set
		if (answer.getInferred())
			return blacklist("InferredFlag is set");

		// fetch process data from graphql
		ProcessData processData = qwandaUtils.fetchProcessData(processId);
		log.debug("Returned processData for (pid=" + processId + ")=" + processData);
		if (processData == null) {
			log.error("Could not find process instance variables for processId [" + processId + "]");
			return false;
		}

		if (processData.getAttributeCodes() == null)
			return blacklist("AttributeCodes null");

		if (!processData.getAttributeCodes().contains(attributeCode))
			return blacklist("AttributeCode " + attributeCode + " does not existing");

		// check target is same
		BaseEntity target = qwandaUtils.generateProcessEntity(processData);
		if (!target.getCode().equals(answer.getTargetCode()))
			return blacklist("TargetCode " + target.getCode() + " does not match answer target " + answer.getTargetCode());

		Definition definition = beUtils.getDefinition(processData.getDefinitionCode());
		log.infof("Definition %s found for target %s", definition.getCode(), answer.getTargetCode());

		BaseEntity originalTarget = beUtils.getBaseEntity(processData.getTargetCode());

		EntityAttribute baseEntityAttribute = beaUtils.getEntityAttribute(definition.getRealm(), definition.getCode(), "UNQ_" + attributeCode);
		if (baseEntityAttribute != null) {
			if (qwandaUtils.isDuplicate(definition, answer, target, originalTarget)) {
				log.error("Duplicate answer detected for target " + answer.getTargetCode());
				String feedback = "Error: This value already exists and must be unique.";

				String parentCode = processData.getQuestionCode();
				String questionCode = answer.getCode();

				qwandaUtils.sendAttributeErrorMessage(parentCode, questionCode, attributeCode, feedback);
				return false;
			}
		}

		// TODO: The attribute should be retrieved from askMessage
		Attribute attribute = cm.getAttribute(attributeCode);

		// check attribute code is allowed by target DEF
		if (!definition.containsEntityAttribute("ATT_" + attributeCode))
			return blacklist("AttributeCode " + attributeCode + " not allowed for " + definition.getCode());

		temporaryBucketSearchHandler(answer, target, attribute);

		// handleBucketSearch(ans)
		if ("PRI_ABN".equals(attributeCode)) {
			if (isValidABN(answer.getValue()))
				return true;
			return blacklist(String.format("invalid ABN %s", answer.getValue()));
		}

		if ("PRI_CREDITCARD".equals(attributeCode)) {
			if (isValidCreditCard(answer.getValue()))
				return true;
			return blacklist(String.format("invalid Credit Card %s", answer.getValue()));
		}

		// check if answer is null
		if (answer.getValue() == null) {
			log.warnf("Received a null answer value from: %s, for: %s", userToken.getUserCode(),
					attributeCode);
			return true;
		}

		// blacklist if none of the regex match
		if (!qwandaUtils.validationsAreMet(attribute, answer.getValue()))
			return blacklist("Answer Value is bad: " + answer.getValue());
		log.info("Answer Value is good: " + answer.getValue());

		return true;
	}

	/**
	 * Blacklist the user if blacklists are enabled, and return
	 * a Boolean representing whether or not the messsage
	 * should be considered valid.
	 *
	 * @return Boolean
	 */
	public Boolean blacklist(String err) {
		String uuid = userToken.getUuid();
		log.error(err);
		log.info("BLACKLIST " + (enableBlacklist ? "ON" : "OFF") + " " + userToken.getEmail() + ":" + uuid);

		if (enableBlacklist)
			KafkaUtils.writeMsg(KafkaTopic.BLACKLIST, uuid);

		return false;
	}

	/**
	 * Helper function for checking ABN validity.
	 *
	 * Thanks to "Joker" from stackOverflow -
	 * https://stackoverflow.com/users/3949925/joker
	 *
	 * @param abnCode The ABN to check
	 * @return {@link Boolean}: ABN is valid
	 */
	public Boolean isValidABN(final String abnCode) {

		if (abnCode.matches("[0-9]+") && abnCode.length() != 11) {
			return false;
		}
		final int[] weights = { 10, 1, 3, 5, 7, 9, 11, 13, 15, 17, 19 };
		// split abn number string by digits to get int array
		try {
			int[] abnDigits = Stream.of(abnCode.split("\\B")).mapToInt(Integer::parseInt).toArray();
			// reduce by applying weight[index] * abnDigits[index] (NOTE: substract 1 for
			// the first digit in abn number)
			int sum = IntStream.range(0, weights.length).reduce(0,
					(total, idx) -> total + weights[idx] * (idx == 0 ? abnDigits[idx] - 1 : abnDigits[idx]));
			return (sum % 89 == 0);
		} catch (NumberFormatException e) {
			log.error("Attempted to parse valid ABN of: " + abnCode);
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			log.error("Attempted to parse valid ABN of: " + abnCode);
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Helper function to check if a credit card number is valid
	 *
	 * @param creditCardNumber Credit card number to check
	 * @return {@link Boolean}: Number is valid
	 */
	public static boolean isValidCreditCard(String creditCardNumber) {
		int sum = 0;
		boolean alternate = false;
		for (int i = creditCardNumber.length() - 1; i >= 0; i--) {
			int n = Integer.parseInt(creditCardNumber.substring(i, i + 1));
			if (alternate) {
				n *= 2;
				if (n > 9) {
					n = (n % 10) + 1;
				}
			}
			sum += n;
			alternate = !alternate;
		}
		return (sum % 10 == 0);
	}

	/**
	 * A Temporary handler for the bucket quick search.
	 * 
	 * @param answer    The Answer to handle
	 * @param target    The target entity
	 * @param attribute The attribute used
	 */
	// NOTE: This should be removed soon and rethought
	public void temporaryBucketSearchHandler(Answer answer, BaseEntity target, Attribute attribute) {

		if (("LNK_PERSON".equals(answer.getAttributeCode()))
				&& ("BKT_APPLICATIONS".equals(answer.getTargetCode()))
				&& ("[]".equals(answer.getValue()))) {

			// So send back a dummy empty value for the LNK_PERSON
			try {
				EntityAttribute entityAttribute = beaUtils.getEntityAttribute(target.getRealm(), target.getCode(), attribute.getCode());
				if (entityAttribute == null) {
					entityAttribute = new EntityAttribute(target, attribute, 0.0, "[]");
					beaUtils.updateEntityAttribute(entityAttribute);
				}
				target.addAttribute(entityAttribute);
				QDataBaseEntityMessage responseMsg = new QDataBaseEntityMessage(target);
				responseMsg.setTotal(1L);
				responseMsg.setReturnCount(1L);
				responseMsg.setToken(userToken.getToken());

				KafkaUtils.writeMsg(KafkaTopic.WEBDATA, responseMsg);
				log.info("Detected cleared BKT_APPLICATIONS search from " + userToken.getEmailUserCode());

			} catch (BadDataException e) {
				e.printStackTrace();
			}
		}
	}

}
