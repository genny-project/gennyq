package life.genny.lauchy.streams;

import io.quarkus.runtime.StartupEvent;

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
import javax.json.bind.JsonbConfig;

import life.genny.qwandaq.Answer;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.ProcessVariables;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

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

		log.info("Initializing ServiceQ Services");
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
				.filter((k, v) -> validate(v))
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
	 * @return Boolean
	 */
	public Boolean validate(String data) {

		JsonObject json = jsonb.fromJson(data, JsonObject.class);

		if(json.containsKey("empty")) {
			log.info("Detected a payload with empty=false.. ignoring & proceding..");
			return false;
		}

		JsonArray items = json.getJsonArray("items");

		if (items.size() == 0) {
			log.info("Detected a payload with empty items.. ignoring & proceding..");
			return false;
		}

		for (int i = 0; i < items.size(); i++) {

			Answer answer = jsonb.fromJson(items.get(i).toString(), Answer.class);
			String attributeCode = answer.getAttributeCode();
			String processAttributeCode = null;

			// TODO: check questionCode by fetching from Questions
			// TODO: check askID by fetching from Tasks

			// check that user is the source of message
			if (!(userToken.getUserCode()).equals(answer.getSourceCode())) {
				// log.errorv("UserCode {} does not match answer source {}",
				// userToken.getUserCode(), answer.getSourceCode());
				log.error("UserCode " + userToken.getUserCode() + " does not match answer source "
						+ answer.getSourceCode());
				return blacklist();
			}

				String processId = null;
		if (answer.getProcessId() != null) {
			processId = answer.getProcessId();
		} else {
			JsonObject nonTokenJson = json;
			if (nonTokenJson.containsKey("token")) {
				 nonTokenJson = Json.createObjectBuilder(nonTokenJson).remove("token").build();
			}
			
			log.error("No processId in DD Event "+nonTokenJson);
		}
	
		BaseEntity target = null;
		BaseEntity defBE = null;
		String askMessageJson = null;

		if (!StringUtils.isBlank(processId)) {
			// This means that the target should come from the graphql
			ProcessVariables processVariables = fetchProcessInstanceProcessBE(processId);
			if (processVariables == null) {
				log.error("Could not find process instance variables for processId [" + processId + "]");
				return false;
			}
			target = processVariables.getProcessEntity();
			defBE = beUtils.getBaseEntityOrNull(processVariables.getDefinitionCode());
			askMessageJson = processVariables.getAskMessageJson();
			// Check integrity
			log.info("CHECK Integrity of processId [" + processId + "]");
			if (!target.getCode().equals(answer.getTargetCode())) {
				log.warn("TargetCode " + target.getCode() + " does not match answer target "
						+ answer.getTargetCode() + " BLACKLISTED");
				return blacklist();
			}
			Optional<EntityAttribute> fieldAttribute= target.findEntityAttribute(attributeCode);
			if (!fieldAttribute.isPresent()) {
				log.warn("AttributeCode " +attributeCode+" is not present in the target so BLACKLISTED!"
					);
				return blacklist();
			}
		} else {
			target = beUtils.getBaseEntityOrNull(answer.getTargetCode());
			if (target == null) {
				return false;
			}
		}

		// Find the DEF
		if (defBE == null) {
			defBE = defUtils.getDEF(target);
		}
		if (defBE == null) {
			log.error("No DEF found for target " + answer.getTargetCode());
			return false;
		}
		log.info("Full DEF BE -->"+defBE.getCode()+" Found fine for target " + answer.getTargetCode());

		// Check if attribute code exists as a UNQ for the DEF
		Optional<EntityAttribute> uniqueAttribute = defBE.findEntityAttribute("UNQ_" + attributeCode);

		if (uniqueAttribute.isPresent()) {
			log.info("Target: " + target.getCode() + ", Definition: " + defBE.getCode()
					+ ", Attribute found for UNQ_" + attributeCode + " LOOKING for duplicate using "+uniqueAttribute.get().getValue());
					
			// Check if the value is already in the database
			//databaseUtils.findValueByAttributeCount(productCode, target.getCode(), uniqueAttribute.get().getValue(), answer.getValue());
			// if duplicate found then send back the baseentity with the conflicting attribute and feedback message to display

			//return false;
		} else {
			log.info("uniqueAttribute is Not present! for UNQ_" + attributeCode);
		}

			// check source entity exists
			BaseEntity sourceBe = beUtils.getBaseEntityByCode(answer.getSourceCode());
			if (sourceBe == null) {
				log.error("Source " + answer.getSourceCode() + " does not exist");
				return blacklist();
			}
			log.info("Source = " + sourceBe.getCode() + ":" + sourceBe.getName());

			// TODO: do this better
			// The attribute should be in askMessageJson

			Attribute attribute = qwandaUtils.getAttribute(answer.getAttributeCode());
			if (attribute == null) {
				log.error("Attribute " + answer.getAttributeCode() + " does not exist");
				return blacklist();
			}

			// check target entity exist
			if (StringUtils.isBlank(processId)) {
				BaseEntity targetBe = beUtils.getBaseEntityByCode(answer.getTargetCode());
				if (targetBe == null) {
					log.error("Target " + answer.getTargetCode() + " does not exist");
					return blacklist();
				}

				// check DEF was found for target
				BaseEntity defBe = defUtils.getDEF(targetBe);
				if (defBe == null) {
					// log.errorv("DEF entity not found for {}", targetBe.getCode());
					log.error("DEF entity not found for " + targetBe.getCode());
					return blacklist();
				}

				// check attribute code is allowed by targetDEF
				if (!defBe.containsEntityAttribute("ATT_" + answer.getAttributeCode())) {
					// log.errorv("AttributeCode {} not allowed for {}", answer.getAttributeCode(),
					// defBe.getCode());
					log.error("AttributeCode " + answer.getAttributeCode() + " not allowed for " + defBe.getCode());
					return blacklist();
				}

				// check attribute exists
				// TODO: do this better
				if (!askMessageJson.contains(answer.getAttributeCode())) {
					// log.errorv("AttributeCode {} does not existing", answer.getAttributeCode());
					log.error("AttributeCode " + answer.getAttributeCode() + " does not existing");
					return blacklist();
				}
			}

			DataType dataType = attribute.getDataType();

			// HACK: TODO ACC - To send back an empty LNK_PERSON for a bucket search
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

			if ("PRI_ABN".equals(answer.getAttributeCode())) {

				if (!isValidABN(answer.getValue())) {
					// log.errorv("invalid ABN {}", answer.getValue());
					log.error("invalid ABN " + answer.getValue());
					return blacklist();
				}

			} else if ("PRI_CREDITCARD".equals(answer.getAttributeCode())) {

				if (!isValidCreditCard(answer.getValue())) {
					// log.errorv("invalid Credit Card {}", answer.getValue());
					log.error("invalid Credit Card " + answer.getValue());
					return blacklist();
				}

			} else {
				Boolean isAnyValid = false;

				// check the answer field and allow through if null
				if (answer.getValue() == null) {
					log.warn("Received a null answer field from: " + userToken.getUserCode() + ", for: "
							+ answer.getAttributeCode());
					isAnyValid = true;
					continue;
				}

				for (Validation validation : dataType.getValidationList()) {

					// Now check the validation
					String regex = validation.getRegex();
					log.info("Answer Value: " + answer.getValue());
					boolean regexOk = Pattern.compile(regex).matcher(answer.getValue()).matches();

					if (regexOk) {
						isAnyValid = true;
						// log.infov("Regex OK! [{}] for regex {}", answer.getValue().toString(),
						// regex);
						log.info("Regex OK! [ " + answer.getValue() + " ] for regex " + regex);
						break;
					}
					// log.errorv("Regex failed! Att: [{}] {} [{}] for regex {} ... {}",
					// answer.getAttributeCode(),
					// attribute.getDataType().getDttCode(),
					// answer.getValue(),
					// regex,
					// validation.getErrormsg());
					log.error("Regex failed! " + regex + " ... " + validation.getErrormsg());
				}

				// blacklist if none of the regex match
				if (!isAnyValid) {
					return blacklist();
				}
			}
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
	 * Fetch the targetCode stored in the processInstance 
	 * for the given processId.
	 */
	public ProcessVariables fetchProcessInstanceProcessBE(String processId) {
		BaseEntity processBe = null;
		String defCode = null;
		String processBeStr = null;
		String askMessageJsonStr = null;

		log.info("Fetching processBE for processId : " + processId);

		// check in cache first (But not ready yet, processQuestions would need to save
		// the processBe into cache every answer received)
		String processVariablesStr = CacheUtils.getObject(userToken.getProductCode(), processId + ":PROCESS_BE",
				String.class);
		if (!StringUtils.isBlank(processVariablesStr)) {
			log.info("ProcessVariables fetched from cache");
			ProcessVariables processVariables = null;
			try {
				processVariables = jsonb.fromJson(processVariablesStr, ProcessVariables.class);
			} catch (Exception e) {
				log.error("Error parsing processVariables from cache: " + processVariablesStr);
			}
			return processVariables;
		} else {
			log.info("ProcessVariables for " + processId + ":PROCESS_BE not found in cache");
		}

		JsonArray array = gqlUtils.queryTable("ProcessInstances", "id", processId, "variables");
		if (array.isEmpty()) {
			log.error("Nothing found for processId: " + processId);
			return null;
		}
		JsonObject variables = jsonb.fromJson(array.getJsonObject(0).getString("variables"), JsonObject.class);

		// grab the targetCode from process questions variables
		processBeStr = variables.getString("processBEJson");
		askMessageJsonStr = variables.getString("askMessageJson");
		defCode = variables.containsKey("defCode") ? variables.getString("defCode") : null;
		processBe = jsonb.fromJson(processBeStr, BaseEntity.class);

		if (defCode == null) {
			BaseEntity defBE = defUtils.getDEF(processBe);
			defCode = defBE.getCode();
		}
		ProcessVariables processVariables = new ProcessVariables();
		processVariables.setProcessEntity(processBe);
		processVariables.setDefinitionCode(defCode);
		processVariables.setAskMessageJson(askMessageJsonStr);

		// cache
		CacheUtils.putObject(userToken.getProductCode(), processId + ":PROCESS_BE", processVariables);

		return processVariables;
	}
}
