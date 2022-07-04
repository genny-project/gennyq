package life.genny.kogito.common.utils;

import java.net.http.HttpResponse;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;
// import org.kie.api.runtime.KieRuntimeBuilder;

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

        HttpResponse<String> response = HttpUtils.post(uri, payload, "application/json", userToken);

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

		return triggerWorkflow(id, builder.build());
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
		builder.add("userToken", jsonb.toJson(userToken));
		json = builder.build();

        String uri = GennySettings.kogitoServiceUrl() + "/" + id;
		log.info("Triggering workflow with uri: " + uri);

        HttpResponse<String> response = HttpUtils.post(uri, json.toString(), userToken);

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
