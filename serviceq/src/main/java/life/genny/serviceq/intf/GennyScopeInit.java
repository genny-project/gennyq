package life.genny.serviceq.intf;

import io.quarkus.arc.Arc;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;

import org.jboss.logging.Logger;

import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;

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

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	EntityManager entityManager;

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
	public void init(String data) {
		if (beUtils == null) {
			log.error("NULL BE UTILS");
			this.beUtils = new BaseEntityUtils();
		}

		if (databaseUtils == null) {
			log.error("NULL DATABASE UTILS");
			this.databaseUtils = new DatabaseUtils();
			this.databaseUtils.setEntityManager(entityManager);
		}

		// activate request scope and fetch UserToken
		Arc.container().requestContext().activate();

		if (data == null) {
			log.error("Null data received at Scope Init");
			return;
		}
		try {

			JsonObject json = jsonb.fromJson(data, JsonObject.class);
			String token = json.getString("token");

			// init GennyToken from token string
			userToken.init(token);
			log.debug("Token Initialized: " + userToken);

		} catch (Exception e) {
			log.error("Error initializing token from data: " + data);
			e.printStackTrace();
		}
	}

	/**
	 * Destroy the UserToken using the request context.
	 **/
	public void destroy() {
		Arc.container().requestContext().activate();
	}
}
