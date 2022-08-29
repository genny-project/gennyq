package life.genny.kogito.common.service;

import io.quarkus.arc.Arc;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.kogito.common.models.S2SData;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.KeycloakUtils;

/**
 * Custom Kogito Scope Initializer class for initializing the UserToken when
 * context is lost in workflow
 **/
@ApplicationScoped
public class KogitoScopeInit {

	static final Logger log = Logger.getLogger(KogitoScopeInit.class);

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	ServiceToken serviceToken;

	@Inject
	BaseEntityUtils beUtils;

	/**
	 * Default Constructor.
	 **/
	public KogitoScopeInit() {
	}

	/**
	 * Activate the UserToken using the request context
	 * and initialise the UserToken using consumed data.
	 *
	 * @param data The consumed message from kafka
	 **/
	public void init(S2SData data) {
		log.info("KogitoScopeInit init! " + data);
		if (data == null) {
			log.error("Null data received at Kogito Scope Init");
			return;
		}
		if (beUtils == null) {
			log.error("NULL BE UTILS");
			this.beUtils = new BaseEntityUtils(serviceToken);
		}
		Arc.container().requestContext().activate();
		this.beUtils = new BaseEntityUtils(serviceToken);
		// We need to fetch the latest token for the sourceUser
		log.info("sourceCode=" + data.getSourceCode());
		log.info("serviceToken=" + serviceToken);

		String userTokenStr = KeycloakUtils.getImpersonatedToken(serviceToken, data.getSourceCode());
		if (StringUtils.isBlank(userTokenStr)) {
			log.error("Could not get impersonated token for " + data.getSourceCode());
		}
		userToken = new UserToken(userTokenStr);
		log.info("generated userToken " + userToken);
		data.setToken(userToken.getToken());
		log.infof("USER [%s] : [%s]", userToken.getUserCode(), userToken.getUsername());

		// activate request scope and fetch UserToken

		try {

			String token = data.getToken();

			// init GennyToken from token string
			userToken = new UserToken(token);
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
