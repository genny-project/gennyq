package life.genny.gadaq.live.data;

import java.lang.invoke.MethodHandles;
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
import org.kie.api.runtime.KieRuntimeBuilder;
import org.kie.api.runtime.KieSession;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.annotations.Blocking;
import life.genny.kogito.common.utils.KogitoUtils;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.message.QDataAnswerMessage;
import life.genny.qwandaq.message.QEventMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.serviceq.Service;
import life.genny.serviceq.intf.GennyScopeInit;

@ApplicationScoped
public class InternalConsumer {

	static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	static Jsonb jsonb = JsonbBuilder.create();

	@ConfigProperty(name = "kogito.service.url", defaultValue = "http://alyson.genny.life:8250")
	String myUrl;

	@Inject
	Service service;

	@Inject
	GennyScopeInit scope;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	UserToken userToken;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	KogitoUtils kogitoUtils;

	@Inject
	DatabaseUtils databaseUtils;

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
	 * Fetch target baseentity from cache 'baseentity'
	 * Add/Replace EntityAttribute value from answer
	 * Push back the baseentity into 'baseentity' cache
	 */
	// TODO: Potentially removing this
	@Blocking
	@Incoming("answer")
	public void fromAnswers(String payload) {
		log.info("RECEIVED ANSWER :: " + payload);
	}

	/**
	 * Consume incoming answers for inference
	 */
	@Incoming("valid_data")
	@Blocking
	public void getData(String data) {
		scope.init(data);

		log.info("Received Data : " + data);
		log.info("userToken :: " + userToken);

		Instant start = Instant.now();

		// check if event is a valid event
		QDataAnswerMessage msg = null;
		try {
			msg = jsonb.fromJson(data, QDataAnswerMessage.class);
		} catch (Exception e) {
			log.error("Cannot parse this data!");
			e.printStackTrace();
			return;
		}

		log.info("Getting session");

		// start new session
		KieSession session = kieRuntimeBuilder.newKieSession();
		session.getAgenda().getAgendaGroup("Inference").setFocus();

		session.insert(kogitoUtils);
		session.insert(beUtils);
		session.insert(userToken);
		session.insert(msg);

		// Infer data
		try {
			session.fireAllRules();
		} finally {
			session.dispose();
		}

		// check for event based answers
		for (Answer answer : msg.getItems()) {

			// skip if no processId is present
			if ("no-id".equals(answer.getProcessId())) {
				continue;
			}

			String processId = answer.getProcessId();
			String answerJson = jsonb.toJson(answer);

			kogitoUtils.sendSignal("processQuestions", processId, "answer", answerJson);
		}

		Instant end = Instant.now();
		log.info("Duration = " + Duration.between(start, end).toMillis() + "ms");
		scope.destroy();
	}

	/**
	 * Consume from the genny_events topic.
	 *
	 * @param event The incoming event
	 */
	@Incoming("events")
	@Blocking
	public void getEvent(String event) {

		scope.init(event);

		log.info("Received Event : " + event);
		log.info("userToken :: " + userToken);
		Instant start = Instant.now();

		// check if event is a valid event
		QEventMessage msg = null;
		try {
			msg = jsonb.fromJson(event, QEventMessage.class);
		} catch (Exception e) {
			log.error("Cannot parse this event!");
			e.printStackTrace();
			return;
		}

		// start new session
		KieSession session = kieRuntimeBuilder.newKieSession();
		session.getAgenda().getAgendaGroup("EventRoutes").setFocus();

		session.insert(kogitoUtils);
		session.insert(beUtils);
		session.insert(userToken);
		session.insert(msg);

		try {
			session.fireAllRules();
		} finally {
			session.dispose();
		}

		Instant end = Instant.now();
		log.info("Duration = " + Duration.between(start, end).toMillis() + "ms");

		scope.destroy();
	}
}
