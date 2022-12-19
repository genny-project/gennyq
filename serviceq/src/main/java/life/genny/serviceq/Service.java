package life.genny.serviceq;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.data.GennyCache;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.DefUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.KeycloakUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.serviceq.intf.KafkaBean;
import life.genny.serviceq.live.data.InternalProducer;

@RegisterForReflection
@ApplicationScoped
public class Service {

	static final Logger log = Logger.getLogger(Service.class);

	static Jsonb jsonb = JsonbBuilder.create();

	@ConfigProperty(name = "genny.show.values")
	Boolean showValues;

	@ConfigProperty(name = "genny.keycloak.url")
	String keycloakUrl;

	@ConfigProperty(name = "genny.keycloak.realm")
	String keycloakRealm;

	@ConfigProperty(name = "genny.service.username")
	String serviceUsername;

	@ConfigProperty(name = "genny.service.password")
	String servicePassword;

	@ConfigProperty(name = "genny.client.id")
	String clientId;

	@ConfigProperty(name = "genny.client.secret")
	String secret;

	@Inject
	EntityManager entityManager;

	@Inject
	InternalProducer producer;

	@Inject
	GennyCache cache;

	@Inject
	KafkaBean kafkaBean;

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	DefUtils defUtils;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	UserToken userToken;

	@Inject
	ServiceToken serviceToken;

	private Boolean initialised = false;

	public Service() {
		// activate our request scope
		Arc.container().requestContext().activate();
	}

	/**
	 * Initialize the serviceTok
	 */
	public void initToken() {

		// fetch token and init entity utility
		String token = KeycloakUtils.getToken(keycloakUrl, keycloakRealm, clientId, secret, serviceUsername, servicePassword);

		if (token == null) {
			log.error("Service token is null for realm!: " + keycloakRealm);
		}
		log.debug("ServiceToken: " + token);

		// init the injected serviceToken
		serviceToken.init(token);
		// set gennyToken as serviceToken just for initialisation purposes
		userToken.init(token);

		// add list of allowed products
		String[] products = getProductCodes();
		if (products != null) {
			serviceToken.setAllowedProducts(products);
		} else {
			log.error("Could not resolve allowed products from either PROJECT_REALM or PRODUCT_CODES env. Ensure they are defined!");
		}
	}

	/**
	 * Fetch the Product Codes from the PRODUCT_CODES. If PRODUCT_CODES are unset use PROJECT_REALM
	 * @return
	 */
	public String[] getProductCodes() {

		String projectRealm = CommonUtils.getSystemEnv("PROJECT_REALM", false);
		String allowedProducts = CommonUtils.getSystemEnv("PRODUCT_CODES");

		if (allowedProducts != null) {
			// Ensure we have unique product codes
			return Arrays.stream(allowedProducts.split(":")).distinct().toArray(String[]::new);

		} else if (projectRealm != null) {
			return new String[]{ projectRealm };
		}

		return null;
	}

	/**
	 * Initialize the cache connection
	 */
	public void initCache() {
		CacheUtils.init(cache);
	}

	/**
	 * Initialize the Kafka channels.
	 */
	public void initKafka() {
		KafkaUtils.init(kafkaBean);
	}

	/**
	 * Initialize the Attribute cache for each allowed productCode.
	 */
	public void initAttributes() {

		// null check the allowed codes
		String[] allowedProducts = serviceToken.getAllowedProducts();
		if (allowedProducts == null) {
			log.error("You must set up the PRODUCT_CODES environment variable!");
		}

		for (String productCode : allowedProducts) {
			qwandaUtils.loadAllAttributesIntoCache(productCode);
		}
	}

	/**
	 * log the service confiduration details.
	 */
	public void showConfiguration() {

		if (showValues) {
			log.info("service username  : " + serviceUsername);
			log.info("service password  : " + servicePassword);
			log.info("keycloakUrl       : " + keycloakUrl);
			log.info("keycloak clientId : " + clientId);
			log.info("keycloak secret   : " + secret);
			log.info("keycloak realm    : " + keycloakRealm);
		}
	}

	/**
	 * Perform a full initialization of the service.
	 */
	public void fullServiceInit() {
		fullServiceInit(false);
	}

	/**
	 * Perform a full initialization of the service.
	 */
	public void fullServiceInit(Boolean hasTopology) {

		if (initialised) {
			log.warn("Attempted initialisation again. Are you calling this method in more than one place?");
			return;
		}

		// log our service config
		showConfiguration();

		// init all
		initToken();
		initCache();
		initKafka();
		// attempt to stop topology producers from failing on startup

		initialised = true;

		log.info("[@] Service Initialised!");
	}

	/**
	 * Boolean representing whether Service should print config values.
	 *
	 * @return should show values
	 */
	public Boolean showValues() {
		return showValues;
	}
}
