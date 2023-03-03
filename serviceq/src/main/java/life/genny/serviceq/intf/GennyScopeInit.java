package life.genny.serviceq.intf;

import io.quarkus.arc.Arc;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;

/**
 * Custom Genny Scope Initializer class for initializing the UserToken after
 * consuming from Kafka.
 **/
@ApplicationScoped
public class GennyScopeInit {

	static final Logger log = Logger.getLogger(GennyScopeInit.class);

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	BaseEntityUtils beUtils;

	/**
	 * Default Constructor.
	 **/
	public GennyScopeInit() {
	}

	/**
	 * Activate the UserToken using the request context
	 * and initialise the UserToken using consumed data.
	 *
	 * @param data The consumed message from kafka
	 **/
	public JsonObject init(String data) {

		// activate request scope and fetch UserToken
		Arc.container().requestContext().activate();

		if (data == null) {
			log.error("Null data received at Scope Init");
			return null;
		}
		try {

			JsonObject json = jsonb.fromJson(data, JsonObject.class);
			String token = json.getString("token");

			// init GennyToken from token string
			userToken.init(token);
			log.debug("Token Initialized: " + userToken);
			return json;

		} catch (Exception e) {
			log.error("Error initializing token from data: " + data);
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Destroy the UserToken using the request context.
	 **/
	public void destroy() {
		Arc.container().requestContext().activate();
	}
}
