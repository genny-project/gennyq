package life.genny.kogito.common.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.kogito.common.models.KogitoData;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.serviceq.intf.GennyScopeInit;

/**
 * A Service class used for communication with other kogito services.
 *
 * @author Jasper Robison
 */
@ApplicationScoped
public class Service2Service {

	private static final Logger log = Logger.getLogger(Service2Service.class);

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	GennyScopeInit scope;

	/**
	 * Add a token to a KogitoData message for sending.
	 *
	 * @param data The KogitoData object
	 * @return The  updated data object
	 */
	public KogitoData addToken(KogitoData data) {

		data.setToken(userToken.getToken());
		return data;
	}

	/**
	 * Initialise the RequestScope.
	 *
	 * @param data The KogitoData object
	 */
	public void initialiseScope(KogitoData data) {
		log.info(data.toString());
		scope.init(jsonb.toJson(data));
	}

	/**
	 * Send a message to signify that an item's creation is complete
	 */
	public void sendItemComplete(String definitionCode, String entityCode) {

		JsonObject json = Json.createObjectBuilder()
			.add("event_type", "LIFECYCLE")
			.add("msg_type", "EVT_MSG")
			.add("token", userToken.getToken())
			.add("data", Json.createObjectBuilder()
				.add("code", "ITEM_COMPLETE")
				.add("parentCode", definitionCode)
				.add("targetCode", entityCode))
			.build();

		KafkaUtils.writeMsg("genny_events", json.toString());
	}

	/**
	 * Send a message to perform an update of a persons summary
	 */
	public void updateSummary(String personCode, String summaryCode) {

		JsonObject json = Json.createObjectBuilder()
			.add("event_type", "LIFECYCLE")
			.add("msg_type", "EVT_MSG")
			.add("token", userToken.getToken())
			.add("data", Json.createObjectBuilder()
				.add("code", "UPDATE_SUMMARY")
				.add("parentCode", summaryCode)
				.add("targetCode", personCode))
			.build();

		KafkaUtils.writeMsg("genny_events", json.toString());
	}
}
