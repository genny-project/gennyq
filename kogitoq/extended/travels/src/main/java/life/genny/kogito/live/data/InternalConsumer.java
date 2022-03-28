package life.genny.kogito.live.data;

import java.time.Duration;
import java.time.Instant;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;
import org.kie.api.runtime.KieSession;
import org.kie.kogito.legacy.rules.KieRuntimeBuilder;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.annotations.Blocking;

import life.genny.kogito.utils.KogitoUtils;
import life.genny.qwandaq.message.QEventMessage;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.serviceq.Service;

@ApplicationScoped
public class InternalConsumer {

    static final Logger log = Logger.getLogger(InternalConsumer.class);

    static Jsonb jsonb = JsonbBuilder.create();

    @Inject
    KogitoUtils kogitoUtils;

    @ConfigProperty(name = "kogito.service.url", defaultValue = "http://alyson.genny.life:8250")
    String myUrl;

    @Inject
    KieRuntimeBuilder kieRuntimeBuilder;

    @Inject
    Service service;

    KieSession ksession;

    /**
     * Execute on start up.
     *
     * @param ev
     */
    void onStart(@Observes StartupEvent ev) {
        service.fullServiceInit();
        log.info("[*] Finished Events Startup!");
    }

    /**
     * Consume from the valid_data topic.
     *
     * @param data
     */
    @Incoming("events")
    @Blocking
    public void getEvent(String data) {

        // log.info("Incoming Event :" + data);
        Instant start = Instant.now();

        KieSession session = kieRuntimeBuilder.newKieSession();

        // session.setGlobal("maxAmount", loanDto.getMaxAmount());

        QEventMessage msg = null;

        try {
            msg = jsonb.fromJson(data, QEventMessage.class);
        } catch (Exception e) {
            log.warn("Cannot parse this data ..");
            return;
        }
        GennyToken userToken = new GennyToken("USERTOKEN", msg.getToken());
        BaseEntityUtils beUtils = new BaseEntityUtils(userToken);
        // log.info("Token username " + userToken.getUsername());

        session.insert(kogitoUtils);
        session.insert(beUtils);
        session.insert(userToken);
        session.insert(msg);
        session.fireAllRules();
        session.dispose();

        Instant end = Instant.now();
        // log.info("Duration = " + Duration.between(start, end).toMillis() + "ms");
    }

    /**
     * Consume from the valid_data topic.
     *
     * @param data
     */
    @Incoming("valid_data")
    @Blocking
    public void getData(String data) {
        // Convert to Json and identify the application
        JsonObject dataJson = jsonb.fromJson(data, JsonObject.class);
        if (dataJson.containsKey("items")) {
            JsonArray itemsArray = dataJson.getJsonArray("items");
            if (!itemsArray.isEmpty()) {

                JsonObject item0 = itemsArray.getJsonObject(0);
                String item0Str = item0.toString();
                // log.info("item0=" + item0Str);
                if ("{}".equals(item0Str)) {
                    log.info("Alyson Heartbeat");
                    return;
                }
            }
        }
        log.info("Incoming Data :");
        Instant start = Instant.now();

        Instant end = Instant.now();
        // log.info("Duration = " + Duration.between(start, end).toMillis() + "ms");
    }

}
