package life.genny.fyodor.live.data;

import java.time.Duration;
import java.time.Instant;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.annotations.Blocking;
import life.genny.fyodor.utils.FyodorUltra;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.exception.runtime.DebugException;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.message.QSearchMessage;
import life.genny.qwandaq.models.Page;
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
	FyodorUltra fyodor;

	@Inject
	CacheManager cm;

	void onStart(@Observes StartupEvent ev) {

		service.showConfiguration();

		service.initToken();
		service.initCache();
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
		SearchEntity searchEntity = msg.getSearchEntity();
		if (searchEntity == null)
			throw new DebugException("Message did NOT contain a SearchEntity!!!");

		log.info("Handling search " + searchEntity.getCode());

		Page page = fyodor.fetch26(searchEntity);

		Attribute totalResults = cm.getAttribute(Attribute.PRI_TOTAL_RESULTS);
		searchEntity.addAttribute(new EntityAttribute(searchEntity, totalResults, 1.0, String.valueOf(page.getTotal())));
		Attribute index = cm.getAttribute(Attribute.PRI_INDEX);
		searchEntity.addAttribute(new EntityAttribute(searchEntity, index, 1.0, String.valueOf(page.getPageNumber())));

		// convert to sendable
		searchEntity = searchEntity.convertToSendable();

		// send search message
		QDataBaseEntityMessage searchMessage = new QDataBaseEntityMessage(searchEntity);
		searchMessage.setToken(userToken.getToken());
		searchMessage.setReplace(true);
		KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, searchMessage);

		// send results message
		QDataBaseEntityMessage entityMsg = new QDataBaseEntityMessage(page.getItems());
		entityMsg.setTotal(page.getTotal());
		entityMsg.setReplace(true);
		entityMsg.setParentCode(searchEntity.getCode());
		entityMsg.setToken(userToken.getToken());
		KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, entityMsg);

		scope.destroy();
		Instant end = Instant.now();
		log.info("Finished! - Duration: " + Duration.between(start, end).toMillis() + " millSeconds.");
	}

}
