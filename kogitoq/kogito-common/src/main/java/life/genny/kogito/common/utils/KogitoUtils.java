package life.genny.kogito.common.utils;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonValue;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.Response;

import life.genny.qwandaq.managers.CacheManager;
import org.jboss.logging.Logger;
import org.kie.api.runtime.KieRuntimeBuilder;
import org.kie.api.runtime.KieSession;
import org.apache.commons.lang3.StringUtils;

import life.genny.kogito.common.service.BaseEntityService;
import life.genny.kogito.common.service.ImportGithubService;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.message.QDataAnswerMessage;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DefUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.utils.GraphQLUtils;
import life.genny.qwandaq.utils.HttpUtils;
import life.genny.qwandaq.exception.checked.GraphQLException;

/*
 * A static utility class used for standard Kogito interactions
 * 
 * @author Adam Crow
 */
@ApplicationScoped
public class KogitoUtils {

	private static final Logger log = Logger.getLogger(KogitoUtils.class);
	private static Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	KogitoUtils kogitoUtils;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	GraphQLUtils gqlUtils;

	@Inject
	DefUtils defUtils;

	@Inject
	KieRuntimeBuilder kieRuntimeBuilder;

	@Inject
	ImportGithubService importGithubService;

	@Inject
	BaseEntityService baseEntityService;

	@Inject
	CacheManager cacheManager;

	public static enum UseService {
		SELF(GennySettings.kogitoServiceUrl()),
		GADAQ(GennySettings.gadaqServiceUrl());

		private final String uri;

		private UseService(String uri) {
			this.uri = uri;
		}

		public String getServiceUrl() {
			return uri;
		}
	}

	/**
	 * Send a workflow signal
	 *
	 * @param workflowId The workflow Id
	 * @param processId  The process Id
	 * @param signal     the signal code
	 */
	public String sendSignal(final UseService useService, final String workflowId, final String processId,
			final String signal) {

		return sendSignal(useService, workflowId, processId, signal, "");
	}

	/**
	 * Send a workflow signal
	 *
	 * @param workflowId The workflow Id
	 * @param processId  The process Id
	 * @param signal     the signal code
	 * @param key        The key of of the item to send in the payload
	 * @param value      The value of of the item to send in the payload
	 */
	public String sendSignal(final UseService useService, final String workflowId, final String processId,
			final String signal, String key, String value) {

		// build json with key and value
		JsonObject payload = Json.createObjectBuilder()
				.add(key, value)
				.build();

		return sendSignal(useService, workflowId, processId, signal, payload);
	}

	/**
	 * Send a workflow signal
	 *
	 * @param workflowId The workflow Id
	 * @param processId  The process Id
	 * @param signal     the signal code
	 * @param payload    the payload to send as a json object
	 */
	public String sendSignal(final UseService useService, final String workflowId, final String processId,
			final String signal, JsonObject payload) {

		// add token to JsonObject
		JsonObjectBuilder builder = Json.createObjectBuilder();
		payload.forEach(builder::add);
		builder.add("token", userToken.getToken());
		builder.add("userToken", jsonb.toJson(userToken));
		payload = builder.build();

		return sendSignal(useService, workflowId, processId, signal, payload.toString());
	}

	/**
	 * Send a workflow signal
	 *
	 * @param workflowId The workflow Id
	 * @param processId  The process Id
	 * @param signal     the signal code
	 * @param payload    Th payload to send
	 */
	public String sendSignal(final UseService useService, final String workflowId, final String processId,
			final String signal, final String payload) {

		String uri = selectServiceURI(useService) + "/" + workflowId + "/" + processId + "/" + signal;
		log.info("Sending Signal to uri: " + uri);

		HttpResponse<String> response = HttpUtils.post(uri, payload, "application/json", userToken);
		if((int)Math.floor(response.statusCode() / 100) != 2) {
			log.error("Got " + response.statusCode() + " back from signal sent to " + uri);
			log.error("payload: " + payload);
			log.error("Response body: " + response.body());
		}

		return response.body();
	}

	/**
	 * Trigger a workflow.
	 *
	 * @param id  The workflow id
	 * @param key The key to set in the json object
	 * @param obj The object to set as the value in the json object
	 */
	public String triggerWorkflow(final UseService useService, final String id, final Map<String, Object> parameters) {

		JsonObjectBuilder builder = Json.createObjectBuilder();

		for (String key : parameters.keySet()) {
			Object obj = parameters.get(key);
			try {
				builder.add(key, jsonb.fromJson(jsonb.toJson(obj), JsonObject.class));
			} catch (Exception e) {
				// catch standard type objects (String, Integer, etc.)
				if (obj instanceof String) {
					builder.add(key, (String) obj);
				} else if (obj instanceof Integer) {
					builder.add(key, (Integer) obj);
				} else if (obj instanceof Long) {
					builder.add(key, (Long) obj);
				} else if (obj instanceof Double) {
					builder.add(key, (Double) obj);
				} else {
					log.warn("Unknown type available for " + obj);
				}
			}
		}

		return triggerWorkflow(useService, id, builder.build());
	}

	/**
	 * Trigger a workflow.
	 *
	 * @param id  The workflow id
	 * @param key The key to set in the json object
	 * @param obj The object to set as the value in the json object
	 */
	public String triggerWorkflow(final UseService useService, final String id, final String key, final Object obj) {

		JsonObjectBuilder builder = Json.createObjectBuilder();

		try {
			builder.add(key, jsonb.fromJson(jsonb.toJson(obj), JsonObject.class));
		} catch (Exception e) {
			// catch standard type objects (String, Integer, etc.)
			if (obj instanceof String) {
				builder.add(key, (String) obj);
			} else if (obj instanceof Integer) {
				builder.add(key, (Integer) obj);
			} else if (obj instanceof Long) {
				builder.add(key, (Long) obj);
			} else if (obj instanceof Double) {
				builder.add(key, (Double) obj);
			} else {
				log.warn("Unknown type available for " + obj);
			}
		}

		return triggerWorkflow(useService, id, builder.build());
	}

	/**
	 * Trigger a workflow.
	 *
	 * @param id   The workflow id
	 * @param json The json object to send
	 */
	public String triggerWorkflow(final UseService useService, final String id, JsonObject json) {

		// add token to JsonObject
		JsonObjectBuilder builder = Json.createObjectBuilder();
		json.forEach(builder::add);

		if (userToken != null) {
			if (userToken.getToken() != null) {
				builder.add("token", userToken.getToken()); // userToken not always there
			}
			builder.add("userToken", jsonb.toJson(userToken));
		} else {
			log.warn("No userToken available");
		}
		json = builder.build();

		// select uri
		String uri = selectServiceURI(useService) + "/" + id;
		log.info("Triggering workflow with uri: " + uri);

		// make post request
		HttpResponse<String> response = HttpUtils.post(uri, json.toString(), userToken);
		if (response == null) {
			log.error("NULL RESPONSE from workflow endpoint");
			return null;
		}

		// ensure request was a success
		if (Response.Status.Family.familyOf(response.statusCode()) != Response.Status.Family.SUCCESSFUL) {
			log.error("Error, Response Status: " + response.statusCode());
			log.error("Body: " + response.body());
			return null;
		}

		JsonObject result = jsonb.fromJson(response.body(), JsonObject.class);

		// return the processId
		return result.getString("id");
	}

	/**
	 * Helper function for selecting a uri
	 * 
	 * @param useService The Service enum
	 */
	public String selectServiceURI(final UseService useService) {
		return useService.getServiceUrl();
	}

	/**
	 * Process an Answer msg using inference rules.
	 *
	 * @param data The stringified data message
	 * @return A list of answers output from the inference rules
	 */
	public List<Answer> runDataInference(String data) {

		// check if event is a valid event
		QDataAnswerMessage msg = null;
		try {
			msg = jsonb.fromJson(data, QDataAnswerMessage.class);
		} catch (Exception e) {
			log.error("Cannot parse this data!");
			e.printStackTrace();
			return new ArrayList<>();
		}

		if (msg.getItems().length == 0) {
			log.debug("Received empty answer message: " + data);
			return new ArrayList<>();
		}

		// start new session
		KieSession session = kieRuntimeBuilder.newKieSession();
		initSession(session, "Inference");

		// insert answers from message
		int answerCount = msg.getItems().length;

		for (Answer answer : msg.getItems()) {
			log.debug("Inserting answer: " + answer.getAttributeCode() + "=" + answer.getValue() + " into session");
			session.insert(answer);
		}
		log.debug("Inserted " + answerCount + " answers into session");

		// Infer data
		session.fireAllRules();

		// Collect all new answers from the rules
		List<Answer> answers = session.getObjects().stream()
				.filter(o -> (o instanceof Answer))
				.map(o -> (Answer) o)
				.collect(Collectors.toList());

		answerCount = answers.size() - answerCount;

		log.debug("Inferred " + answerCount + " answers");
		session.dispose();
		return answers;
	}

	/**
	 * Funnel a list of answers into ProcessQuestions.
	 * 
	 * @param answers List of answers
	 */
	public void funnelAnswers(List<Answer> answers) {
		// feed all answers from facts into ProcessQuestions
		answers.stream()
				.filter(answer -> answer.getProcessId() != null)
				.filter(answer -> !"no-id".equals(answer.getProcessId()))
				.forEach(answer -> {
					try {
						sendSignal(UseService.GADAQ, "processQuestions", answer.getProcessId(),
								"answer", jsonb.toJson(answer));
					} catch (Exception e) {
						log.error("Cannot send answer!");
						e.printStackTrace();
						return;
					}
				});
		return;
	}

	/**
	 * Get the processId of an outstanding task in ProcessQuestions.
	 * 
	 * @return The processId
	 */
	public String getOutstandingTaskProcessId() throws GraphQLException {

		// TODO: allow this to check for internal gadaq processQuestions too
		// we store the summary code in the persons lifecycle
		JsonArray array = gqlUtils.queryTable("ReceiveQuestionRequest", "sourceCode", userToken.getUserCode(), "id");
		if (array == null || array.isEmpty())
			throw new GraphQLException("No ReceiveQuestionRequest items found");

		// grab ProcessInstances with the parentId equal to this calling id
		String callProcessId = array.getJsonObject(0).getString("id");
		array = gqlUtils.queryTable("ProcessInstances", "parentProcessInstanceId", callProcessId, "id", "variables");
		if (array == null || array.isEmpty())
			throw new GraphQLException("No ProcessInstances items found");

		// iterate processInstance tokens
		for (JsonValue value : array) {

			JsonObject object = value.asJsonObject();
			JsonObject variables = jsonb.fromJson(object.getString("variables"), JsonObject.class);
			String status = variables.getString("status");

			// return first active instance id
			if ("ACTIVE".equals(status))
				return object.getString("id");
		}

		throw new GraphQLException("All intances are complete");
	}

	public String getOutstandingTasks() throws GraphQLException {

		// TODO: allow this to check for internal gadaq processQuestions too
		// we store the summary code in the persons lifecycle
		JsonArray array = gqlUtils.queryTable("ReceiveQuestionRequest", "sourceCode", userToken.getUserCode(), "id");
		if (array == null || array.isEmpty())
			throw new GraphQLException("No ReceiveQuestionRequest items found");

		// grab ProcessInstances with the parentId equal to this calling id
		String callProcessId = array.getJsonObject(0).getString("id");
		array = gqlUtils.queryTable("ProcessInstances", "parentProcessInstanceId", callProcessId, "id", "variables");
		if (array == null || array.isEmpty())
			throw new GraphQLException("No ProcessInstances items found");

		// iterate processInstance tokens
		for (JsonValue value : array) {

			JsonObject object = value.asJsonObject();
			JsonObject variables = jsonb.fromJson(object.getString("variables"), JsonObject.class);
			String status = variables.getString("status");

			// return first active instance id
			if (status.equals("ACTIVE"))
				return object.getString("id");
		}

		throw new GraphQLException("All intances are complete");
	}

	private void initSession(KieSession session, String focus) {
		if (!StringUtils.isBlank(focus))
			session.getAgenda().getAgendaGroup(focus).setFocus();

		// insert utils and other beans
		session.insert(kogitoUtils);
		session.insert(jsonb);
		session.insert(defUtils);
		session.insert(qwandaUtils);
		session.insert(beUtils);
		session.insert(userToken);
		session.insert(baseEntityService);
		session.insert(importGithubService);
		session.insert(cacheManager);
	}

	/**
	 * Initialise data by rule group
	 * 
	 * @param ruleGroupName Group rule name
	 */
	public void initDataByRuleGroup(String ruleGroupName) {
		// start new session
		KieSession session = kieRuntimeBuilder.newKieSession();
		session.getAgenda().getAgendaGroup(ruleGroupName).setFocus();

		// insert utils and other beans
		session.insert(kogitoUtils);
		session.insert(jsonb);
		session.insert(defUtils);
		session.insert(beUtils);
		session.insert(userToken);
		session.insert(baseEntityService);
		session.insert(importGithubService);

		session.fireAllRules();
		session.dispose();
	}

}
