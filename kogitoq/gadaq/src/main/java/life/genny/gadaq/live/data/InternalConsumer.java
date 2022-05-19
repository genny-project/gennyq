package life.genny.gadaq.live.data;

import java.time.Duration;
import java.time.Instant;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;
import org.kie.api.runtime.KieSession;
import org.kie.kogito.legacy.rules.KieRuntimeBuilder;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.annotations.Blocking;
import life.genny.gadaq.utils.KogitoUtils;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.message.QDataAnswerMessage;
import life.genny.qwandaq.message.QEventMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.serviceq.Service;
import life.genny.serviceq.intf.GennyScopeInit;

@ApplicationScoped
public class InternalConsumer {

	static final Logger log = Logger.getLogger(InternalConsumer.class);

	static Jsonb jsonb = JsonbBuilder.create();

	@ConfigProperty(name = "kogito.service.url", defaultValue = "http://alyson.genny.life:8250")
	String myUrl;

	@Inject
	Service service;

	@Inject
	GennyScopeInit scope;

	@Inject
	UserToken userToken;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	KogitoUtils kogitoUtils;

	@Inject
	KieRuntimeBuilder kieRuntimeBuilder;

	KieSession ksession;

	/**
	 * Execute on start up.
	 *
	 * @param ev The startup event
	 */
	void onStart(@Observes StartupEvent ev) {
		service.fullServiceInit();
	}

	/**
	 * Consume from the events topic.
	 *
	 * @param event The incoming event
	 */
	@Incoming("events")
	@Blocking
	public void getEvent(String event) {

		scope.init(event);

		log.info("Incoming Event : " + event);
		Instant start = Instant.now();

		// check if event is a valid event
		QEventMessage msg = null;
		try {
			msg = jsonb.fromJson(event, QEventMessage.class);
		} catch (Exception e) {
			log.error("Cannot parse this event!");
			return;
		}

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

		scope.destroy();
	}

	/**
	 * Consume from the valid_data topic.
	 *
	 * @param data The incoming data
	 */
	@Incoming("valid_data")
	@Blocking
	public void getData(String data) {

		scope.init(data);

		log.info("Incoming Data : " + data);
		Instant start = Instant.now();

		// check if data is a valid answer msg
		QDataAnswerMessage msg = null;
		try {
			msg = jsonb.fromJson(data, QDataAnswerMessage.class);
		} catch (Exception e) {
			log.warn("Cannot parse this data!");
			return;
		}

		// check for null or empty answer array
		if (msg.getItems() == null || msg.getItems().length == 0) {
			log.error("null or empty items in answer msg!");
			return;
		}

		// check for event based answers
		for (Answer answer : msg.getItems()) {

			// skip if no processId is present
			if (answer.getProcessId() == null) {
				continue;
			}

			String processId = answer.getProcessId();
			String answerJson = jsonb.toJson(answer);

			if ("PRI_SUBMIT".equals(answer.getAttributeCode())) {
				kogitoUtils.sendSignal("processquestions", processId, "submit", answerJson);

			} else if ("PRI_CANCEL".equals(answer.getAttributeCode())) {
				kogitoUtils.sendSignal("processquestions", processId, "cancel", answerJson);

			} else {
				kogitoUtils.sendSignal("processquestions", processId, "answer", answerJson);
			}

		}

		Instant end = Instant.now();
		log.info("Duration = " + Duration.between(start, end).toMillis() + "ms");

		scope.destroy();
	}
}
