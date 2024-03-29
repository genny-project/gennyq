package life.genny.kogito.common.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;

import life.genny.kogito.common.models.TaskExchange;
import life.genny.kogito.common.models.UserExchange;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.KeycloakUtils;

/**
 * A Service class used for communication with other kogito services.
 *
 * @author Jasper Robison
 */
@ApplicationScoped
public class Service2Service extends KogitoService {

	@Inject
	KeycloakUtils keycloakUtils;

	@Inject
	Logger log;

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
			String userCode = taskExchange.getSourceCode();
			userToken = new UserToken(keycloakUtils.getImpersonatedToken(userCode, serviceToken));
		}
		log.info(getLogString());
		taskExchange.setToken(userToken.getToken());
		return taskExchange;
	}

	/**
	 * Add a token to a UserExchange message for sending.
	 *
	 * @param userExchange
	 * @return
	 */
	public UserExchange addToken(UserExchange userExchange) {
		userExchange.setToken(userToken.getToken());
		return userExchange;
	}

	/**
	 * Initialise the RequestScope.
	 *
	 * @param taskExchange The TaskExchange object
	 */
	public void initialiseScope(TaskExchange taskExchange) {
		scope.init(jsonb.toJson(taskExchange));
		log.info(getLogString());
	}

	/**
	 * Initialise the RequestScope.
	 *
	 * @param userExchange
	 */
	public void initialiseScope(UserExchange userExchange) {
		scope.init(jsonb.toJson(userExchange));
		log.info(getLogString());
	}

	/**
	 * log token
	 */
	private String getLogString() {
		return String.format("USER [%s] : [%s]", userToken.getUserCode(), userToken.getUsername());
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
