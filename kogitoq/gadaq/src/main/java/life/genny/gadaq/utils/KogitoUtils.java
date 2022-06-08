package life.genny.gadaq.utils;

import java.net.http.HttpResponse;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.kie.kogito.legacy.rules.KieRuntimeBuilder;

import life.genny.qwandaq.message.QEventMessage;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.HttpUtils;

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
    KieRuntimeBuilder kieRuntimeBuilder;

    public String fetchGraphQL(final String graphTable, final String likeField, final String likeValue, String... fields) {

        String data = " query {"
                + "  " + graphTable + " (where: {"
                + "      " + likeField + ": {"
                + " like: \"" + likeValue + "\" }}) {";
        for (String field : fields) {
            data += "   " + field;
        }
        data += "  }"
                + "}";

        String graphQlUrl = System.getenv("GENNY_KOGITO_DATAINDEX_HTTP_URL") + "/graphql";
        HttpResponse<String> response = HttpUtils.post(graphQlUrl, data, "application/GraphQL", userToken.getToken());

        return response.body();
    }

    public String fetchProcessId(final String graphTable, final String likeField, final String likeValue) throws Exception {

        String data = "query { " + graphTable + " (where: { " + likeField + ": { like: \"" + likeValue + "\" }}) { id }}";

        String graphQlUrl = System.getenv("GENNY_KOGITO_DATAINDEX_HTTP_URL") + "/graphql";
        HttpResponse<String> response = HttpUtils.post(graphQlUrl, data, "application/GraphQL", userToken.getToken());

        if (response == null) {
            throw new Exception("No processId found");
		}

		String responseBody = response.body();

		if (responseBody.contains("Error id")) {
			throw new Exception("No processId found");
		}

		// isolate the id
		JsonObject responseJson = jsonb.fromJson(responseBody, JsonObject.class);
		log.info(responseJson);
		JsonObject json = responseJson.getJsonObject("data");
		JsonArray jsonArray = json.getJsonArray(graphTable);

		if (jsonArray == null || jsonArray.isEmpty()) {
			throw new Exception("No processId found");
		}

		JsonObject firstItem = jsonArray.getJsonObject(0);
		return firstItem.getString("id");
    }

    public String fetchProcessId(final String graphTable, final String graphQL) {

        String graphQlUrl = System.getenv("GENNY_KOGITO_DATAINDEX_HTTP_URL") + "/graphql";

        if (userToken == null) {
            log.error("userToken supplied is null");
			return null;
		}

        HttpResponse<String> response = HttpUtils.post(graphQlUrl, graphQL, "application/GraphQL", userToken.getToken());

		if (response == null) {
			log.error("Response was null!");
			return null;
		}

		String responseBody = response.body();
		if (responseBody.contains("Error id")) {
			log.error("Error fetching ProcessId");
			return null;
		}

		// isolate the id
		JsonObject responseJson = jsonb.fromJson(responseBody, JsonObject.class);
		JsonObject json = responseJson.getJsonObject("data");
		JsonArray jsonArray = json.getJsonArray(graphTable);

		if (jsonArray == null || jsonArray.isEmpty()) {
			log.error("No processId found");
		}

		JsonObject firstItem = jsonArray.getJsonObject(0);
		return firstItem.getString("id");
    }

	/**
	 * Send a workflow signal
	 *
	 * @param workflowId The workflow Id
	 * @param processId The process Id
	 * @param signal the signal code
	 */
    public String sendSignal(final String workflowId, final String processId, final String signal) {

        return sendSignal(workflowId, processId, signal, "");
    }

	/**
	 * Send a workflow signal
	 *
	 * @param workflowId The workflow Id
	 * @param processId The process Id
	 * @param signal the signal code
	 * @param payload Th payload to send
	 */
    public String sendSignal(final String workflowId, final String processId, final String signal, final String payload) {

        String uri = GennySettings.kogitoServiceUrl() + "/" + workflowId + "/" + processId + "/" + signal;

		log.info("Sending Signal to uri: " + uri);

        HttpResponse<String> response = HttpUtils.post(uri, payload, "application/json", userToken.getToken());

        return response.body();
    }

	/**
	 * Trigger a workflow.
	 *
	 * @param id The workflow id
	 * @param key The key to set in the json object
	 * @param obj The object to set as the value in the json object
	 */
	public String triggerWorkflow(final String id, final String key, final Object obj) {

		JsonObject json = Json.createObjectBuilder()
				.add(key, jsonb.fromJson(jsonb.toJson(obj), JsonObject.class))
				.build();

		return triggerWorkflow(id, json);
	}

	/**
	 * Trigger a workflow.
	 *
	 * @param id The workflow id
	 * @param json The json object to send
	 */
	public String triggerWorkflow(final String id, JsonObject json) {

		// add token to JsonObject
		JsonObjectBuilder builder = Json.createObjectBuilder();
		json.forEach(builder::add);
		builder.add("token", userToken.getToken());
		json = builder.build();

        String uri = GennySettings.kogitoServiceUrl() + "/" + id;
		log.info("Triggering workflow with uri: " + uri);

        HttpResponse<String> response = HttpUtils.post(uri, json.toString(), userToken.getToken());

        if (response != null && response.statusCode() == 201) {

            JsonObject result = jsonb.fromJson(response.body(), JsonObject.class);

			// return the processId
			return result.getString("id");

        } else {
            log.error("TriggerWorkflow Response Status:  " + (response != null ? response.statusCode() : "NULL RESPONSE"));
        }

        return null;
    }
}
