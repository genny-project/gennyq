package life.genny.gadaq.live.data;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.annotations.Blocking;
import life.genny.kogito.common.utils.KogitoUtils;
import life.genny.qwandaq.Answer;
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
	 */
	@Incoming("valid_data")
	@Blocking
	public void getData(String data) {

		Instant start = Instant.now();
		log.info("Received Data : " + data);

		// init scope and process msg
		scope.init(data);
		List<Answer> answers = kogitoUtils.runDataInference(data);
		if(answers.size() == 0) {
			log.warn("[!] Received no answers!!!");
		}
		else
			kogitoUtils.funnelAnswers(answers);

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
		if ("DD".equals("event_type")) {
			return; // Don't process Dropdowns
		}
		JsonObject nonTokenJson = eventJson;
		if (nonTokenJson.containsKey("token")) {
				 nonTokenJson = javax.json.Json.createObjectBuilder(nonTokenJson).remove("token").build();
		}
		log.info("Received Event : " +nonTokenJson.toString());

		// init scope and process msg
		scope.init(event);
		kogitoUtils.routeEvent(event);
		scope.destroy();
		// log duration
		Instant end = Instant.now();
		log.info("Duration = " + Duration.between(start, end).toMillis() + "ms");
	}
}
