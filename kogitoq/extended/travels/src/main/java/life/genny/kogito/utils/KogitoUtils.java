package life.genny.kogito.utils;

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
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.utils.HttpUtils;

/*
 * A static utility class used for standard 
 * Kogito interactions
 * 
 * @author Adam Crow
 */
@ApplicationScoped
public class KogitoUtils implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(KogitoUtils.class);
    private static Jsonb jsonb = JsonbBuilder.create();

    @ConfigProperty(name = "kogito.service.url", defaultValue = "http://alyson.genny.life:8250")
    String kogitoServiceUrl;

    @Inject
    KieRuntimeBuilder kieRuntimeBuilder;

    public String fetchGraphQL(final String graphTable, final String likeField, final String likeValue,
            final GennyToken userToken, String... fields) {
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
        log.info("graphQL url=" + graphQlUrl);
        java.net.http.HttpResponse<String> response = HttpUtils.post(graphQlUrl, data, "application/GraphQL",
                userToken.getToken());
        return response.body();
    }

    public String fetchProcessId(final String graphTable, final String likeField, final String likeValue,
            final GennyToken userToken) throws Exception {
        String idStr = null;
        String data = " query {"
                + "  " + graphTable + " (where: {"
                + "      " + likeField + ": {"
                + " like: \"" + likeValue + "\" }}) {";
        data += "   id";
        data += "  }"
                + "}";
        String graphQlUrl = System.getenv("GENNY_KOGITO_DATAINDEX_HTTP_URL") + "/graphql";
        log.info("graphQL url=" + graphQlUrl);
        log.info("queryJson->" + data);

        java.net.http.HttpResponse<String> response = HttpUtils.post(graphQlUrl, data, "application/GraphQL",
                userToken.getToken());
        if (response != null) {

            String responseBody = response.body();
            log.info("responseBody:" + responseBody);
            if (!responseBody.contains("Error id")) {
                // isolate the id
                JsonObject responseJson = jsonb.fromJson(responseBody, JsonObject.class);
                log.info(responseJson);
                JsonObject json = responseJson.getJsonObject("data");
                JsonArray jsonArray = json.getJsonArray(graphTable);
                if (jsonArray != null && (!jsonArray.isEmpty())) {
                    JsonObject firstItem = jsonArray.getJsonObject(0);
                    idStr = firstItem.getString("id");

                } else {
                    throw new Exception("No processId found");
                }
            } else {
                throw new Exception("No processId found");
            }
        } else {
            throw new Exception("No processId found");
        }
        return idStr;

    }

    public String fetchProcessId(final String graphTable, final String graphQL,
            final GennyToken userToken) throws Exception {
        String idStr = null;

        String graphQlUrl = System.getenv("GENNY_KOGITO_DATAINDEX_HTTP_URL") + "/graphql";
        log.info("graphQL url=" + graphQlUrl);
        log.info("queryJson->" + graphQL);

        java.net.http.HttpResponse<String> response = HttpUtils.post(graphQlUrl, graphQL, "application/GraphQL",
                userToken.getToken());
        if (response != null) {

            String responseBody = response.body();
            log.info("responseBody:" + responseBody);
            if (!responseBody.contains("Error id")) {
                // isolate the id
                JsonObject responseJson = jsonb.fromJson(responseBody, JsonObject.class);
                log.info(responseJson);
                JsonObject json = responseJson.getJsonObject("data");
                JsonArray jsonArray = json.getJsonArray(graphTable);
                if (jsonArray != null && (!jsonArray.isEmpty())) {
                    JsonObject firstItem = jsonArray.getJsonObject(0);
                    idStr = firstItem.getString("id");

                } else {
                    throw new Exception("No processId found");
                }
            } else {
                throw new Exception("No processId found");
            }
        } else {
            throw new Exception("No processId found");
        }
        return idStr;

    }

    public String sendSignal(final String graphTable, final String processId, final String signalCode,
            GennyToken userToken) {
        // http://alyson2.genny.life:${port}/travels/${id}/${abortCode}
        String kogitoUrl = System.getenv("GENNY_KOGITO_SERVICE_URL") + "/" + graphTable.toLowerCase() + "/" + processId
                + "/" + signalCode;
        log.info("signal endpoint url=" + kogitoUrl);
        java.net.http.HttpResponse<String> response = HttpUtils.post(kogitoUrl, "", "application/json",
                userToken.getToken());
        String responseBody = response.body();
        return responseBody;
    }

    public String triggerWorkflow(final String graphTable, final QEventMessage message, GennyToken userToken) {
        String processId = "0";
        String url = kogitoServiceUrl + "/" + graphTable.toLowerCase();
        String jsonStr = jsonb.toJson(message);
        // log.info("triggerWorkFLow:json:" + json);
        String workflowJsonStr = "{\"eventMessage\":" + jsonStr + "}";
        java.net.http.HttpResponse<String> response = HttpUtils.post(url, workflowJsonStr, userToken.getToken());
        int responseCode = response.statusCode();
        if (responseCode == 201) {
            JsonObject idJson = jsonb.fromJson(response.body(), JsonObject.class);
            processId = idJson.getString("id");
            log.info("processId = " + processId);
        } else {
            log.error("TriggerWorkflow " + response.statusCode());
        }
        return processId;
    }
}
