package life.genny.lauchy.streams;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.graphql.ProcessData;
import life.genny.qwandaq.message.QDataAnswerMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.DefUtils;
import life.genny.qwandaq.utils.GraphQLUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.validation.Validation;
import life.genny.serviceq.Service;
import life.genny.serviceq.intf.GennyScopeInit;

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
			log.info("Detected a payload with empty items.. ignoring & proceding..");
			return false;
		}

		try {
			for (Answer answer : msg.getItems()) {
				if (!validateAnswer(data, answer))
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
	public Boolean validateAnswer(String data, Answer answer) {

		// TODO: check questionCode by fetching from Questions
		// TODO: check askID by fetching from Tasks

		// check that user is the source of message
		if (!(userToken.getUserCode()).equals(answer.getSourceCode())) {
			log.errorf("UserCode %s does not match answer source %s",
					userToken.getUserCode(),
					answer.getSourceCode());
			return blacklist();
		}

		// check processId is no blank
		String processId = answer.getProcessId();
		log.info("CHECK Integrity of processId [" + processId + "]");
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
		ProcessData processData = gqlUtils.fetchProcessData(processId);
		log.debug("Returned processData for (pid=" + processId + ")=" + processData);
		if (processData == null) {
			log.error("Could not find process instance variables for processId [" + processId + "]");
			return false;
		}

		// check target is same
		BaseEntity target = qwandaUtils.generateProcessEntity(processData);
		if (!target.getCode().equals(answer.getTargetCode())) {
			log.warn("TargetCode " + target.getCode() + " does not match answer target " + answer.getTargetCode());
			return blacklist();
		}

		BaseEntity defBE = beUtils.getBaseEntity(processData.getDefinitionCode());
		log.infof("Definition %s found for target %s", defBE.getCode(), answer.getTargetCode());

		String attributeCode = answer.getAttributeCode();
		Optional<EntityAttribute> fieldAttribute = target.findEntityAttribute(attributeCode);
		if (fieldAttribute.isEmpty()) {
			log.errorf("AttributeCode %s is not present", attributeCode);
			return blacklist();
		}

		// check duplicate attributes
		String questionCode = processData.getQuestionCode();
		String key = String.format("%s:%s", processId, questionCode);
		Ask ask = CacheUtils.getObject(userToken.getProductCode(), key, Ask.class);
		if (qwandaUtils.isDuplicate(target, defBE, answer.getAttributeCode(), answer.getValue())) {
			log.error("Duplicate answer detected for target " + answer.getTargetCode());
			qwandaUtils.sendSubmit(ask, false);

			JsonObject dataJson = jsonb.fromJson(data, JsonObject.class);
			JsonArray items = dataJson.getJsonArray("items");

			// dumbly loop through items until matching answer. This is ugly
			String code = "";
			for (int i = 0; i < items.size(); i++) {
				JsonObject item = items.getJsonObject(i);
				log.info("item:" + item.toString());
				if (item.getString("attributeCode").equals(answer.getAttributeCode())) {
					code = item.getString("code");
					break;
				}
			}

			log.info(dataJson.toString());

			// send a special FIELDMSG
			String cmd_type = "FIELDMSG";
			String attrCode = answer.getAttributeCode();
			JsonObject errorMsgJson = Json.createObjectBuilder()
					.add("cmd_type", cmd_type)
					.add("msg_type", "CMD_MSG")
					.add("code", code)
					.add("attributeCode", attrCode)
					.add("questionCode", questionCode)
					.add("message", Json.createObjectBuilder()
							.add("value", "This field must be unique and not have already been selected ")
							.build())
					.add("token", userToken.getToken())
					.build();
			log.info("errorMsg:" + errorMsgJson.toString());
			KafkaUtils.writeMsg("webcmds", errorMsgJson.toString());

			return false;
		}

		// TODO: The attribute should be retrieved from askMessage
		Attribute attribute = qwandaUtils.getAttribute(answer.getAttributeCode());

		// check attribute code is allowed by target DEF
		if (!defBE.containsEntityAttribute("ATT_" + answer.getAttributeCode())) {
			log.error("AttributeCode " + answer.getAttributeCode() + " not allowed for " + defBE.getCode());
			return blacklist();
		}

		if (!jsonb.toJson(ask).contains(answer.getAttributeCode())) {
			log.error("AttributeCode " + answer.getAttributeCode() + " does not existing");
			return blacklist();
		}

		temporaryBucketSearchHandler(answer, target, attribute);

		// handleBucketSearch(ans)
		if ("PRI_ABN".equals(answer.getAttributeCode())) {

			if (isValidABN(answer.getValue())) {
				return true;
			}
			log.errorf("invalid ABN %s", answer.getValue());
			return blacklist();
		}

		if ("PRI_CREDITCARD".equals(answer.getAttributeCode())) {

			if (isValidCreditCard(answer.getValue())) {
				return true;
			}
			log.errorf("invalid Credit Card %s", answer.getValue());
			return blacklist();
		}

		// check if answer is null
		if (answer.getValue() == null) {
			log.warnf("Received a null answer value from: %s, for: %s", userToken.getUserCode(),
					answer.getAttributeCode());
			return true;
		}

		// blacklist if none of the regex match
		if (!validationsAreMet(answer, attribute))
			return blacklist();

		return true;
	}

	/**
	 * Check if all validations are met for an answer.
	 * 
	 * @param answer    The answer to check
	 * @param attribute The Attribute of the answer
	 * @return Boolean representing whether the validation conditions have been met
	 */
	public Boolean validationsAreMet(Answer answer, Attribute attribute) {

		log.info("Answer Value: " + answer.getValue());
		DataType dataType = attribute.getDataType();

		// check each validation against value
		for (Validation validation : dataType.getValidationList()) {

			String regex = validation.getRegex();
			boolean regexOk = Pattern.compile(regex).matcher(answer.getValue()).matches();

			if (!regexOk) {
				log.error("Regex FAILED! " + regex + " ... " + validation.getErrormsg());
				return false;
			}
			log.info("Regex OK! [ " + answer.getValue() + " ] for regex " + regex);
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

		KafkaUtils.writeMsg("blacklist", uuid);
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

				KafkaUtils.writeMsg("webdata", responseMsg);
				log.info("Detected cleared BKT_APPLICATIONS search from " + userToken.getEmailUserCode());

			} catch (BadDataException e) {
				e.printStackTrace();
			}
		}
	}

}
