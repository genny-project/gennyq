package life.genny.gadaq.live.data;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import life.genny.gadaq.cache.SearchCaching;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.annotations.Blocking;
import life.genny.gadaq.route.Events;
import life.genny.kogito.common.service.SearchService;
import life.genny.kogito.common.utils.KogitoUtils;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.message.QDataAnswerMessage;
import life.genny.qwandaq.message.QEventMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.SearchUtils;
import life.genny.qwandaq.utils.SecurityUtils;
import life.genny.serviceq.Service;
import life.genny.serviceq.intf.GennyScopeInit;
import life.genny.gadaq.search.FilterGroupService;

@ApplicationScoped
public class InternalConsumer {

	static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	static Jsonb jsonb = JsonbBuilder.create();

	@Inject
	GennyScopeInit scope;
	@Inject
	Service service;
	@Inject
	UserToken userToken;

	@Inject
	KogitoUtils kogitoUtils;
	@Inject
	SearchService search;

	@Inject
	SearchCaching searchCaching;

	@Inject
	SearchUtils searchUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	Events events;

	@Inject
	FilterGroupService filter;

	/**
	 * Execute on start up.
	 *
	 * @param ev The startup event
	 */
	void onStart(@Observes StartupEvent ev) {

		service.fullServiceInit();
		searchCaching.saveToCache();
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
		log.info("Received Data : " + SecurityUtils.obfuscate(data));

		// init scope and process msg
		scope.init(data);
		List<Answer> answers = kogitoUtils.runDataInference(data);
		if (answers.isEmpty())
			log.warn("[!] No answers after inference");

		Optional<Answer> searchText = answers.stream()
				.filter(ans -> ans.getAttributeCode().equals(Attribute.PRI_SEARCH_TEXT))
				.findFirst();

		if (searchText.isPresent()) {
			Answer ans = searchText.get();
			search.sendNameSearch(ans.getTargetCode(), ans.getValue());
		}

		// pass it on to the next stage of inference pipeline
		QDataAnswerMessage msg = new QDataAnswerMessage(answers);
		msg.setToken(userToken.getToken());
		KafkaUtils.writeMsg(KafkaTopic.GENNY_DATA, msg);

		events.route(msg);

		scope.destroy();
		// log duration
		Instant end = Instant.now();
		log.trace("Duration = " + Duration.between(start, end).toMillis() + "ms");
	}

	/**
	 * Consume from the genny_events topic.
	 * 
	 * @param event The incoming event
	 */
	@Incoming("events")
	@Blocking
	public void getEvent(String event) {

		// init scope and process msg
		Instant start = Instant.now();
		scope.init(event);

		// check if event is a valid event
		QEventMessage msg = null;
		try {
			msg = jsonb.fromJson(event, QEventMessage.class);
		} catch (Exception e) {
			log.error("Cannot parse this event! " + event);
			e.printStackTrace();
			return;
		}

		log.info("Received Event : " + SecurityUtils.obfuscate(event));

		// Check if a token is present, if not then log an error and abort
		if (StringUtils.isBlank(msg.getToken())) {
			log.error("No token present, so aborting , for event! " + event);
		} else {
			events.route(msg);
		}

		scope.destroy();
		Instant end = Instant.now();
		log.trace("Duration = " + Duration.between(start, end).toMillis() + "ms");
	}

}
