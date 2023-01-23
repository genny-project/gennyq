package life.genny.messages.managers;

import life.genny.qwandaq.entity.BaseEntity;
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

@ApplicationScoped
public final class QSlackMessageManager extends QMessageProvider {

    private static final Logger log = Logger.getLogger(QSlackMessageManager.class);

    @Inject
    MergeUtils mergeUtils;

    @Override
    public void sendMessage(BaseEntity templateBe, Map<String, Object> contextMap) {
        log.info(ANSIColour.GREEN + ">>>>>>>>>>> About to trigger SLACK <<<<<<<<<<<<<<" + ANSIColour.RESET);

        BaseEntity projectBe = (BaseEntity) contextMap.get("PROJECT");
        BaseEntity target = (BaseEntity) contextMap.get("RECIPIENT");

        if (target == null) {
            log.error(ANSIColour.RED + "Target is NULL" + ANSIColour.RESET);
            return;
        }
        log.info("Target is " + target.getCode());

        if (projectBe == null) {
            log.error(ANSIColour.RED + "ProjectBe is NULL" + ANSIColour.RESET);
            return;
        }
        log.info("Project is " + projectBe.getCode());

        String targetUrl = target.getValue("PRI_URL", null);
        if (targetUrl == null) {
            log.error(ANSIColour.RED + "targetUrl is NULL" + ANSIColour.RESET);
            return;
        }

        String body = null;
        if (contextMap.containsKey("BODY")) {
            body = (String) contextMap.get("BODY");
        } else {
            body = templateBe.getValue("PRI_BODY", null);
        }
        if (body == null) {
            log.error(ANSIColour.RED + "Body is NULL" + ANSIColour.RESET);
            return;
        }
        log.info("Body is " + body);

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
            log.info(ANSIColour.GREEN + "SLACK response status code = " + response.statusCode() + ANSIColour.RESET);
        } else {
            log.info(ANSIColour.RED + "SLACK response is NULL" + ANSIColour.RESET);
        }

    }

}
