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

import static life.genny.messages.util.FailureHandler.required;
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

        String messageCode = (String) contextMap.get("MESSAGE");
        String recipientCode = (String) contextMap.get("RECIPIENT");
        String project = (String) contextMap.get("PROJECT");
        String realm = userToken.getRealm();

        if (messageCode == null) {
            log.error(ANSIColour.doColour("message code is NULL", ANSIColour.RED));
            return;
        }

        if (recipientCode == null) {
            log.error(ANSIColour.doColour("recipient code is NULL", ANSIColour.RED));
            return;
        }

        if (project == null) {
            log.error(ANSIColour.doColour("project code is NULL", ANSIColour.RED));
            return;
        }

        if (realm == null) {
            log.error(ANSIColour.doColour("realm is NULL", ANSIColour.RED));
            return;
        }

        String targetUrl = required(() ->beaUtils.getEntityAttribute(realm, recipientCode, "PRI_URL").getValueString());

        String body = null;

        if (contextMap.containsKey("BODY")) {
            body = (String) contextMap.get("BODY");
        } else {
            body = required(() -> beaUtils.getEntityAttribute(realm, messageCode, "PRI_SLACK_BODY").getValueString());
        }

        // Mail Merging Data
        body = mergeUtils.merge(body, contextMap);

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = null;

        try {
            response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
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
