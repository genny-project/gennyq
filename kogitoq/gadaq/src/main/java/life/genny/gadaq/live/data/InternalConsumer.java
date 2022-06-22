package life.genny.gadaq.live.data;

import java.time.Duration;
import java.time.Instant;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import org.jboss.logging.Logger;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieRuntimeBuilder;

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

import life.genny.qwandaq.utils.QwandaUtils;

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
	QwandaUtils qwandaUtils;

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

	@Blocking
	@Incoming("process_questions")
	public void fromProcessQuestions(String payload) {

		Instant start = Instant.now();
		scope.init(payload);

		JsonObject json = jsonb.fromJson(payload, JsonObject.class);
		kogitoUtils.triggerWorkflow("processQuestions", json);

		scope.destroy();
		Instant end = Instant.now();
		log.info("Duration = " + Duration.between(start, end).toMillis() + "ms");
	}

	/**
	 * Fetch target baseentity from cache 'baseentity'
	 * Add/Replace EntityAttribute value from answer
	 * Push back the baseentity into 'baseentity' cache
	 */
	@Blocking
	@Incoming("answer")
	public void fromAnswers(String payload) {

		Instant start = Instant.now();
		scope.init(payload);

		Answer answer = jsonb.fromJson(payload, Answer.class);

		String targetCode = answer.getTargetCode();
		String productCode = userToken.getProductCode();
		String attributeCode = answer.getAttributeCode();
		String ansValue = answer.getValue();

		log.info("Processing answer:" + targetCode + ":" + attributeCode + "=" + ansValue);

		if (ansValue != null && ansValue.length() <= 50) {
			log.debug("[!] Received Kafka Answer!");
			log.debug("Target: " + targetCode);
			log.debug("Attribute Code: " + attributeCode);
			log.debug("Value: " + ansValue);
			log.debug("Token Product Code: " + productCode);
			log.debug("================= END ANSWER ==================");
		}

		qwandaUtils.saveAnswer(answer);

		scope.destroy();
		Instant end = Instant.now();
		log.info("Duration = " + Duration.between(start, end).toMillis() + "ms");
	}

	/**
	 * Consume from the forwarded_events topic.
	 *
	 * @param event The incoming event
	 */
	@Incoming("forwarded_events")
	@Blocking
	public void getEvent(String event) {

		scope.init(event);

		log.info("Received Forwarded Event : " + event);
		log.info("userToken = " + userToken);
		log.info("productCode = " + userToken.getProductCode());
		Instant start = Instant.now();

		// check if event is a valid event
		log.info("Parsing msg");
		QEventMessage msg = null;
		try {
			msg = jsonb.fromJson(event, QEventMessage.class);
		} catch (Exception e) {
			log.error("Cannot parse this event!");
			e.printStackTrace();
			return;
		}
		log.info("Getting session");

		// start new session
		KieSession session = kieRuntimeBuilder.newKieSession();

		log.info("Inserting facts");
		session.insert(kogitoUtils);
		session.insert(beUtils);
		session.insert(userToken);
		session.insert(msg);

		log.info("Firing Rules");
		try {
			session.fireAllRules();
		} finally {
			session.dispose();
		}

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
			if (answer.getProcessId() == null || answer.getProcessId().equals("no-id")) {
				continue;
			}

			String processId = answer.getProcessId();
			String answerJson = jsonb.toJson(answer);

			kogitoUtils.sendSignal("processquestions", processId, "answer", answerJson);
		}

		Instant end = Instant.now();
		log.info("Duration = " + Duration.between(start, end).toMillis() + "ms");

		scope.destroy();
	}
}
