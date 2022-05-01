package life.genny.qwandaq.security.keycloak;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import life.genny.qwandaq.exception.GennyKeycloakException;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.security.keycloak.client.KeycloakHttpClient;

/**
 * TokenVerification --- Handles the signature of the token to check if it was signed 
 * by the right Authorization server
 *
 * @author    hello@gada.io
 *
 */
@ApplicationScoped
public class TokenVerification {

	private static final Logger log = Logger.getLogger(TokenVerification.class);

	static Jsonb jsonb = JsonbBuilder.create();

	@Inject JWTParser parser;
	@Inject @RestClient KeycloakHttpClient client;

	Map<String, CertsResponse> certStore = new HashMap<>();

	/**
	 * Checks in the certStore if the Public Certs has been fetched from a particular 
	 * realm if it exists then retrieve otherwise fetch it.  
	 *
	 * @param realm Realm from Keycloak 
	 *
	 * @return CertsReponse
	 */
	CertsResponse getCert(String realm) {

		if (certStore.containsKey(realm)) {
			return certStore.get(realm);
		}

		log.info("The public cert for realm " + realm + " will be retrieved...");
		certStore.put(realm, client.fetchRealmCerts(realm));

		return certStore.get(realm);
	}

	/**
	 * Verify from the cert that the token has been signed from the right Authorization 
	 * server
	 *
	 * @param realm Realm from Keycloak 
	 * @param token Bearer token 
	 *
	 * @return The payload encoded in the token parsed to a GennyToken
	 *
	 * @throws GennyKeycloakException throws exception if the public key couldn't be generated
	 * or the wrong algorithm type was used or the token couldn't be parsed to a GennyToken
	 * object
	 */
	public GennyToken verify(String realm, String token) throws GennyKeycloakException {

		CertsResponse certs = getCert(realm);
		Optional<KeycloakRealmKey> keyMaybe = certs.keys.stream().findFirst();

		if (keyMaybe.isEmpty()) {
			throw new GennyKeycloakException(
					"KEY_NOT_FOUND_ERROR",
					"Not Certificate public key found for realm " + realm, 
					new NullPointerException());
		}

		KeycloakRealmKey key = keyMaybe.get();
		PublicKey publicKey = null;
		JsonWebToken result = null;

		try {
			publicKey = KeyFactory.getInstance(key.kty)
				.generatePublic(new RSAPublicKeySpec(key.getDecodedModulus(), key.getDecodedExponent()));
			result = parser.verify(token, publicKey);

		} catch (InvalidKeySpecException | NoSuchAlgorithmException exception) {
			throw new GennyKeycloakException(
					"CERTIFICATE_KEY_ERROR", 
					"Something wrong with the Certificte key fields"+
					" maybe the wrong algorithm is used or wrong jwt specifications",
					exception);
		} catch (ParseException exception) {
			throw new GennyKeycloakException(
					"PARSE_ERROR",
					"An error occurred when parsing the token",
					exception);
		} catch (Exception e) {
			e.printStackTrace();
		}

		GennyToken payload = new GennyToken(result.getRawToken());

		return payload;
	}
}
