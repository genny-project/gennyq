package life.genny.fyodor.live.data;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.annotations.Blocking;
import life.genny.fyodor.utils.FyodorSearch;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.qwandaq.message.QDataMessage;
import life.genny.qwandaq.message.QSearchBeResult;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;
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
	BaseEntityUtils beUtils;

	@Inject
	QwandaUtils qwandaUtils;

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
		QDataMessage<SearchEntity> msg = jsonb.fromJson(data, QDataMessage.class);
		SearchEntity searchBE = msg.getItems().get(0);

		if (searchBE == null) {
			log.error("Message did NOT contain a SearchEntity!!!");
			return;
		}

		log.info("Handling search " + searchBE.getCode());


		QSearchBeResult results = null;
		Boolean isCountEntity = false;

		// check if it is a count SBE
		if (searchBE.getCode().startsWith("CNS_")) {

			log.info("Found Count Entity " + searchBE.getCode());
			// Remove CNS_ prefix and set count var
			searchBE.setCode(searchBE.getCode().substring(4));
			isCountEntity = true;
		}

		// Check for a specific item search
		for (EntityAttribute attr : searchBE.getBaseEntityAttributes()) {
			if (attr.getAttributeCode().equals("PRI_CODE") && attr.getAttributeName().equals("_EQ_")) {
				log.info("SINGLE BASE ENTITY SEARCH DETECTED");

				BaseEntity be = beUtils.getBaseEntityOrNull(attr.getValue());
				be.setIndex(0);
				BaseEntity[] arr = new BaseEntity[1];
				arr[0] = be;
				results = new QSearchBeResult();
				results.setEntities(Arrays.asList(arr));
				results.setTotal(Long.valueOf(1));
				break;
			}
		}

		// Perform search
		if (results == null) {
			results = search.findBySearch25(searchBE, isCountEntity, true);
		}

		List<EntityAttribute> cals = searchBE.findPrefixEntityAttributes("COL__");
		if (cals != null) {
			log.info("searchUsingSearch25 -> detected " + cals.size() + " CALS");

			for (EntityAttribute calEA : cals) {
				log.info("Found CAL with code: " + calEA.getAttributeCode());
			}
		}

		// Find Allowed Columns
		List<String> allowed = FyodorSearch.getSearchColumnFilterArray(searchBE);
		// Used to disable the column privacy
		EntityAttribute columnWildcard = searchBE.findEntityAttribute("COL_*").orElse(null);

		// Otherwise handle cals
		if (results != null && results.getEntities() != null && !results.getEntities().isEmpty()) {

			for (BaseEntity be : results.getEntities()) {

				if (be != null) {

					// Filter unwanted attributes
					if (columnWildcard == null) {
						be = beUtils.addNonLiteralAttributes(be);
						be = beUtils.privacyFilter(be, allowed);
					}

					for (EntityAttribute calEA : cals) {

						Answer ans = search.getAssociatedColumnValue(be, calEA.getAttributeCode());
						if (ans != null) {
							try {
								be.addAnswer(ans);
							} catch (BadDataException e) {
								log.error(e.getStackTrace());
							}
						}
					}
				}
			}
		}

		// Perform count for any combined search attributes
		Long totalResultCount = 0L;
		for (EntityAttribute ea : searchBE.getBaseEntityAttributes()) {
			if (ea.getAttributeCode().startsWith("CMB_")) {
				String combinedSearchCode = ea.getAttributeCode().substring("CMB_".length());
				SearchEntity combinedSearch = CacheUtils.getObject(userToken.getProductCode(), combinedSearchCode,
						SearchEntity.class);

				Long subTotal = search.performCount(combinedSearch);
				if (subTotal != null) {
					totalResultCount += subTotal;
					results.setTotal(totalResultCount);
				} else {
					log.info("subTotal count for " + combinedSearchCode + " is NULL");
				}
			}
		}

		try {
			Attribute attrTotalResults = qwandaUtils.getAttribute("PRI_TOTAL_RESULTS");
			searchBE.addAnswer(new Answer(searchBE, searchBE, attrTotalResults, results.getTotal() + ""));
		} catch (BadDataException e) {
			log.error(e.getStackTrace());
		}

		log.info("Results = " + results.getTotal().toString());

		QDataMessage<BaseEntity> searchMsg = new QDataMessage<>(searchBE);
		searchMsg.setToken(userToken.getToken());
		searchMsg.setReplace(true);

		// don't add result entities if it is only a count
		QDataMessage<BaseEntity> entityMsg = new QDataMessage<>(results.getEntities());
		entityMsg.setTotal(results.getTotal());
		entityMsg.setReplace(true);
		entityMsg.setParentCode(searchBE.getCode());
		entityMsg.setToken(userToken.getToken());

		Instant end = Instant.now();
		log.info("Finished! - Duration: " + Duration.between(start, end).toMillis() + " millSeconds.");

		// check for null destination
		if (msg.getDestination() == null) {
			log.error("Destination is null! Not Sending results.");
			return;
		}

		// publish results to destination channel
		KafkaUtils.writeMsg(msg.getDestination(), searchMsg);
		KafkaUtils.writeMsg(msg.getDestination(), entityMsg);
		scope.destroy();
	}
}
