package life.genny.serviceq;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.data.GennyCache;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
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

	@ConfigProperty(name = "genny.show.values", defaultValue = "false")
	Boolean showValues;

	@ConfigProperty(name = "genny.keycloak.url", defaultValue = "https://keycloak.gada.io")
	String keycloakUrl;

	@ConfigProperty(name = "genny.keycloak.realm", defaultValue = "genny")
	String keycloakRealm;

	@ConfigProperty(name = "genny.service.username", defaultValue = "service")
	String serviceUsername;

	@ConfigProperty(name = "genny.service.password", defaultValue = "password")
	String servicePassword;

	@ConfigProperty(name = "genny.oidc.client.id", defaultValue = "dummy")
	String clientId;

	@ConfigProperty(name = "genny.oidc.client.secret", defaultValue = "secret")
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

	GennyToken serviceToken;

	BaseEntityUtils beUtils;

	private Boolean initialised = false;

	/**
	 * Get the BaseEntityUtils instance.
	 *
	 * @return The BaseEntityUtils object
	 */
	public BaseEntityUtils getBeUtils() {
		return beUtils;
	}

	/**
	 * Set the BaseEntityUtils instance.
	 *
	 * @param beUtils The BaseEntityUtils object
	 */
	public void setBeUtils(BaseEntityUtils beUtils) {
		this.beUtils = beUtils;
	}

	/**
	 * Get the serviceToken.
	 *
	 * @return The serviceToken
	 */
	public GennyToken getServiceToken() {
		return serviceToken;
	}

	/**
	 * Set the serviceToken.
	 *
	 * @param serviceToken The serviceToken
	 */
	public void setServiceToken(GennyToken serviceToken) {
		this.serviceToken = serviceToken;
	}

	/**
	 * Initialize the serviceToken and BE Utility.
	 */
	public void initToken() {

		// fetch token and init entity utility
		serviceToken = KeycloakUtils.getToken(keycloakUrl, keycloakRealm, clientId, secret, serviceUsername, servicePassword);

		if (serviceToken == null) {
			log.error("Service token is null for realm!: " + keycloakRealm);
		}
		log.info("ServiceToken: " + (serviceToken != null ? serviceToken.getToken() : " null"));

		beUtils = new BaseEntityUtils(serviceToken, serviceToken);
	}

	/**
	 * Initialize the database connection
	 */
	public void initDatabase() {
		databaseUtils.init(entityManager);
	}

	/**
	 * Initialize the cache connection
	 */
	public void initCache() {
		CacheUtils.init(cache);
	}

	/**
	 * Initialize the Kafka channels.
	 *
	 * NOTE: This is probably redundant.
	 * We could move the KafkaBean code into KafkaUtils.
	 */
	public void initKafka() {
		KafkaUtils.init(kafkaBean);
	}

	/**
	 * Initialize the Attribute cache.
	 */
	public void initAttributes() {
		qwandaUtils.init(serviceToken);
	}

	/**
	 * Initialize BaseEntity Definitions.
	 */
	public void initDefinitions() {
		defUtils.init(beUtils);
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

		if (initialised) {
			log.warn("Attempted initialisation again. Are you calling this method in more than one place?");
			return;
		}

		// log our service config
		showConfiguration();

		// init all
		initToken();
		initDatabase();
		initCache();
		initKafka();
		initAttributes();
		initDefinitions();

		initialised = true;
	}

	/**
	 * Boolean representing whether Service should print config values.
	 *
	 * @return should show values
	 */
	public Boolean showValues() {
		return showValues;
	}

	/**
	 * Update the utils gennyToken
	 *
	 * @param gennyToken the gennyToken to update with
	 */
	public void updateGennyToken(GennyToken gennyToken) {
		this.beUtils.updateGennyToken(gennyToken);
	}
}
