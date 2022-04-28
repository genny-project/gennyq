package life.genny.qwandaq.utils;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import java.security.Key;
import java.util.Date;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.InvalidKeyException;
import life.genny.qwandaq.models.GennyToken;

/**
 * A utility class dedicated to security related operations.
 * 
 * @author Jasper Robison
 */
public class SecurityUtils {

	public static final String SERVICE_USERNAME = "service";

	static final Logger log = Logger.getLogger(SecurityUtils.class);

	/**
	* Function to validate the authority for a given token string
	*
	* @param token the jwt to check.
	* @return Boolean representing whether or not the token is verified.
	 */
	public static Boolean isAuthorizedToken(String token) {

		if (token == null) {
			log.error("Token is null!");
			return false;
		}

		if (token.startsWith("Bearer ")) {
			token = token.substring("Bearer ".length());
		}

		GennyToken gennyToken = null;
		try {
			gennyToken = new GennyToken(token);
		} catch (Exception e) {
			log.errorv("Unable to create GennyToken from token: {}", token);
			log.error(e);
		}

		if (gennyToken == null) {
			return false;
		}

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

		if (token == null) {
			log.error("Token is null!");
			return null;
		}

		// clean bearer prefix and any whitespace
		token = StringUtils.removeStart(token, "Bearer");
		token = StringUtils.strip(token);

		try {
			return new GennyToken(token);
		} catch (Exception e) {
			log.errorv("Unable to create GennyToken from token: {}", token);
			log.error(e);
		}

		return null;
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
}
