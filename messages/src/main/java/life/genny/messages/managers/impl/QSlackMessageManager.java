package life.genny.messages.managers.impl;

import life.genny.messages.live.qualifer.MessageType;
import life.genny.messages.managers.QMessageProvider;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.utils.MergeUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static life.genny.qwandaq.utils.FailureHandler.required;
import static life.genny.qwandaq.message.QBaseMSGMessageType.SLACK;

@ApplicationScoped
@MessageType(type = SLACK)
public final class QSlackMessageManager extends QMessageProvider {

    @Inject
    Logger log;

    @Inject
    MergeUtils mergeUtils;

    @Override
    public void sendMessage(Map<String, Object> contextMap) {
        log.info(ANSIColour.doColour(">>>>>>>>>>> Triggering SLACK <<<<<<<<<<<<<<", ANSIColour.GREEN));

        String messageCode = (String) required(() -> contextMap.get("MESSAGE"));
        log.info(ANSIColour.doColour("messageCode: "+ messageCode, ANSIColour.GREEN));
        String recipientCode = (String) required(() -> contextMap.get("RECIPIENT"));
        log.info(ANSIColour.doColour("recipientCode: "+ recipientCode, ANSIColour.GREEN));
        String project = (String) required(() -> contextMap.get("PROJECT"));
        log.info(ANSIColour.doColour("project: "+ project, ANSIColour.GREEN));
        String realm = userToken.getRealm();
        log.info(ANSIColour.doColour("realm: "+ realm, ANSIColour.GREEN));

        String targetUrl = required(() -> beaUtils.getEntityAttribute(realm, recipientCode, "PRI_URL").getValueString());
        log.info(ANSIColour.doColour("targetUrl: "+ targetUrl, ANSIColour.GREEN));

        String body = null;

        if (contextMap.containsKey("BODY")) {
            body = (String) contextMap.get("BODY");
        } else {
            body = required(() -> beaUtils.getEntityAttribute(realm, messageCode, "PRI_SLACK_BODY").getValueString());
            log.info(ANSIColour.doColour("body: "+ body, ANSIColour.GREEN));
        }

        // Mail Merging Data
        body = mergeUtils.merge(body, contextMap);
        log.info(ANSIColour.doColour("merged body: "+ body, ANSIColour.GREEN));

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(targetUrl)).POST(HttpRequest.BodyPublishers.ofString(body)).build();

        HttpResponse<String> response = null;

        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            log.error(e.getLocalizedMessage());
        }

        if (response != null) {
            log.info(ANSIColour.doColour("SLACK response status code = " + response.statusCode(), ANSIColour.GREEN));
        } else {
            log.info(ANSIColour.doColour("SLACK response is NULL", ANSIColour.RED));
        }

    }

}
