package life.genny.gadaq.live.data;



import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.annotations.Blocking;
import life.genny.kogito.common.utils.KogitoUtils;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.message.QDataAnswerMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.serviceq.Service;
import life.genny.serviceq.intf.GennyScopeInit;

@ApplicationScoped
public class InternalConsumer {

	static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	static Jsonb jsonb = JsonbBuilder.create();

	@Inject
	Service service;

	@Inject
	GennyScopeInit scope;

	@Inject
	UserToken userToken;

	@Inject
	KogitoUtils kogitoUtils;

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
	 * @param data The incoming data
	 */
	@Incoming("valid_data")
	@Blocking
	public void getData(String data) {


		// init scope and process msg
		scope.init(data);

		QDataAnswerMessage msg = null;
		try {
			msg = jsonb.fromJson(data, QDataAnswerMessage.class);
		} catch (Exception e) {
			log.error("Cannot parse this data!");
			e.printStackTrace();
		}
		// check if event is a valid event
		int answerCount = msg.getItems().length;

		if (answerCount) {
			log.debug("Received empty answer message: " + data);
		}

		List<Answer> answers = kogitoUtils.runDataInference(data);
		int answerCountDelta = answers.size() - answerCount;
		if (answerCountDelta == 0)
			log.warn("[!] No inferred answers");
		else
		 	kogitoUtils.funnelAnswers(answers);

		// pass it on to the next stage of inference pipeline
		QDataAnswerMessage msg = new QDataAnswerMessage(answers);
		msg.setToken(userToken.getToken());
		KafkaUtils.writeMsg("genny_data", msg);

		scope.destroy();
	}

	/**
	 * Consume from the genny_events topic.
	 * @param event The incoming event
	 */
	@Incoming("events")
	@Blocking
	public void getEvent(String event) {
		
		Instant start = Instant.now();
		JsonObject eventJson = jsonb.fromJson(event, JsonObject.class);
		if (eventJson.containsKey("event_type")) {
			if ("DD".equals(eventJson.getString("event_type"))) {
				return; // Don't process Dropdowns
			}
		}
		log.info("Received Event : " + obfuscate(eventJson));

		// init scope and process msg
		scope.init(event, start);
		kogitoUtils.routeEvent(event);
		scope.destroy();
	}
}
