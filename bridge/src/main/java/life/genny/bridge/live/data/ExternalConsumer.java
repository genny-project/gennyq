package life.genny.bridge.live.data;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.web.handler.sockjs.BridgeEvent;
import java.util.UUID;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import life.genny.bridge.blacklisting.BlackListInfo;
import life.genny.qwandaq.data.BridgeSwitch;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.security.keycloak.RoleBasedPermission;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.HttpUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.serviceq.Service;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * ExternalConsumer --- External clients can connect to the endpoint configured in {@link
 * ExternalConsumerConfig} to upgrade websockets and maintain a real time communication. The only
 * knowned external consumer is alyson but it can be adapted to any other client.
 *
 * @author hello@gada.io
 */
@Singleton
public class ExternalConsumer {

	private static final Logger log = Logger.getLogger(ExternalConsumer.class);

	static Jsonb jsonb = JsonbBuilder.create();

	@Inject RoleBasedPermission permissions;
	@Inject BlackListInfo blacklist;
	@Inject Service service;

	@ConfigProperty(name = "bridge.id", defaultValue = "false")
	String bridgeId;

	void onStart(@Observes StartupEvent ev) {

		// service.initToken();
		service.initCache();
		service.initKafka();
		log.info("[*] Finished Startup!");
	}

	/**
	 * Only handle mesages when they are type SEND or PUBLISH.
	 *
	 * @param bridgeEvent BridgeEvent object pass as an argument. See more {@link
	 *     ExternalConsumerConfig} method - init(@Observes Router router)
	 */
	void handleConnectionTypes(final BridgeEvent bridgeEvent) {
		BridgeEventType type = bridgeEvent.type();
		if (type.equals(BridgeEventType.PUBLISH) || type.equals(BridgeEventType.SEND)) {
			log.info("Handling BridgeEventType: " + type.name());
			handleIfRolesAllowed(bridgeEvent, "user");
		} else {
			log.debug("Nothing to do. Marking the event as complete since the BridgeEventType is: " + type.name());
		}
		bridgeEvent.complete(true);
	}

	/**
	 * Checks if the token has been verified and contains the roles and permission for this request
	 *
	 * @param bridgeEvent BridgeEvent object pass as an argument. See more {@link
	 *     ExternalConsumerConfig} method - init(@Observes Router router)
	 * @param roles An arrays of string with each string being a role such user, test, admin etc.
	 */
	void handleIfRolesAllowed(final BridgeEvent bridgeEvent, String... roles) {

		JsonObject headers = bridgeEvent.getRawMessage().getJsonObject("headers");
		String token = HttpUtils.extractTokenFromHeaders(headers.getString("Authorization"));

		GennyToken gennyToken = new GennyToken(token);

		UUID uuid = UUID.fromString((String) gennyToken.getAdecodedTokenMap().get("sub"));

		if (blacklist.getBlackListedUUIDs().contains(uuid)) {
			bridgeEvent.socket().close(-1, "Here is the confs ::: KLJHsSF#22452345SD09Jjla");
			log.error("A blacklisted user " + uuid + " tried to access the sockets from remote "
					+ bridgeEvent.socket().remoteAddress());
			return;
		}

		if (permissions.rolesAllowed(gennyToken, roles)) {
			bridgeHandler(bridgeEvent, gennyToken);
		} else {
			log.error("A message was sent with a bad token or an unauthorized user or a token from "
					+ "a different authority this user has not access to this request "
					+ uuid);
			bridgeEvent.complete(false);
		}
	}

	/**
	 * Handle message after token has been verified
	 *
	 * @param bridgeEvent BridgeEvent object pass as an argument. See more {@link
	 *     ExternalConsumerConfig} method - init(@Observes Router router)
	 * @param gennyToken the users GennyToken
	 */
	protected void bridgeHandler(final BridgeEvent bridgeEvent, GennyToken gennyToken) {

		if (bridgeEvent.getRawMessage() == null) {
			log.warn("No raw message!");
			return;
		}

		JsonObject rawMessageBody = bridgeEvent.getRawMessage().getJsonObject("body");

		if (!validateMessage(bridgeEvent)) {
			log.error("An invalid message has been received from this user "
					+ gennyToken.getUserCode() + " this message will be ingored ");
			return;
		}

		// put bridgeId into users cached info
		BridgeSwitch.put(gennyToken, bridgeId);
		BridgeSwitch.addActiveBridgeId(gennyToken, bridgeId);

		if (gennyToken.hasRole("test")) {
			log.info("Saving token -> Key: TOKEN:" + gennyToken.getUserCode() + ", Value: " + gennyToken.getToken());
			CacheUtils.writeCache(gennyToken.getProductCode(), "TOKEN:"+gennyToken.getUserCode(), gennyToken.getToken());
		}

		routeDataByMessageType(rawMessageBody.getJsonObject("data"), gennyToken);
		bridgeEvent.complete(true);
	}

	/**
	 * Checks whether the messsage contains within body and data key property. In addition a limit of
	 * 100kb is set so if the message is greater than tha the socket will be closed
	 *
	 * @param bridgeEvent BridgeEvent object pass as an argument. See more {@link
	 *     ExternalConsumerConfig} method - init(@Observes Router router)
	 * @return - True if contains data key field and json message is less than 100kb - False otherwise
	 */
	Boolean validateMessage(BridgeEvent bridgeEvent) {

		JsonObject rawMessage = bridgeEvent.getRawMessage().getJsonObject("body");

		if (rawMessage.toBuffer().length() > 100000) {
			log.error("message of size "
					+ rawMessage.toBuffer().length()
					+ " is larger than 100kb sent from "
					+ bridgeEvent.socket().remoteAddress()
					+ " coming from the domain "
					+ bridgeEvent.socket().uri());
			bridgeEvent.socket().close(-1, "message message is larger than 100kb");
			return false;
		}

		try {
			return rawMessage.containsKey("data");
		} catch (Exception e) {
			log.error("message does not have data field inside body");
			bridgeEvent.complete(true);
			return false;
		}
	}

	/**
	 * Depending of the message type the corresponding internal producer channel is used to route that
	 * request on the backends such as rules, api, sheelemy notes, messages etc.
	 *
	 * @param body The body extracted from the raw json object sent from BridgeEvent
	 * @param gennyToken the users GennyToken
	 */
	void routeDataByMessageType(JsonObject body, GennyToken gennyToken) {

		log.info("Incoming Payload = " + body.toString());

		if (body == null || body.getString("msg_type") == null) {
			log.error("Bad body JsonObject passed");
			return;
		}

		String msgType = body.getString("msg_type");
		String productCodes = CommonUtils.getSystemEnv("PRODUCT_CODES");
		String topicName = "";

		if (msgType.equals("DATA_MSG")) {
			if (!StringUtils.isEmpty(productCodes)) {
				topicName = "data";
			} else {
				topicName = "genny_data";
			}
		} else if (msgType.equals("EVT_MSG")) {
			if (!StringUtils.isEmpty(productCodes)) {
				topicName = "events";
			} else {
				topicName = "genny_events";
				topicName = "events";
			}
		}

		// publish message
		String payload = body.toString();
		KafkaUtils.writeMsg(topicName, payload);

		// remove token from log for security purposes
		body.remove("token");
		payload = body.toString();
		log.info("Sent payload "+payload+" from user " + gennyToken.getUserCode() + " to topic "+topicName);
	}

}
