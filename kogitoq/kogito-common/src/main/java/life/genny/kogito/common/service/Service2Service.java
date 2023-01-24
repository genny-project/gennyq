package life.genny.kogito.common.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;

import org.jboss.logging.Logger;
import life.genny.kogito.common.models.TaskExchange;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.KeycloakUtils;
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
	ServiceToken serviceToken;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	GennyScopeInit scope;

	@Inject
	EntityManager entityManager;

	/**
	 * Add a token to a TaskExchange message for sending.
	 *
	 * @param taskExchange The TaskExchange object
	 * @return The updated taskExchange object
	 */
	public TaskExchange addToken(TaskExchange taskExchange) {
		log.debug(taskExchange);
		if (userToken == null) {
			// We need to fetch the latest token for the sourceUser
			log.debug(taskExchange.getSourceCode() + " : No token found, fetching latest token");
			BaseEntity userBE = beUtils.getBaseEntity(taskExchange.getSourceCode());
			BaseEntity project = beUtils.getBaseEntity(Prefix.PRJ.concat("_".concat(userBE.getRealm().toUpperCase())));
			userToken = new UserToken(KeycloakUtils.getImpersonatedToken(userBE, serviceToken, project));
		}
		logToken();
		taskExchange.setToken(userToken.getToken());
		return taskExchange;
	}

	/**
	 * Initialise the RequestScope.
	 *
	 * @param taskExchange The TaskExchange object
	 */
	public void initialiseScope(TaskExchange taskExchange) {
		log.debug(taskExchange);
		scope.init(jsonb.toJson(taskExchange));
		logToken();
	}

	/**
	 * log token
	 */
	public void logToken() {
		log.infof("USER [%s] : [%s]", userToken.getUserCode(), userToken.getUsername());
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

		KafkaUtils.writeMsg(KafkaTopic.GENNY_EVENTS, json.toString());
	}

}
