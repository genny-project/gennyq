package life.genny.gadaq.live.data;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.annotations.Blocking;
import java.time.Duration;
import java.time.Instant;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import life.genny.gadaq.utils.KogitoUtils;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.message.QDataAnswerMessage;
import life.genny.qwandaq.message.QEventMessage;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.serviceq.Service;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;
import org.kie.api.runtime.KieSession;
import org.kie.kogito.legacy.rules.KieRuntimeBuilder;

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
     * Consume from the events topic.
     *
     * @param data
     */
    @Incoming("events")
    @Blocking
    public void getEvent(String data) {

        // log.info("Incoming Event :" + data);
        Instant start = Instant.now();

		// is data a valid event
		QEventMessage msg = null;
        try {
            msg = jsonb.fromJson(data, QEventMessage.class);
        } catch (Exception e) {
            log.warn("Cannot parse this event ..");
            return;
        }

		// check if token is valid
		GennyToken userToken = null;
		try {
			userToken = new GennyToken("USERTOKEN", msg.getToken());
		} catch (Exception e) {
			log.error("Bad Token sent in message!");
			return;
		}

        BaseEntityUtils beUtils = new BaseEntityUtils(service.getServiceToken(), userToken);

		// start new session
        KieSession session = kieRuntimeBuilder.newKieSession();

        session.insert(kogitoUtils);
        session.insert(beUtils);
        session.insert(userToken);
        session.insert(msg);
        session.fireAllRules();
        session.dispose();

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

        Instant start = Instant.now();

		// is data a valid event
		QDataAnswerMessage msg = null;
        try {
            msg = jsonb.fromJson(data, QDataAnswerMessage.class);
        } catch (Exception e) {
            log.warn("Cannot parse this data ..");
            return;
        }

		// check if token is valid
		GennyToken userToken = null;
		try {
			userToken = new GennyToken("USERTOKEN", msg.getToken());
		} catch (Exception e) {
			log.error("Bad Token sent in message!");
			return;
		}

		// check for null or empty answer array
		if (msg.getItems() == null || msg.getItems().length == 0) {
			log.error("null or empty items in answer msg!");
			return;
		}

		// check for event based answers
		for (Answer answer : msg.getItems()) {

			String processId = answer.getProcessId();

			if ("PRI_SUBMIT".equals(answer.getAttributeCode())) {
				kogitoUtils.sendSignal("processquestions", processId, "submit", data, userToken);

			} else if ("PRI_CANCEL".equals(answer.getAttributeCode())) {
				kogitoUtils.sendSignal("processquestions", processId, "cancel", data, userToken);

			} else {
				kogitoUtils.sendSignal("processquestions", processId, "answer", data, userToken);
			}
		}

        Instant end = Instant.now();
        log.info("Duration = " + Duration.between(start, end).toMillis() + "ms");
    }
}
