package life.genny.qwandaq.models;

import life.genny.qwandaq.utils.CommonUtils;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Various Settings to be used in the Genny System
 **/
@RegisterForReflection
public class GennySettings {

	// URLs
	// public static final String projectUrl = CommonUtils.getSystemEnv("PROJECT_URL") != null ? CommonUtils.getSystemEnv("PROJECT_URL")
	// 		: "http://alyson7.genny.life";
	// public static final String qwandaServiceUrl = CommonUtils.getSystemEnv("GENNY_API_URL") != null
	// 		? CommonUtils.getSystemEnv("GENNY_API_URL")
	// 		: projectUrl + ":8280";
	// public static final String bridgeServiceUrl = CommonUtils.getSystemEnv("BRIDGE_SERVICE_API") != null
	// 		? CommonUtils.getSystemEnv("BRIDGE_SERVICE_API")
	// 		: projectUrl + "/api/service/commands";
	// public static final String fyodorServiceUrl = CommonUtils.getSystemEnv("FYODOR_SERVICE_API") != null 
	// 		? CommonUtils.getSystemEnv("FYODOR_SERVICE_API") 
	// 		: "http://erstwhile-wolf-genny-fyodor-svc:4242";
	// public static final String shleemyServiceUrl = CommonUtils.getSystemEnv("SHLEEMY_SERVICE_API") != null
	// 		? CommonUtils.getSystemEnv("SHLEEMY_SERVICE_API")
	// 		: (projectUrl + ":4242");
	// public static final String infinispanHost = CommonUtils.getSystemEnv("INFINISPAN_URL") != null
	// 		? CommonUtils.getSystemEnv("INFINISPAN_URL")
	// 		: (projectUrl + ":11222");

	// RULES
	// public static final String realmDir = CommonUtils.getSystemEnv("REALM_DIR") != null ? CommonUtils.getSystemEnv("REALM_DIR") : "./realm";
	// public static final String rulesDir = CommonUtils.getSystemEnv("RULES_DIR") != null ? CommonUtils.getSystemEnv("RULES_DIR") : "/rules";
	// public static final String keycloakUrl = CommonUtils.getSystemEnv("GENNY_KEYCLOAK_URL") != null ? CommonUtils.getSystemEnv("GENNY_KEYCLOAK_URL")
	// 		: "http://keycloak.genny.life";

	// UI Defaults
	// public static final Integer defaultPageSize = CommonUtils.getSystemEnv("DEFAULT_PAGE_SIZE") == null ? 10
	// 		: (Integer.parseInt(CommonUtils.getSystemEnv("DEFAULT_PAGE_SIZE")));
	// public static final Integer defaultDropDownPageSize = CommonUtils.getSystemEnv("DEFAULT_DROPDOWN_PAGE_SIZE") == null ? 25
	// 		: (Integer.parseInt(CommonUtils.getSystemEnv("DEFAULT_DROPDOWN_PAGE_SIZE")));
	// public static final Integer defaultBucketSize = CommonUtils.getSystemEnv("DEFAULT_BUCKET_SIZE") == null ? 8
	// 		: (Integer.parseInt(CommonUtils.getSystemEnv("DEFAULT_BUCKET_SIZE")));

	// TWILIO
	public static final String twilioAccountSid = CommonUtils.getSystemEnv("TWILIO_ACCOUNT_SID") != null
			? CommonUtils.getSystemEnv("TWILIO_ACCOUNT_SID")
			: "TWILIO_ACCOUNT_SID";
	public static final String twilioAuthToken = CommonUtils.getSystemEnv("TWILIO_AUTH_TOKEN") != null
			? CommonUtils.getSystemEnv("TWILIO_AUTH_TOKEN")
			: "TWILIO_AUTH_TOKEN";
	public static final String twilioSenderMobile = CommonUtils.getSystemEnv("TWILIO_SENDER_MOBILE") != null
			? CommonUtils.getSystemEnv("TWILIO_SENDER_MOBILE")
			: "TWILIO_SENDER_MOBILE";

	/*
	 * NOTE: The variables above seem to be defaulting all the time.
	 * likely due to an issue with the environment variables not being present at initialisation.
	 * If it cannot be fixed, we may have to opt for doing it as seen below.
	 */

	/* ############ URL Methods ############## */
	
	/** 
	 * Return the project URL
	 *
	 * @return String
	 */
	public static String projectUrl() {
		return CommonUtils.getSystemEnv("PROJECT_URL") != null ? CommonUtils.getSystemEnv("PROJECT_URL")
			: "http://alyson7.genny.life";
	}
	
	/** 
	 * @return String
	 */
	public static String qwandaServiceUrl() {
		return CommonUtils.getSystemEnv("GENNY_API_URL") != null ? CommonUtils.getSystemEnv("GENNY_API_URL") 
			: (projectUrl() + ":8280");
	}
	
	/** 
	 * @return String
	 */
	public static String fyodorServiceUrl() {
		return CommonUtils.getSystemEnv("FYODOR_SERVICE_API") != null ? CommonUtils.getSystemEnv("FYODOR_SERVICE_API") 
			: (projectUrl() + ":4242");
	}

	/**
	* @return String
	 */
	public static String kogitoServiceUrl() {
		return CommonUtils.getSystemEnv("GENNY_KOGITO_SERVICE_URL") != null ? CommonUtils.getSystemEnv("GENNY_KOGITO_SERVICE_URL") 
			: (projectUrl() + ":4242");
	}

	/** 
	 * @return String
	 */
	public static String shleemyServiceUrl() {
		return CommonUtils.getSystemEnv("SHLEEMY_SERVICE_API") != null ? CommonUtils.getSystemEnv("SHLEEMY_SERVICE_API")
			: (projectUrl() + ":4242");
	}
	
	/** 
	 * Get the Infinispan host
	 *
	 * @return String
	 */
	public static String infinispanHost() {
		return CommonUtils.getSystemEnv("INFINISPAN_URL") != null ? CommonUtils.getSystemEnv("INFINISPAN_URL")
			: (projectUrl() + ":11222");
	}

	/** 
	 * Get the keyclak url
	 *
	 * @return String
	 */
	public static String keycloakUrl() {
		return CommonUtils.getSystemEnv("KEYCLOAK_URL") != null ? CommonUtils.getSystemEnv("KEYCLOAK_URL")
			: "http://keycloak.genny.life";
	}

	/* ############ UI Defaults ############## */

	/**
	* Get The default search page size.
	*
	* @return
	 */
	public static Integer defaultPageSize() {
		return CommonUtils.getSystemEnv("DEFAULT_PAGE_SIZE") != null
			? Integer.parseInt(CommonUtils.getSystemEnv("DEFAULT_PAGE_SIZE")) : 10;
	}

	/**
	* Get the default dropdown page size.
	*
	* @return
	 */
	public static Integer defaultDropDownPageSize() {
		return CommonUtils.getSystemEnv("DEFAULT_DROPDOWN_PAGE_SIZE") != null
			? Integer.parseInt(CommonUtils.getSystemEnv("DEFAULT_DROPDOWN_PAGE_SIZE")) : 25;
	}

	/**
	* Get the default bucket size.
	*
	* @return
	 */
	public static Integer defaultBucketSize() {
		return CommonUtils.getSystemEnv("DEFAULT_BUCKET_SIZE") != null
			? Integer.parseInt(CommonUtils.getSystemEnv("DEFAULT_BUCKET_SIZE")) : 8;
	}

}
