package life.genny.lauchy.streams;

import io.quarkus.runtime.StartupEvent;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.graphql.ProcessData;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.message.QDataAnswerMessage;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.*;
import life.genny.serviceq.Service;
import life.genny.serviceq.intf.GennyScopeInit;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ApplicationScoped
public class TopologyProducer {

	static Logger log = Logger.getLogger(TopologyProducer.class);

	Jsonb jsonb = JsonbBuilder.create();

	@ConfigProperty(name = "genny.enable.blacklist", defaultValue = "true")
	Boolean enableBlacklist;

	@Inject
	GennyScopeInit scope;

	@Inject
	Service service;

	@Inject
	UserToken userToken;

	@Inject
	DefUtils defUtils;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	GraphQLUtils gqlUtils;

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	FilterUtils filter;

	void onStart(@Observes StartupEvent ev) {

		if (service.showValues()) {
			log.info("Blacklist        :" + (enableBlacklist ? "ON" : "OFF"));
		}

		service.fullServiceInit(true);
		log.info("[*] Finished Topology Startup!");
	}

	@Produces
	public Topology buildTopology() {

		// Read the input Kafka topic into a KStream instance.
		StreamsBuilder builder = new StreamsBuilder();
		builder
				.stream("data", Consumed.with(Serdes.String(), Serdes.String()))
				.peek((k, v) -> scope.init(v))
				.peek((k, v) -> log.info("Received message: " + stripToken(v)))
				.filter((k, v) -> (v != null))
				.mapValues((k, v) -> tidy(v))
				.filter((k, v) -> validateData(v))
				.mapValues((k, v) -> handleDependentDropdowns(v))
				.peek((k, v) -> log.info("Forwarding valid message"))
				.to("valid_data", Produced.with(Serdes.String(), Serdes.String()));

		return builder.build();
	}

	/**
	 * Helper function to show the data without a token
	 * 
	 * @param data
	 * @return
	 */
	public String stripToken(String data) {
		JsonObject dataJson = jsonb.fromJson(data, JsonObject.class);
		return javax.json.Json.createObjectBuilder(dataJson).remove("token").build().toString();
	}

	/**
	 * @param data
	 * @return
	 */
	public String handleDependentDropdowns(String data) {

		QDataAnswerMessage answers = jsonb.fromJson(data, QDataAnswerMessage.class);
		List<Ask> asksToSend = new ArrayList<>();

		Arrays.stream(answers.getItems())
				.filter(answer -> answer.getAttributeCode() != null && answer.getAttributeCode().startsWith(Prefix.LNK))
				.forEach(answer -> {
					String processId = answer.getProcessId();
					// TODO: Wondering if we can just get the processData from the first processId
					// we get
					ProcessData processData = qwandaUtils.fetchProcessData(processId);
						if(processData !=null) {
							List<Ask> asks = qwandaUtils.fetchAsks(processData);

							BaseEntity defBE = beUtils.getBaseEntity(processData.getDefinitionCode());
							BaseEntity processEntity = qwandaUtils.generateProcessEntity(processData);

							Map<String, Ask> flatMapAsks = qwandaUtils.buildAskFlatMap(asks);

							qwandaUtils.updateDependentAsks(processEntity, defBE, flatMapAsks);
							asksToSend.addAll(asks);
						}
				});

		QDataAskMessage msg = new QDataAskMessage(asksToSend);
		msg.setReplace(true);
		msg.setToken(userToken.getToken());
		KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);

		return data;
	}

	/**
	 * Helper function to tidy some values
	 * 
	 * @param data
	 * @return
	 */
	public String tidy(String data) {

		return data.replaceAll("Adamm", "Adam");
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

		// check that user is the source of message
		if (!(userToken.getUserCode()).equals(answer.getSourceCode())) {
			log.errorf("UserCode %s does not match answer source %s",
					userToken.getUserCode(),
					answer.getSourceCode());
			return blacklist();
		}

		// check processId is not blank
		String processId = answer.getProcessId();
		log.info("CHECK Integrity of processId [" + processId + "]");
		if(processId != null && processId.equalsIgnoreCase("no-idq") ) {
			//TODO : Temporary solution and rethink for filter and saved search
			if(filter.validFilter(attributeCode)) {
				return true;
			}
		}

		if (StringUtils.isBlank(processId)) {
			log.error("ProcessId is blank");
			return blacklist();
		}

		// Check if inferredflag is set
		if (answer.getInferred()) {
			log.error("InferredFlag is set");
			return blacklist();
		}

		// fetch process data from graphql
		ProcessData processData = qwandaUtils.fetchProcessData(processId);
		log.debug("Returned processData for (pid=" + processId + ")=" + processData);
		if (processData == null) {
			log.error("Could not find process instance variables for processId [" + processId + "]");
			return false;
		}

		if (processData.getAttributeCodes() == null) {
			log.error("AttributeCodes null");
			return blacklist();
		}

		if (!processData.getAttributeCodes().contains(attributeCode)) {
			log.error("AttributeCode " + attributeCode + " does not existing");
			return blacklist();
		}

		// check target is same
		BaseEntity target = qwandaUtils.generateProcessEntity(processData);
		if (!target.getCode().equals(answer.getTargetCode())) {
			log.warn("TargetCode " + target.getCode() + " does not match answer target " + answer.getTargetCode());
			return blacklist();
		}

		BaseEntity originalTarget = beUtils.getBaseEntity(processData.getTargetCode());
		// TODO: The attribute should be retrieved from askMessage
		Attribute attribute = qwandaUtils.getAttribute(attributeCode);

		for (String defCode : processData.getDefCodes()) {
			BaseEntity definition = beUtils.getBaseEntity(defCode);
			log.infof("Definition %s found for target %s", definition.getCode(), answer.getTargetCode());
			if (definition.findEntityAttribute("UNQ_" + attributeCode).isPresent()) {
				if (qwandaUtils.isDuplicate(definition, answer, target, originalTarget)) {
					log.error("Duplicate answer detected for target " + answer.getTargetCode());
					String feedback = "Error: This value already exists and must be unique.";

					String parentCode = processData.getQuestionCode();
					String questionCode = answer.getCode();

					qwandaUtils.sendAttributeErrorMessage(parentCode, questionCode, attributeCode, feedback);
					return false;
				}
			}
			// check attribute code is allowed by target DEF
			if (!definition.containsEntityAttribute("ATT_" + attributeCode)) {
				log.error("AttributeCode " + attributeCode + " not allowed for " + definition.getCode());
				return blacklist();
			}
		}

		temporaryBucketSearchHandler(answer, target, attribute);

		// handleBucketSearch(ans)
		if ("PRI_ABN".equals(attributeCode)) {

			if (isValidABN(answer.getValue())) {
				return true;
			}
			log.errorf("invalid ABN %s", answer.getValue());
			return blacklist();
		}

		if ("PRI_CREDITCARD".equals(attributeCode)) {

			if (isValidCreditCard(answer.getValue())) {
				return true;
			}
			log.errorf("invalid Credit Card %s", answer.getValue());
			return blacklist();
		}

		// check if answer is null
		if (answer.getValue() == null) {
			log.warnf("Received a null answer value from: %s, for: %s", userToken.getUserCode(),
					attributeCode);
			return true;
		}

		// blacklist if none of the regex match
		if (!qwandaUtils.validationsAreMet(attribute, answer.getValue())) {
			log.info("Answer Value is bad: " + answer.getValue());
			return blacklist();
		} else {
			log.info("Answer Value is good: " + answer.getValue());
		}

		return true;
	}

	/**
	 * Blacklist the user if blacklists are enabled, and return
	 * a Boolean representing whether or not the messsage
	 * should be considered valid.
	 *
	 * @return Boolean
	 */
	public Boolean blacklist() {

		String uuid = userToken.getUuid();

		log.info("BLACKLIST " + (enableBlacklist ? "ON" : "OFF") + " " + userToken.getEmail() + ":" + uuid);

		if (!enableBlacklist) {
			return true;
		}

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
	public static Boolean isValidABN(final String abnCode) {

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
				target.setValue(attribute, "[]");

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
