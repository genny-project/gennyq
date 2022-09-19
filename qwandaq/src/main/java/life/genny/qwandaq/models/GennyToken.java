package life.genny.qwandaq.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.utils.CommonUtils;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.annotation.JsonbTransient;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

@RegisterForReflection
public class GennyToken implements Serializable {

	private static final long serialVersionUID = 1L;
	static final Logger log = Logger.getLogger(GennyToken.class);
	static Jsonb jsonb = JsonbBuilder.create();

	public String code;
	public String userCode;
	public String userUUID;
	public String token;
	public String realm;

	public String keycloakRealm;
	public String productCode;
	public String[] allowedProducts;

	public Map<String, Object> adecodedTokenMap = null;
	public Set<String> userRoles = new HashSet<String>();

	/**
	 * Constructor. Default.
	 */
	public GennyToken() {
	}

	/**
	 * Constructor.
	 * Create a GennyToken from a code and jwt string.
	 *
	 * @param code  The code and userCode to set
	 * @param token The jwt string
	 */
	public GennyToken(final String code, final String token) {

		this(token);
		this.code = code;
		if ("PER_SERVICE".equals(code)) {
			this.userCode = code;
		}
	}

	/**
	 * Constructor.
	 * Create a GennyToken from a jwt string.
	 *
	 * @param token The jwt string
	 */
	public GennyToken(final String token) {
		init(token);
	}

	/**
	 * Initialise the current token object.
	 *
	 * @param token The token string to init with
	 */
	public void init(String token) {

		if (token == null || token.isEmpty()) {
			log.error("Token must not be null or empty!");
			return;
		}

		this.token = token;

		// get decoded map of token
		this.adecodedTokenMap = getJsonMap(token);

		if (this.adecodedTokenMap == null) {
			log.error("Token cannot be decoded!");
			return;
		}

		// extract realm name from iss value
		String iss = getString("iss");

		if (iss != null) {
			String[] issArray = iss.split("/");
			String tokenRealm = issArray[issArray.length - 1];

			this.realm = tokenRealm;
			this.keycloakRealm = tokenRealm;
		}

		// extract product code from azp
		String azp = getString("azp");

		// If azp is alyson then it must be internmatch
		// NOTE: solution would be to eliminate any internmatch
		// tokens using this client id.
		if (("alyson".equals(azp)) || ("backend".equals(azp))) {
			// log.warn("alyson clientid detected.");
			azp = "internmatch";
		}

		if (azp != null) {
			this.productCode = azp;

			// use client id as realm if not already found
			if (this.realm == null) {
				this.realm = azp;
			}
		}

		String prodCodeStr = CommonUtils.getSystemEnv("PRODUCT_CODES");
		if(prodCodeStr != null) {
			String[] productCodes = prodCodeStr.split(":");
			if(productCodes.length == 1) {
				log.info("Overriding Product code with: " + productCodes[0]);
				this.productCode = productCodes[0];
			}
		} 

		// add realm name to the decoded token
		this.adecodedTokenMap.put("realm", this.realm);

		String username = getString("preferred_username");
		this.userUUID = "PER_" + this.getUuid().toUpperCase();

		if ("service".equals(username)) {
			this.userCode = "PER_SERVICE";
		} else {
			this.userCode = userUUID;
		}

		setupRoles();
	}

	/**
	 * @return String
	 */
	public String getToken() {
		return token;
	}

	/**
	 * @return String
	 */
	public String getRealm() {
		if (realm == null) {
			log.error("Null realm in GennyToken!");
		}
		return realm;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public void setUserUUID(String userUUID) {
		this.userUUID = userUUID;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	/**
	 * Set the userCode
	 *
	 * @param userCode the user code to set
	 * @return String
	 */
	public String setUserCode(String userCode) {
		return this.userCode = userCode;
	}

	/**
	 * @return String
	 */
	public String getUserUUID() {
		return userUUID;
	}

	public void setUserRoles(Set<String> userRoles) {
		this.userRoles = userRoles;
	}

	/**
	 * @return the userRoles
	 */
	public Set<String> getUserRoles() {
		return userRoles;
	}

	public String getProductCode() {
		return productCode;
	}

	public void setProductCode(String productCode) {
		this.productCode = productCode;
	}

	public String[] getAllowedProducts() {
		return allowedProducts;
	}

	public void setAllowedProducts(String[] allowedProducts) {
		this.allowedProducts = allowedProducts;
	}

	public String getKeycloakRealm() {
		return keycloakRealm;
	}

	public void setKeycloakRealm(String keycloakRealm) {
		this.keycloakRealm = keycloakRealm;
	}

	/**
	 * @return Map&lt;String, Object&gt;
	 */
	public Map<String, Object> getAdecodedTokenMap() {
		return adecodedTokenMap;
	}

	/**
	 * @param adecodedTokenMap the decoded token map to set
	 */
	public void setAdecodedTokenMap(Map<String, Object> adecodedTokenMap) {
		this.adecodedTokenMap = adecodedTokenMap;
	}

	/**
	 * @return String
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @return String
	 */
	@JsonbTransient
	@JsonIgnore
	public String getSessionCode() {
		return getString("session_state");
	}

	/**
	 * @return String
	 */
	@JsonbTransient
	@JsonIgnore
	public String getUsername() {
		return getString("preferred_username");
	}

	/**
	 * @return String
	 */
	@JsonbTransient
	@JsonIgnore
	public String getKeycloakUrl() {
		String fullUrl = getString("iss");
		URI uri;
		try {
			uri = new URI(fullUrl);
			String domain = uri.getHost();
			String proto = uri.getScheme();
			Integer port = uri.getPort();
			return proto + "://" + domain + ":" + port;
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return "http://keycloak.genny.life";
	}

	/**
	 * @return String
	 */
	@JsonbTransient
	@JsonIgnore
	public String getClientCode() {
		return getString("aud");
	}

	/**
	 * @return String
	 */
	@JsonbTransient
	@JsonIgnore
	public String getEmail() {
		return getString("email");
	}

	/**
	 * @return String the userCode
	 */
	public String getUserCode() {
		return userCode;
	}

	/**
	 * @return String the token jti field
	 */
	@JsonbTransient
	@JsonIgnore
	public String getJTI() {
		return getString("jti");
	}

	/**
	 * @return String
	 */
	@JsonbTransient
	@JsonIgnore
	public String getUuid() {
		String uuid = null;

		try {
			uuid = (String) adecodedTokenMap.get("sub");
		} catch (Exception e) {
			log.info("Not a valid user");
		}

		return uuid;
	}

	/**
	 * @return String
	 */
	@Override
	public String toString() {
		return getProductCode() + ": " + getCode() + ": " + getUserCode() + ": " + this.userRoles;
	}

	/**
	 * @param key the key of the string item to get
	 * @return String
	 */
	@JsonbTransient
	@JsonIgnore
	public String getString(final String key) {
		return (String) adecodedTokenMap.get(key);
	}

	private void setupRoles() {
		String realm_accessStr = "";
		if (adecodedTokenMap.get("realm_access") == null) {
			userRoles.add("user");
		} else {
			realm_accessStr = adecodedTokenMap.get("realm_access").toString();
			Pattern p = Pattern.compile("(?<=\\[)([^\\]]+)(?=\\])");
			Matcher m = p.matcher(realm_accessStr);

			if (m.find()) {
				String[] roles = m.group(1).split(",");
				for (String role : roles) {
					userRoles.add((String) role.trim());
				}
				;
			}
		}

	}

	/**
	 * <p>
	 * Checks the {@link GennyToken#userRoles} for any of the supplied roles
	 * </p>
	 * 
	 * @param roles the roles to check against
	 * @return boolean whether or not the token has at least one of the roles
	 */
	public boolean hasRole(final String... roles) {
		return hasRole(false, roles);
	}

	/**
	 * <p>
	 * A method to for checking the token roles. Can check for a match on all of the
	 * roles or at least one of the roles
	 * </p>
	 * 
	 * @param roles       the roles to check against
	 * @param requiresAll whether or not the token is required to have all roles
	 *                    supplied
	 *                    - Default: <b>false</b>
	 * @return boolean whether or not the token has either all of the roles or at
	 *         least one of the roles (depending on requiresAll)
	 */
	public boolean hasRole(final boolean requiresAll, final String... roles) {
		/*
		 * if we require all to be checked, check if any are missing. If there is one
		 * missing return false
		 * if we don't require all to be checked, return true if found at least 1
		 * 
		 * on return, if we require all and we didn't return early, return true (missing
		 * 1 match)
		 * on return, if we didn't require all and we didn't return early, return false
		 * (no match)
		 */
		for (String role : roles) {
			boolean hasRole = userRoles.contains(role);

			if (requiresAll) {
				if (!hasRole) {
					return false;
				}
			} else {
				if (hasRole) {
					return true;
				}
			}
		}

		return requiresAll;
	}

	/**
	 * @return LocalDateTime
	 */
	@JsonbTransient
	@JsonIgnore
	public LocalDateTime getAuthDateTime() {
		Long auth_timestamp = null;
		try {
			auth_timestamp = ((Number) adecodedTokenMap.get("auth_time")).longValue();
		} catch (Exception e) {
			auth_timestamp = ((Number) adecodedTokenMap.get("iat")).longValue(); // this is the 'issued at' timestamp
		}
		LocalDateTime authTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(auth_timestamp),
				TimeZone.getDefault().toZoneId());
		return authTime;
	}

	/**
	 * @return LocalDateTime
	 */
	@JsonbTransient
	@JsonIgnore
	public LocalDateTime getExpiryDateTime() {
		Long exp_timestamp = ((Number) adecodedTokenMap.get("exp")).longValue();
		LocalDateTime expTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(exp_timestamp),
				TimeZone.getDefault().toZoneId());
		return expTime;
	}

	/**
	 * @return OffsetDateTime
	 */
	@JsonbTransient
	@JsonIgnore
	public OffsetDateTime getExpiryDateTimeInUTC() {

		Long exp_timestamp = ((Number) adecodedTokenMap.get("exp")).longValue();
		LocalDateTime expTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(exp_timestamp),
				TimeZone.getDefault().toZoneId());
		ZonedDateTime ldtZoned = expTime.atZone(ZoneId.systemDefault());
		ZonedDateTime utcZoned = ldtZoned.withZoneSameInstant(ZoneId.of("UTC"));

		return utcZoned.toOffsetDateTime();
	}

	/**
	 * @return Integer
	 */
	@JsonbTransient
	@JsonIgnore
	public Integer getSecondsUntilExpiry() {

		OffsetDateTime expiry = getExpiryDateTimeInUTC();
		LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
		Long diff = expiry.toEpochSecond() - now.toEpochSecond(ZoneOffset.UTC);
		return diff.intValue();
	}

	/**
	 * @return LocalDateTime the JWT Issue datetime object
	 */
	@JsonbTransient
	@JsonIgnore
	public LocalDateTime getiatDateTime() {
		Long iat_timestamp = ((Number) adecodedTokenMap.get("iat")).longValue();
		LocalDateTime iatTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(iat_timestamp),
				TimeZone.getDefault().toZoneId());
		return iatTime;
	}

	/**
	 * @return String
	 */
	@JsonbTransient
	@JsonIgnore
	public String getEmailUserCode() {
		String username = (String) adecodedTokenMap.get("preferred_username");
		String normalisedUsername = getNormalisedUsername(username);
		return "PER_" + normalisedUsername.toUpperCase();

	}

	/**
	 * @param rawUsername the raw username to normalize
	 * @return String
	 */
	@JsonbTransient
	@JsonIgnore
	public String getNormalisedUsername(final String rawUsername) {
		if (rawUsername == null) {
			return null;
		}
		String username = rawUsername.replaceAll("\\&", "_AND_").replaceAll("@", "_AT_").replaceAll("\\.", "_DOT_")
				.replaceAll("\\+", "_PLUS_").toUpperCase();
		// remove bad characters
		username = username.replaceAll("[^a-zA-Z0-9_]", "");
		return username;

	}

	/**
	 * @param userCode the userCode to check
	 * @return Boolean
	 */
	public Boolean checkUserCode(String userCode) {
		if (getUserCode().equals(userCode)) {
			return true;
		}
		if (getEmailUserCode().equals(userCode)) {
			return true;
		}
		return false;

	}

	/**
	 * @return the realm and usercode concatenated
	 */
	@JsonbTransient
	@JsonIgnore
	public String getRealmUserCode() {
		return getRealm() + "+" + getUserCode();
	}

	/**
	 * @param json the json string to get
	 * @return Map&lt;String, Object&gt;
	 */
	@JsonbTransient
	@JsonIgnore
	public Map<String, Object> getJsonMap(final String json) {
		final JsonObject jsonObj = getDecodedToken(json);
		return getJsonMap(jsonObj);
	}

	/**
	 * @param jsonObj the json object to get
	 * @return Map&lt;String, Object&gt;
	 */
	@JsonbTransient
	@JsonIgnore
	public static Map<String, Object> getJsonMap(final JsonObject jsonObj) {
		final String json = jsonObj.toString();
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			final ObjectMapper mapper = new ObjectMapper();
			// convert JSON string to Map
			final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
			};

			map = mapper.readValue(json, typeRef);

		} catch (final JsonGenerationException e) {
			e.printStackTrace();
		} catch (final JsonMappingException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return map;
	}

	/**
	 * @param bearerToken the bearer token to set
	 * @return JsonObject
	 */
	@JsonbTransient
	@JsonIgnore
	public JsonObject getDecodedToken(final String bearerToken) {

		final String[] chunks = bearerToken.split("\\.");
		if (chunks.length < 2 || StringUtils.isBlank(bearerToken)) {
			log.error("Invalid Bearer Token!");
			log.error("Bearer: [" + bearerToken + "]");
		}
		Base64.Decoder decoder = Base64.getDecoder();
		String payload = new String(decoder.decode(chunks[1]));
		JsonObject json = jsonb.fromJson(payload, JsonObject.class);

		return json;
	}

}
