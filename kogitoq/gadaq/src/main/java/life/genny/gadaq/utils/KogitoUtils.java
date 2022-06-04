package life.genny.gadaq.utils;

import java.io.Serializable;
import java.net.http.HttpResponse;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
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

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(KogitoUtils.class);
    private static Jsonb jsonb = JsonbBuilder.create();

    @ConfigProperty(name = "kogito.service.url", defaultValue = "http://alyson.genny.life:8250")
    String kogitoServiceUrl;

    @Inject
    UserToken userToken;

    @Inject
    KieRuntimeBuilder kieRuntimeBuilder;

    public String fetchGraphQL(final String graphTable, final String likeField, final String likeValue,
            String... fields) {

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

    public String fetchProcessId(final String graphTable, final String likeField, final String likeValue)
            throws Exception {

        String data = " query {"
                + "  " + graphTable + " (where: {"
                + "      " + likeField + ": {"
                + " like: \"" + likeValue + "\" }}) {";
        data += "   id";
        data += "  }"
                + "}";

        String graphQlUrl = System.getenv("GENNY_KOGITO_DATAINDEX_HTTP_URL") + "/graphql";
        HttpResponse<String> response = HttpUtils.post(graphQlUrl, data, "application/GraphQL", userToken.getToken());

        if (response != null) {

            String responseBody = response.body();

            if (!responseBody.contains("Error id")) {

                // isolate the id
                JsonObject responseJson = jsonb.fromJson(responseBody, JsonObject.class);
                log.info(responseJson);
                JsonObject json = responseJson.getJsonObject("data");
                JsonArray jsonArray = json.getJsonArray(graphTable);

                if (jsonArray != null && (!jsonArray.isEmpty())) {

                    JsonObject firstItem = jsonArray.getJsonObject(0);
                    return firstItem.getString("id");

                } else {
                    throw new Exception("No processId found");
                }
            } else {
                throw new Exception("No processId found");
            }
        } else {
            throw new Exception("No processId found");
        }
    }

    public String fetchProcessId(final String graphTable, final String graphQL) {

        String graphQlUrl = System.getenv("GENNY_KOGITO_DATAINDEX_HTTP_URL") + "/graphql";

        if (userToken == null) {
            log.error("userToken supplied is null");
            return null;
        }

        HttpResponse<String> response = HttpUtils.post(graphQlUrl, graphQL, "application/GraphQL",
                userToken.getToken());

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

    public String sendSignal(final String graphTable, final String processId, final String signalCode) {

        return sendSignal(graphTable, processId, signalCode, "");
    }

    public String sendSignal(final String workflow, final String processId, final String signalCode,
            final String entity) {

        String kogitoUrl = GennySettings.kogitoServiceUrl() + "/" + workflow.toLowerCase() + "/" + processId + "/"
                + signalCode;

        log.info("Sending Signal to uri: " + kogitoUrl);

        HttpResponse<String> response = HttpUtils.post(kogitoUrl, entity, "application/json", userToken.getToken());

        return response.body();
    }

    public String triggerWorkflow(final String process, final QEventMessage message) {

        String url = kogitoServiceUrl + "/" + process.toLowerCase();
        String jsonStr = jsonb.toJson(message);

        log.info("TOKEN = " + userToken.getToken());

        String workflowJsonStr = "{\"eventMessage\":" + jsonStr + ", \"token\":\"" + userToken.getToken() + "\"}";

        HttpResponse<String> response = HttpUtils.post(url, workflowJsonStr, userToken.getToken());

        if (response != null && response.statusCode() == 201) {

            JsonObject json = jsonb.fromJson(response.body(), JsonObject.class);

            // return the processId
            return json.getString("id");

        } else {
            log.error("TriggerWorkflow Response Status:  "
                    + (response != null ? response.statusCode() : "NULL RESPONSE"));
        }

        return null;
    }
}
