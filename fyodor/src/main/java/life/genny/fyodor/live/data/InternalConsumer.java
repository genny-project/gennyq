package life.genny.fyodor.live.data;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import java.time.Duration;
import java.time.Instant;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.annotations.Blocking;
import life.genny.fyodor.utils.FyodorSearch;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.message.QSearchMessage;
import life.genny.qwandaq.message.QBulkMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.serviceq.Service;
import life.genny.serviceq.intf.GennyScopeInit;

@ApplicationScoped
public class InternalConsumer {

	static final Logger log = Logger.getLogger(InternalConsumer.class);

    static Jsonb jsonb = JsonbBuilder.create();

	@Inject
	GennyScopeInit scope;

	@Inject
	Service service;

	@Inject
	UserToken userToken;

	@Inject
	FyodorSearch search;

    void onStart(@Observes StartupEvent ev) {

		service.showConfiguration();

		service.initToken();
		service.initCache();
		service.initAttributes();
		service.initKafka();
		log.info("[*] Finished Startup!");
    }

	@Incoming("search_events")
	@Blocking
	public void getSearchEvents(String data) {

		scope.init(data);

		log.info("Received incoming Search Event... ");
		log.debug(data);

		Instant start = Instant.now();

		// Deserialize msg
		QSearchMessage msg = jsonb.fromJson(data, QSearchMessage.class);
		SearchEntity searchBE = msg.getSearchEntity();

		if (searchBE == null) {
			log.error("Message did NOT contain a SearchEntity!!!");
			return;
		}

		log.info("Handling search " + searchBE.getCode());

        QBulkMessage bulkMsg = search.processSearchEntity(searchBE);

		// publish results to destination channel
		KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, bulkMsg);
		scope.destroy();

		Instant end = Instant.now();
		log.info("Finished! - Duration: " + Duration.between(start, end).toMillis() + " millSeconds.");
	}

}
