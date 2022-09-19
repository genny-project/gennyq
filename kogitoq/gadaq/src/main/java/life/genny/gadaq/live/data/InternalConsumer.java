package life.genny.gadaq.live.data;

import static life.genny.qwandaq.utils.SecurityUtils.obfuscate;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
import life.genny.kogito.common.service.SearchService;
import life.genny.kogito.common.utils.KogitoUtils;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.kafka.KafkaTopic;
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

	@Inject
	SearchService search;

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
	 * 
	 * @param data The incoming data
	 */
	@Incoming("valid_data")
	@Blocking
	public void getData(String data) {

		Instant start = Instant.now();
		log.info("Received Data : " + obfuscate(data));

		// init scope and process msg
		scope.init(data);
		List<Answer> answers = kogitoUtils.runDataInference(data);
		if (answers.isEmpty())
			log.warn("[!] No answers after inference");
		// else
		// kogitoUtils.funnelAnswers(answers);

		Optional<Answer> searchText = answers.stream()
				.filter(ans -> ans.getAttributeCode().equals("PRI_SEARCH_TEXT"))
				.findFirst();

		if (searchText.isPresent()) {
			Answer ans = searchText.get();
			search.sendNameSearch(ans.getTargetCode(), ans.getValue());
		}

		// pass it on to the next stage of inference pipeline
		QDataAnswerMessage msg = new QDataAnswerMessage(answers);
		msg.setToken(userToken.getToken());
		KafkaUtils.writeMsg(KafkaTopic.GENNY_DATA, msg);

		scope.destroy();
		// log duration
		Instant end = Instant.now();
		log.info("Duration = " + Duration.between(start, end).toMillis() + "ms");
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
		JsonObject eventJson = jsonb.fromJson(event, JsonObject.class);
		if (eventJson.containsKey("event_type")) {
			if ("DD".equals(eventJson.getString("event_type"))) {
				return; // Don't process Dropdowns
			}
		}
		log.info("Received Event : " + obfuscate(eventJson));

		// init scope and process msg
		scope.init(event);
		kogitoUtils.routeEvent(event);
		scope.destroy();
	}
}
