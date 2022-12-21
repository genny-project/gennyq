package life.genny.kogito.common.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;

import org.jboss.logging.Logger;

import life.genny.kogito.common.models.S2SData;
import life.genny.kogito.common.models.S2SData.EAbortReason;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.KeycloakUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.serviceq.intf.GennyScopeInit;

import org.apache.commons.lang3.StringUtils;

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
	 * Add a token to a S2SData message for sending.
	 *
	 * @param data The S2SData object
	 * @return The updated data object
	 */
	public S2SData addToken(S2SData data) {
		log.info("Adding token to data before sending");
		log.info("AbortReason = " + data.getAbortReason().toString());
		data.setAbortReason(EAbortReason.NONE);
		if (userToken == null) {
			// We need to fetch the latest token for the sourceUser
			log.debug(data.getSourceCode() + ": No token found, fetching latest token");
			BaseEntity userBE = beUtils.getBaseEntity(data.getSourceCode());
			BaseEntity project = beUtils.getBaseEntity(userBE.getRealm());
			log.debug("Fetching impersonated token for " + userBE.getCode() + " in " + project.getCode());

			String userTokenStr = KeycloakUtils.getImpersonatedToken(userBE, serviceToken, project);
			userToken = new UserToken(userTokenStr);
			//log.debug("generated userToken " + userToken);
			data.setToken(userToken.getToken());
			log.infof("USER [%s] : [%s]", userToken.getUserCode(), userToken.getUsername());

		} else {
			try {
				String username = userToken.getUsername();
				//log.debug("username is " + username); // flush out timer based npe
			} catch (NullPointerException npe) {
				String userTokenStr = KeycloakUtils.getImpersonatedToken(serviceToken, data.getSourceCode());
				if (StringUtils.isBlank(userTokenStr)) {
					log.error("Could not get impersonated token for " + data.getSourceCode());
				}
				userToken = new UserToken(userTokenStr);
			}
			log.info("Token Added");
			data.setToken(userToken.getToken());
			//log.infof("USER [%s] : [%s]", userToken.getUserCode(), userToken.getUsername());
		}

		// if (data.isAborted()) {
		// 	log.info("Sending aborted message " + data.getAbortReason());
		// 	return data;
		// }
		return data;
	}

	/**
	 * Initialise the RequestScope.
	 *
	 * @param data The S2SData object
	 */
	public void initialiseScope(S2SData data) {
		log.info(data.toString());
		// if (data.isAborted()) {
		// 	log.info("Handle aborted message " + data.getAbortReason());
		// 	userToken = new UserToken(data.getToken());
		// }
		scope.init(jsonb.toJson(data));

		log.infof("USER [%s] : [%s]", userToken.getUserCode(), userToken.getUsername());
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
