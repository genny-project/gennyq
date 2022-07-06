package life.genny.gadaq.live.data;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.annotations.Blocking;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Instant;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import life.genny.kogito.common.utils.KogitoUtils;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.message.QDataAnswerMessage;
import life.genny.qwandaq.message.QEventMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.SearchUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.serviceq.Service;
import life.genny.serviceq.intf.GennyScopeInit;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;
import org.kie.api.runtime.KieRuntimeBuilder;
import org.kie.api.runtime.KieSession;




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
	SearchUtils searchUtils;

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
		Instant start = Instant.now();

		if (data.contains("\"items\":[{}]")) {
			log.info("Empty answer received");
			return;
		}

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
			log.info("No data to process!");
			scope.destroy();
			return;
		}

		Answer ans = msg.getItems()[0];
		if (ans == null) {
			log.info("No answer to process!");
			scope.destroy();
			return;
		}

		log.info("Received Data : " + data+", userToken="+userToken);

		// start new session
		KieSession session = kieRuntimeBuilder.newKieSession();
		session.getAgenda().getAgendaGroup("Inference").setFocus();

		session.insert(kogitoUtils);
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

		scope.init(event);

	
		Instant start = Instant.now();

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
			log.info("Leaving event to DropKick");
			//kogitoUtils.sendSignal("dropkick", "dropkick", "dropkick", event);
			scope.destroy();
			return;
		}

		log.info("Received Event : " + msg.getEvent_type()+" "+msg.getData().getCode()+", userCode:"+userToken.getUsername());

		// start new session
		KieSession session = kieRuntimeBuilder.newKieSession();
		session.getAgenda().getAgendaGroup("EventRoutes").setFocus();

		session.insert(kogitoUtils);
		session.insert(beUtils);
		session.insert(userToken);
		session.insert(searchUtils);
		session.insert(beUtils);
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
