package life.genny.qwandaq.models;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Various Settings to be used in the Genny System
 **/
@RegisterForReflection
public class GennySettings {

	// URLs
	public static final String projectUrl = System.getenv("PROJECT_URL") != null ? System.getenv("PROJECT_URL")
			: "http://alyson7.genny.life";
	public static final String qwandaServiceUrl = System.getenv("GENNY_API_URL") != null
			? System.getenv("GENNY_API_URL")
			: projectUrl + ":8280";
	public static final String bridgeServiceUrl = System.getenv("BRIDGE_SERVICE_API") != null
			? System.getenv("BRIDGE_SERVICE_API")
			: projectUrl + "/api/service/commands";
	public static final String fyodorServiceUrl = System.getenv("FYODOR_SERVICE_API") != null 
			? System.getenv("FYODOR_SERVICE_API") 
			: "http://erstwhile-wolf-genny-fyodor-svc:4242";
	public static final String shleemyServiceUrl = System.getenv("SHLEEMY_SERVICE_API") != null
			? System.getenv("SHLEEMY_SERVICE_API")
			: (projectUrl + ":4242");
	public static final String infinispanHost = System.getenv("INFINISPAN_HOST") != null
			? System.getenv("INFINISPAN_HOST")
			: (projectUrl + ":11222");

	// RULES
	public static final String realmDir = System.getenv("REALM_DIR") != null ? System.getenv("REALM_DIR") : "./realm";
	public static final String rulesDir = System.getenv("RULES_DIR") != null ? System.getenv("RULES_DIR") : "/rules";
	public static final String keycloakUrl = System.getenv("KEYCLOAK_URL") != null ? System.getenv("KEYCLOAK_URL")
			: "http://keycloak.genny.life";

	// UI Defaults
	public static final Integer defaultPageSize = System.getenv("DEFAULT_PAGE_SIZE") == null ? 10
			: (Integer.parseInt(System.getenv("DEFAULT_PAGE_SIZE")));
	public static final Integer defaultDropDownPageSize = System.getenv("DEFAULT_DROPDOWN_PAGE_SIZE") == null ? 25
			: (Integer.parseInt(System.getenv("DEFAULT_DROPDOWN_PAGE_SIZE")));
	public static final Integer defaultBucketSize = System.getenv("DEFAULT_BUCKET_SIZE") == null ? 8
			: (Integer.parseInt(System.getenv("DEFAULT_BUCKET_SIZE")));

	// TWILIO
	public static final String twilioAccountSid = System.getenv("TWILIO_ACCOUNT_SID") != null
			? System.getenv("TWILIO_ACCOUNT_SID")
			: "TWILIO_ACCOUNT_SID";
	public static final String twilioAuthToken = System.getenv("TWILIO_AUTH_TOKEN") != null
			? System.getenv("TWILIO_AUTH_TOKEN")
			: "TWILIO_AUTH_TOKEN";
	public static final String twilioSenderMobile = System.getenv("TWILIO_SENDER_MOBILE") != null
			? System.getenv("TWILIO_SENDER_MOBILE")
			: "TWILIO_SENDER_MOBILE";

	/*
	 * NOTE: The variables above seem to be defaulting all the time.
	 * likely due to an issue with the environment variables not being present at initialisation.
	 * If it cannot be fixed, we may have to opt for doing it as seen below.
	 */

	// URL methods
	
	/** 
	 * @return String
	 */
	public static String projectUrl() {
		return System.getenv("PROJECT_URL") != null ? System.getenv("PROJECT_URL")
			: "http://alyson7.genny.life";
	}

	
	/** 
	 * @return String
	 */
	public static String qwandaServiceUrl() {
		return System.getenv("GENNY_API_URL") != null ? System.getenv("GENNY_API_URL") 
			: (projectUrl() + ":8280");
	}

	
	/** 
	 * @return String
	 */
	public static String fyodorServiceUrl() {
		return System.getenv("FYODOR_SERVICE_API") != null ? System.getenv("FYODOR_SERVICE_API") 
			: (projectUrl() + ":4242");
	}

	
	/** 
	 * @return String
	 */
	public static String shleemyServiceUrl() {
		return System.getenv("SHLEEMY_SERVICE_API") != null ? System.getenv("SHLEEMY_SERVICE_API")
			: (projectUrl() + ":4242");
	}

	
	/** 
	 * @return String
	 */
	public static String infinispanHost() {
		return System.getenv("INFINISPAN_HOST") != null ? System.getenv("INFINISPAN_HOST")
			: (projectUrl() + ":11222");
	}

	
	/** 
	 * @return String
	 */
	public static String keycloakUrl() {
		return System.getenv("KEYCLOAK_URL") != null ? System.getenv("KEYCLOAK_URL")
			: "http://keycloak.genny.life";
	}

}
