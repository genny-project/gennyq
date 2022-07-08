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
import life.genny.qwandaq.utils.DefUtils;
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
	UserToken userToken;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	KogitoUtils kogitoUtils;

	@Inject
	DefUtils defUtils;

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
	 * Consume incoming answers for inference
	 */
	@Incoming("valid_data")
	@Blocking
	public void getData(String data) {

		Instant start = Instant.now();
		scope.init(data);

		// check if event is a valid event
		QDataAnswerMessage msg = null;
		try {
			msg = jsonb.fromJson(data, QDataAnswerMessage.class);
		} catch (Exception e) {
			log.error("Cannot parse this data!");
			e.printStackTrace();
			scope.destroy();
			return;
		}

		if (msg.getItems().length == 0) {
			log.debug("Received empty answer message: " + data);
			scope.destroy();
			return;
		}

		log.info("Received Data : " + data);
		log.info("UserToken : " + userToken);

		// start new session
		KieSession session = kieRuntimeBuilder.newKieSession();
		session.getAgenda().getAgendaGroup("Inference").setFocus();

		session.insert(kogitoUtils);
		session.insert(defUtils);
		session.insert(beUtils);
		session.insert(userToken);
		insertAnswersFromMessage(session, msg);

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
			try  {
				kogitoUtils.sendSignal("processQuestions", processId, "answer", answerJson);
			} catch (Exception e) {
				log.error("Cannot send event!");
				e.printStackTrace();
				scope.destroy();
				return;
			}
			
		}

		Instant end = Instant.now();
		log.info("Duration = " + Duration.between(start, end).toMillis() + "ms");
		scope.destroy();
	}

	/**
	 * Insert answers int the kie session from an answer message.
	 */
	private int insertAnswersFromMessage(KieSession session, QDataAnswerMessage message) {
		int counter = 0; // deliberately counter instead of array.length
		for(Answer answer : message.getItems()) {
			log.debug("Inserting answer: " + answer.getAttributeCode() + "=" + answer.getValue() + " into session");
			session.insert(answer);
			counter++;
		}

		log.debug("Inserted " + counter + " answers into session");
		return counter;
	}

	/**
	 * Consume from the genny_events topic.
	 *
	 * @param event The incoming event
	 */
	@Incoming("events")
	@Blocking
	public void getEvent(String event) {

		Instant start = Instant.now();
		scope.init(event);

		// check if event is a valid event
		QEventMessage msg = null;
		try {
			msg = jsonb.fromJson(event, QEventMessage.class);
		} catch (Exception e) {
			log.error("Cannot parse this event!");
			e.printStackTrace();
			scope.destroy();
			return;
		}

		// If the event is a Dropdown then leave it for DropKick
		if ("DD".equals(msg.getEvent_type())) {
			scope.destroy();
			return;
		}

		log.info("Received Event : " + msg.getEvent_type()+"-->"+msg.getData().getCode()+", userCode:"+userToken.getUsername());

		// start new session
		KieSession session = kieRuntimeBuilder.newKieSession();
		session.getAgenda().getAgendaGroup("EventRoutes").setFocus();

		session.insert(kogitoUtils);
		session.insert(jsonb);
		session.insert(userToken);
		session.insert(beUtils);
		session.insert(msg);

		try {
			log.info("firing rules");
			session.fireAllRules();
		} finally {
			session.dispose();
		}

		Instant end = Instant.now();
		log.info("Duration = " + Duration.between(start, end).toMillis() + "ms");

		scope.destroy();
	}
}
