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

    //    @Inject
    //   Service service;

    KieSession ksession;

    /**
     * Execute on start up.
     *
     * @param ev
     */
    void onStart(@Observes StartupEvent ev) {

        // service.fullServiceInit();
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

        log.info("Incoming Event :" + data);
        Instant start = Instant.now();

        KieSession session = kieRuntimeBuilder.newKieSession();

        // session.setGlobal("maxAmount", loanDto.getMaxAmount());

        QEventMessage msg = jsonb.fromJson(data, QEventMessage.class);
        GennyToken userToken = new GennyToken("USERTOKEN", msg.getToken());
        BaseEntityUtils beUtils = new BaseEntityUtils(userToken);
        log.info("Token username " + userToken.getUsername());

        session.insert(kogitoUtils);
        session.insert(beUtils);
        session.insert(userToken);
        session.insert(msg);
        session.fireAllRules();
        session.dispose();

        // Convert to Json and identify the application
        JsonObject eventJson = jsonb.fromJson(data, JsonObject.class);
        if (eventJson.containsKey("token")) {
            String tokenStr = eventJson.getString("token");
            // log.info("token=" + tokenStr);
            userToken = new GennyToken(tokenStr);
            log.info("Token username " + userToken.getUsername());

            if (eventJson.containsKey("data")) {
                JsonObject dataJson = eventJson.getJsonObject("data");
                if (dataJson.containsKey("code")) {
                    String code = dataJson.getString("code");
                    if ("ACT_PRI_EVENT_APPLY".equals(code)) {

                    } else if ("ACT_PRI_EVENT_VIEW".equals(code)) {
                        // Now signal the process
                        String targetCode = dataJson.getString("targetCode");
                        log.info("Intern VIEW - targetCode:" + targetCode);
                        String internCode = dataJson.getString("targetCode");

                        String test = kogitoUtils.fetchGraphQL("Application", "internCode", internCode,
                                userToken, "id", "internCode");
                        log.info(test);
                        String sourceCode = userToken.getUserCode();
                        if ("PER_086CDF1F-A98F-4E73-9825-0A4CFE2BB943".equals(sourceCode)) {
                            try {
                                String processId = kogitoUtils.fetchProcessId("Application", "internCode",
                                        internCode,
                                        userToken); // fetchProcessId("Application", "internCode",
                                                                                                                                           // internCode,
                                                                                                                                           // gToken.getToken());
                                                                                                                                           // Send signal
                                log.info("ProcessId=" + processId);
                                String result = kogitoUtils.sendSignal("Application", processId, "ARCHIVE",
                                        userToken);
                                log.info(result);
                            } catch (Exception e) {
                                log.info(e.getLocalizedMessage());
                            }
                        }

                    }
                } else {

                }
            }
        }

        Instant end = Instant.now();
        log.info("Duration = " + Duration.between(start, end).toMillis() + "ms");
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
        log.info("Incoming Data :" + data);
        Instant start = Instant.now();

        Instant end = Instant.now();
        log.info("Duration = " + Duration.between(start, end).toMillis() + "ms");
    }

}
