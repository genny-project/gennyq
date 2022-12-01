package life.genny.qwandaq.models;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.utils.CommonUtils;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

/**
 * Various Settings to be used in the Genny System
 **/
@RegisterForReflection
public class GennySettings {
	private static final Logger log = Logger.getLogger(GennySettings.class);

	/**
	 * Get System environment variable, providing a fallback if necessary. Will throw {@link IllegalStateException} if
	 * no fallback is present
	 * @param env - key of the system environment variable to get
	 * @param fallback - default value in the case the environment variable is not present
	 * @param alert - whether or not to log a warning if the environment variable is missing
	 * */
	private static final String getConfig(String env, String fallback) 
		throws IllegalStateException {
		return getConfig(env, fallback, false);
	}

	/**
	 * Get System environment variable, providing a fallback if necessary. Will throw {@link IllegalStateException} if
	 * no fallback is present
	 * @param env - key of the system environment variable to get
	 * @param fallback - default value in the case the environment variable is not present
	 * @param alert - whether or not to log a warning if the environment variable is missing
	 * */
	private static final String getConfig(String env, String fallback, boolean alert) 
		throws IllegalStateException {
		String value = CommonUtils.getSystemEnv(env, alert);
		if(StringUtils.isBlank(value)) {
			if(fallback != null) {
				log.warn("Using fallback for " + env + ": " + fallback);
				return fallback;
			}
			else {
				log.error("Missing required ENV: " + env);
				log.error("Please define this in your genny.env or System environment variables");
				throw new IllegalStateException("Missing required environment variable: " + env);
			}
		}
		return value;
	}
	
	/*
	 * NOTE: Static variables using getEnv seem to be defaulting all the time.
	 * This is likely due to an issue with the environment variables not being present at initialisation.
	 * Because of this, we are have to opt for doing it as seen below.
	 */

	/* ############ URL Methods ############## */
	
	/** 
	 * Return the project URL
	 *
	 * @return String
	 */
	public static String projectUrl() {
		return getConfig("PROJECT_URL", "http://alyson.genny.life", true);
	}
	
	/** 
	 * @return String
	 */
	public static String qwandaServiceUrl() {
		return getConfig("GENNY_API_URL", projectUrl() + ":8280");
	}
	
	/** 
	 * @return String
	 */
	public static String fyodorServiceUrl() {
		return getConfig("FYODOR_SERVICE_API", projectUrl() + ":4242", true);
	}

	/**
	* @return String
	 */
	public static String gadaqServiceUrl() {
		return getConfig("GENNY_GADAQ_SERVICE_URL", projectUrl() + ":6590", true);
	}

	/**
	* @return String
	 */
	public static String kogitoServiceUrl() {
		return getConfig("GENNY_KOGITO_SERVICE_URL", projectUrl() + ":6590", true);
	}

	/** 
	 * @return String
	 */
	public static String shleemyServiceUrl() {
		return getConfig("SHLEEMY_SERVICE_API", projectUrl() + ":6969", true);
	}

	/** 
	 * @return String
	 */
	public static String dataIndexUrl() {
		return getConfig("GENNY_KOGITO_DATAINDEX_HTTP_URL", projectUrl() + ":8582", true);
	}
	
	/** 
	 * Get the Infinispan host
	 *
	 * @return String
	 */
	public static String infinispanHost() {
		return getConfig("INFINISPAN_URL", projectUrl() + ":11222", true);
	}

	/** 
	 * Get the keyclak url
	 *
	 * @return String
	 */
	public static String keycloakUrl() {
		return getConfig("GENNY_KEYCLOAK_URL", "http://keycloak.genny.life");
	}

	/* ############ UI Defaults ############## */

	/**
	* Get The default search page size.
	*
	* @return String
	 */
	public static Integer defaultPageSize() {
		return Integer.parseInt(getConfig("DEFAULT_PAGE_SIZE", "10"));
	}

	/**
	* Get the default dropdown page size.
	*
	* @return String
	 */
	public static Integer defaultDropDownPageSize() {
		return Integer.parseInt(getConfig("DEFAULT_DROPDOWN_PAGE_SIZE", "25"));
	}

	/**
	* Get the default bucket size.
	*
	* @return String
	 */
	public static Integer defaultBucketSize() {
		return Integer.parseInt(getConfig("DEFAULT_BUCKET_SIZE", "8"));
	}

	/**
	 * Get executorThreadCount
	 * @return Integer
	 */
	public static Integer executorThreadCount() {
		return Integer.parseInt(getConfig("EXECUTOR_THREAD_COUNT", "200"));
	}


	/**
	 * Get minIO server url
	 * @return String
	 */
	public static String minIOServerUrl() {
		return getConfig("MINIO_SERVER_URL", "http://localhost:9000", true);
	}

	/**
	 * Get minIO access key
	 * @return String
	 */
	public static String minIOAccessKey() {
		return getConfig("MINIO_ACCESS_KEY", "accessKey", true);
	}
	/**
	 * Get minIO secret key
	 * @return String
	 */
	public static String minIOSecretKey() {
		return getConfig("MINIO_SECRET_KEY", "secretKey", true);
	}
	/**
	 * Get minIO bucket name
	 * @return String
	 */
	public static String minIOBucketName() {
		return getConfig("BUCKET_NAME", "media", true);
	}
}
