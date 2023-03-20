package life.genny.messages.managers;

import life.genny.qwandaq.attribute.EntityAttribute;
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
        log.info(ANSIColour.doColour(">>>>>>>>>>> About to trigger SLACK <<<<<<<<<<<<<<", ANSIColour.GREEN));

        BaseEntity projectBe = (BaseEntity) contextMap.get("PROJECT");
        BaseEntity target = (BaseEntity) contextMap.get("RECIPIENT");

        if (target == null) {
            log.error(ANSIColour.doColour("Target is NULL", ANSIColour.RED));
            return;
        }
        log.info("Target is " + target.getCode());

        if (projectBe == null) {
            log.error(ANSIColour.doColour("ProjectBe is NULL", ANSIColour.RED));
            return;
        }
        log.info("Project is " + projectBe.getCode());

        EntityAttribute targetUrlAttribute = beaUtils.getEntityAttribute(projectBe.getRealm(), projectBe.getCode(), "PRI_URL");
        String targetUrl = targetUrlAttribute != null ? targetUrlAttribute.getValueString() : null;
        if (targetUrl == null) {
            log.error(ANSIColour.doColour("targetUrl is NULL", ANSIColour.RED));
            return;
        }

        String body = null;
        if (contextMap.containsKey("BODY")) {
            body = (String) contextMap.get("BODY");
        } else {
            EntityAttribute bodyAttribute = beaUtils.getEntityAttribute(templateBe.getRealm(), templateBe.getCode(), "PRI_BODY");
            body = bodyAttribute != null ? bodyAttribute.getValueString() : null;
        }
        if (body == null) {
            log.error(ANSIColour.doColour("Body is NULL", ANSIColour.RED));
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
            log.info(ANSIColour.doColour("SLACK response status code = " + response.statusCode(), ANSIColour.GREEN));
        } else {
            log.info(ANSIColour.doColour("SLACK response is NULL", ANSIColour.RED));
        }

    }

}
