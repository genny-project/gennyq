package life.genny.bridge.live.data;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import life.genny.bridge.model.grpc.Item;
import life.genny.bridge.blacklisting.BlackListInfo;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.security.keycloak.KeycloakTokenPayload;
import life.genny.qwandaq.security.keycloak.TokenVerification;
import life.genny.serviceq.Service;
import life.genny.serviceq.intf.GennyScopeInit;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.UUID;


/**
 * InternalConsumer --- The class where all messages from the backends such as lauchy,
 * wildfly-rulesservice and other internal applications are received to dispatch to the requested
 * client by reading the token and routing by sesssion id.
 *
 * @author hello@gada.io
 */
@ApplicationScoped
public class InternalConsumer {

    private static final Logger log = Logger.getLogger(InternalConsumer.class);

    @Inject
    TokenVerification verification;
    @Inject
    EventBus bus;
    @Inject
    BlackListInfo blackList;
    @Inject
    BridgeGrpcService grpcService;
    @Inject
    GennyScopeInit scope;
    @Inject
    Service service;
    @Inject
    UserToken userToken;

    void onStart(@Observes StartupEvent ev) {

        service.initToken();
        service.initCache();
        service.initKafka();
        log.info("[*] Finished Startup!");
    }


    /**
     * A request with a protocol which will add, delete all or delete just a record depending on the
     * protocol specified in the in the message. The protocol consist of the following: - Just a
     * dash/minus (-) - A dash/minus appended with a {@link UUID} (-UUID.toString()) - A {@link UUID}
     * (UUID.toString())
     *
     * @param protocol A string with the rules already mentioned
     */
    @Incoming("blacklists")
    public void getBlackLists(String protocol) {

        log.warn("New recorded info associated to invalid data this protocol {"
                + protocol + "} will be handled in the blacklisted class");
        blackList.onReceived(protocol);
    }

    @Incoming("webcmds")
    public void getFromWebCmds(String data) {

        scope.init(data);

        log.info("Message received in webcmd");
        handleIncomingMessage(data);

        scope.destroy();
    }

    @Incoming("webdata")
    public void getFromWebData(String data) {

        scope.init(data);

        log.info("Message received in webdata");
        handleIncomingMessage(data);

        scope.destroy();
    }

    /**
     * It checks that no confidential information has been leaked. It will delete the key properties
     * if it finds any
     *
     * @param json A JsonObject
     * @return A JsonObject without the confidential key properties
     */
    public static JsonObject removeKeys(final JsonObject json) {

        if (json.containsKey("token")) {
            json.remove("token");
        }

        if (json.containsKey("recipientCodeArray")) {
            json.remove("recipientCodeArray");
        }

        return json;
    }

    /**
     * Handle the message and route by session id which is extracted from the token
     *
     * @param arg A Json string which is parsed inside the body of the method
     */
    public void handleIncomingMessage(String arg) {

		log.debug("Outgoing Payload = " + arg);

		String incoming = arg;
		if ("{}".equals(incoming)) {
			log.warn("The payload sent from the webcmd producer is empty");
			return;
		}

		final JsonObject json = new JsonObject(incoming);
        final JsonArray items = json.getJsonArray("items");
        if(items != null) {
            if(items.size() == 1) {
                if(StringUtils.isBlank(json.getString("aliasCode"))) {
                    JsonObject firstItem = items.getJsonObject(0);
                    if(firstItem != null) {
                        String aliasCode = firstItem.getString("name");
                        json.put("aliasCode", aliasCode);
                    }
                }
            }
        } else if(items == null || items.size() == 0) {
            log.error("[!] Sending out a message with 0 items! Not forwarding message to Frontend");
            return;
        }
        
		GennyToken gennyToken = new GennyToken(json.getString("token"));
		try {
			verification.verify(gennyToken.getKeycloakRealm(), gennyToken.getToken());
		} catch (Exception e) {
			log.error("The token verification has failed somehow this token was able to penatrate other "
				+ "security barriers please check this exception in more depth");
			e.printStackTrace();
		}
		// KeycloakTokenPayload payload = KeycloakTokenPayload.decodeToken(json.getString("token"));

		if (json.containsKey("data_type")) {
			log.info("QBEM being sent outside:" + json);
		} else {
			/// is this really empty body ?
		}

		if (!incoming.contains("<body>Unauthorized</body>")) {
			String sessionState = (String) gennyToken.getAdecodedTokenMap().get("session_state");
			log.info("Publishing message to session " + sessionState);

			KeycloakTokenPayload payload = KeycloakTokenPayload.decodeToken(json.getString("token"));
			grpcService.send(payload.jti, Item.newBuilder().setBody(removeKeys(json).toString()).build());
			bus.publish(sessionState, removeKeys(json));

		} else {
			log.error("The host service of channel producer tried to accessed an endpoint and got an"
				+ " unauthorised message potentially from api and the producer hosted in rulesservice");
		}
    }

}
