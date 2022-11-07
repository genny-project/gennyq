package life.genny.bridge.live.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.vertx.core.Vertx;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;

import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.bridge.BridgeEventType;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CorsHandler;

import io.vertx.ext.web.handler.sockjs.BridgeEvent;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;
import life.genny.qwandaq.utils.CommonUtils;

/**
 * ExternalConsumerConfig --- This class contains configurations for {@link CorsHandler}
 * {@link SockJSHandler} {@link SockJSBridgeOptions} and the populated setting for the 
 * Router. Currently /frontend is hardcoded so external client will need to use this 
 * path also.
 *
 * @author    hello@gada.io
 */
@ApplicationScoped
public class ExternalConsumerConfig {
	private static final Logger log = Logger.getLogger(ExternalConsumerConfig.class.getSimpleName());

	@Inject
	Vertx vertx;
	@Inject
	ExternalConsumer handler;

	@ConfigProperty(name = "environment")
	Optional<String> environment;

	/**
	 * This method is used to set all the types of addresses that will be allowed 
	 * normally external clients like Alyson will use address.inbound to be used to
	 * send or publish messages to this service and handled in {@link ExternalConsumer} 
	 * also UUID like channel addresses is used after extracting the session id of the token 
	 *
	 * @return the list of all inbound permitted options - Allowed addresses in the websocket channel
	 */
	private static List<PermittedOptions> getInbounds(){
		List<PermittedOptions> inbounds = new ArrayList<PermittedOptions>();
		inbounds.add(new PermittedOptions().setAddress("address.inbound"));
		// This regex should be a uuid like 
		inbounds.add(new PermittedOptions().setAddressRegex(".*")); 
		return inbounds;
	}
	
	/**
	 * This method is used to set all the types of addresses that will be allowed 
	 * to send or publish to an external client which has registered the listener with addresses 
	 * specified in this method. Normally the messages are sent to the external client from the 
	 * {@link InternalConsumer} which are data that have been received from the other backends and 
	 * ready to send to the clients who requested the data
	 *
	 * @return the list of all outbound permitted options - Allowed addresses in the websocket channel
	 */
	private static List<PermittedOptions> getOutbounds(){
		List<PermittedOptions> inbounds = new ArrayList<PermittedOptions>();
		inbounds.add(new PermittedOptions().setAddressRegex("address.outbound"));
		// Allowed anything but address.inbound
		inbounds.add(new PermittedOptions().setAddressRegex("^(?!(address\\.inbound)$).*")); 
		return inbounds;
	}

	/**
	 * Use of SockJSBridgeOptions such as timeouts for ping, replys etc. Also the registered addresses
	 * for inbound and outboud are obtained and set in the add method of options.
	 *
	 * @return options SockJSBridgeOptions which used and needed in the SockJSHandler
	 */
	protected static SockJSBridgeOptions setBridgeOptions(){
		SockJSBridgeOptions options = new SockJSBridgeOptions();
		options.setMaxHandlersPerSocket(10);
		options.setPingTimeout(120000); // 2 minutes
		options.setReplyTimeout(60000);
		getInbounds().stream().forEach(options::addInboundPermitted);
		getOutbounds().stream().forEach(options::addOutboundPermitted);
		return options;
	}

    public static CorsHandler cors() {
        String allowedUrl = """
                http://localhost:\\d\\d|
                https://localhost:\\d\\d|
                http://localhost:\\d\\d\\d\\d|
                https://localhost:\\d\\d\\d\\d|
                http://.*.genny.life|http://.*.gada.io|
                https://.*.genny.life|https://.*.gada.io|
                """ + System.getenv("CORS_URLS");
        log.info("allowed url: " + allowedUrl);
        return CorsHandler.create(allowedUrl).allowCredentials(true)
                .allowedMethod(HttpMethod.GET)
                .allowedMethod(HttpMethod.POST)
                .allowedMethod(HttpMethod.PUT)
                .allowedMethod(HttpMethod.OPTIONS)
                .allowedHeader("X-PINGARUNER")
                .allowedHeader("Content-Type")
                .allowedHeader("Authorization")
                .allowedHeader("Accept")
                .allowedHeader("X-Requested-With");
    }

	/**
	 * This method receives  the event with the CDI of tye Router which is used to set all the configs
	 * required to initialized the route /frontend/* so external client can make a get request and then 
	 * upgrade to websockets when a 101 response has been received. After the websockets protocol has 
	 * been successful then the method handleConnectionTypes from {@link ExternalConsumer } will be used
	 * to handle all the messages sent from the external client
	 *
	 * @param router {@link Router } Vertx router to set the routes
	 */
	public void init(@Observes Router router) {
		SockJSHandlerOptions sockOptions = new SockJSHandlerOptions()
			.setHeartbeatInterval(2000);

		SockJSHandler sockJSHandler = SockJSHandler.create(vertx, sockOptions);

		sockJSHandler.bridge(setBridgeOptions(),handler::handleConnectionTypes);

		SockJSBridgeOptions options = new SockJSBridgeOptions();

		Handler<BridgeEvent> handler = new Handler<>() {
			public void handle(BridgeEvent be) {
				boolean isPublish = be.type() == BridgeEventType.PUBLISH;
				boolean isSend = be.type() == BridgeEventType.SEND;
				System.out.println("Received BridgeEvent: " + be.getRawMessage().toString());
				if (isPublish || isSend) {
					// Add Access-Control-Allow-Origin
					JsonObject rawMessage = be.getRawMessage();

					JsonObject headers = rawMessage.getJsonObject("headers");

					String referer = headers.getString("referer");

					headers.put("Access-Control-Allow-Origin", referer);

					rawMessage.put("headers", headers);
					be.setRawMessage(rawMessage);
				}
				be.complete(true);
			}
		};

		environment.filter(d -> !"prod".equals(d))
		.ifPresent(d -> {
				router.route() // do we want to be routing to d here?
				.subRouter(sockJSHandler.bridge(options, handler))
				.handler(cors());
		});

		router.route("/frontend/*")
		.subRouter(sockJSHandler.bridge(options, handler));

		router.route("/frontend/*")
		.handler(cors())
		.handler(sockJSHandler);
		// .subRouter(sockJSHandler.bridge(options, handler))

	}
}
