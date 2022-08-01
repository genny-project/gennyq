package life.genny.qwandaq.utils;

import java.security.Key;
import java.util.Date;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.InvalidKeyException;
import life.genny.qwandaq.exception.NullParameterException;
import life.genny.qwandaq.models.GennyToken;

/**
 * A utility class dedicated to security related operations.
 * 
 * @author Jasper Robison
 */
public class SecurityUtils {

	static final Logger log = Logger.getLogger(SecurityUtils.class);
	static Jsonb jsonb = JsonbBuilder.create();
	public static final String SERVICE_USERNAME = "service";

	/**
	* Function to validate the authority for a given token string
	*
	* @param token the jwt to check.
	* @return Boolean representing whether or not the token is verified.
	 */
	public static Boolean isAuthorizedToken(String token) {

		if (token == null)
			throw new NullParameterException("token");

		if (token.startsWith("Bearer ")) {
			token = token.substring("Bearer ".length());
		}

		GennyToken gennyToken = new GennyToken(token);

		return isAuthorisedGennyToken(gennyToken);
	}

	/**
	* Function to validate the authority for a given {@link GennyToken}.
	*
	* @param gennyToken the gennyToken to check
	* @return Boolean
	 */
	public static Boolean isAuthorisedGennyToken(GennyToken gennyToken) {

		if (gennyToken.hasRole("admin", "service", "dev")) {
			return true;
		}

		log.error(gennyToken.getUserCode() + " is not authorized!");

		return false;
	}

	/**
	* Function to validate the authority for a given token string 
	* and return the GennyToken object. Returns null if token is not valid.
	*
	* @param token the jwt to check.
	* @return The GennyToken object.
	 */
	public static GennyToken getAuthorizedToken(String token) {

		if (token == null)
			throw new NullParameterException("token");

		// clean bearer prefix and any whitespace
		token = StringUtils.removeStart(token, "Bearer");
		token = StringUtils.strip(token);

		return new GennyToken(token);
	}

	/**
	* Helper to check if a token corresponds to a service user.
	*
	* @param token The GennyToken to check
	* @return Boolean
	 */
	public Boolean tokenIsServiceUser(GennyToken token) {
		return SERVICE_USERNAME.equals(token.getUsername());
	}

	/** 
	 * Create a JWT
	 *
	 * @param id the id to set
	 * @param issuer the issuer to set
	 * @param subject the subject to set
	 * @param ttlMillis the ttlMillis to set
	 * @param apiSecret the apiSecret to set
	 * @param claims the claims to set
	 * @return String
	 */
	public static String createJwt(String id, String issuer, String subject, long ttlMillis, String apiSecret,
			Map<String, Object> claims) {

		// The JWT signature algorithm we will be using to sign the token
		SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
		String aud = issuer;
		if (claims.containsKey("aud")) {
			aud = (String) claims.get("aud");
			claims.remove("aud");
		}
		long nowMillis = System.currentTimeMillis();
		Date now = new Date(nowMillis);

		// We will sign our JWT with our ApiKey secret
		byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(apiSecret);

		// Let's set the JWT Claims
		JwtBuilder builder = Jwts.builder().setId(id).setIssuedAt(now).setSubject(subject).setIssuer(issuer)
			.setAudience(aud).setClaims(claims);

		Key key = null;

		try {
			key = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
			builder.signWith(key, SignatureAlgorithm.HS256);

		} catch (Exception e) {
			try {
				key = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
				builder.signWith(key, SignatureAlgorithm.HS256);

			} catch (Exception e1) {
				try {
					Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
					builder.signWith(signingKey, signatureAlgorithm);

				} catch (InvalidKeyException e2) {
					log.error(e2);
				}
			}
		}

		// if it has been specified, let's add the expiration
		if (ttlMillis >= 0) {
			long expMillis = nowMillis + ttlMillis;
			Date exp = new Date(expMillis);
			builder.setExpiration(exp);
		}

		// Builds the JWT and serializes it to a compact, URL-safe string
		return builder.compact();
	}

	/**
	 * Obfuscate the token from a stringified JsonObject.
	 * @param json the complete json object string
	 * @return The secure json object string
	 */
	public static String obfuscate(String json) {

		JsonObject obj = jsonb.fromJson(json, JsonObject.class);

		return obfuscate(obj).toString();
	}

	/**
	 * Obfuscate the token from a JsonObject.
	 * @param json the complete json object
	 * @return The secure json object
	 */
	public static JsonObject obfuscate(JsonObject json) {

		if (!json.containsKey("token"))
			return json;

		return Json.createObjectBuilder(json).remove("token").build();
	}

	/**
	 * It checks that no confidential information has been leaked. It will delete the key properties
	 * if it finds any
	 *
	 * @param json A JsonObject
	 * @return A JsonObject without the confidential key properties
	 */
	public static JsonObject removeKeys(final JsonObject json) {

		if (json.containsKey("token")) {
			json.remove("token");
		}

		if (json.containsKey("recipientCodeArray")) {
			json.remove("recipientCodeArray");
		}

		return json;
	}
}
