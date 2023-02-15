package life.genny.qwandaq.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.net.ssl.HttpsURLConnection;
import javax.ws.rs.core.Response;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.entity.BaseEntity;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.util.JsonSerialization;

import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.exception.runtime.KeycloakException;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.models.ServiceToken;

/**
 * A static utility class used for standard requests and
 * operations involving Keycloak.
 * 
 * @author Adam Crow
 * @author Jasper Robison
 */
@ApplicationScoped
public class KeycloakUtils {

    @Inject
    EntityAttributeUtils beaUtils;

    @Inject
    ServiceToken serviceToken;

	private static Logger log = Logger.getLogger(KeycloakUtils.class);

    static Jsonb jsonb = JsonbBuilder.create();

    /**
     * Fetch a GennyToken for a user using a username, password and client details.
     * 
     * @param keycloakUrl the keycloakUrl to use
     * @param realm       the realm to use
     * @param clientId    the clientId to use
     * @param secret      the secret to use
     * @param username    the username to use
     * @param password    the password to use
     * @return GennyToken
     */
    public static GennyToken getGennyToken(String keycloakUrl, String realm, String clientId, String secret,
            String username, String password) {

        String token = getToken(keycloakUrl, realm, clientId, secret, username, password);

        GennyToken gennyToken = new GennyToken(token);
        return gennyToken;
    }

    /**
     * Fetch an access token for a user using a username, password and client
     * details.
     * 
     * @param keycloakUrl the keycloakUrl to use
     * @param realm       the realm to use
     * @param clientId    the clientId to use
     * @param secret      the secret to use
     * @param username    the username to use
     * @param password    the password to use
     * @return String
     */
    public static String getToken(String keycloakUrl, String realm, String clientId, String secret, String username,
            String password) {

        Map<String, String> params = new HashMap<>();

        log.info("keycloakUrl:" + keycloakUrl);
        log.info("realm:" + realm);
        log.info("clientId:" + clientId);
        log.info("secret:" + secret);
        log.info("username:" + username);
        log.info("password:" + password);

        params.put("username", username);
        params.put("password", password);
        params.put("grant_type", "password");
        params.put("client_id", clientId);

        if ((!StringUtils.isBlank(secret)) && (!"nosecret".equals(secret))) {
            params.put("client_secret", secret);
        }
        log.info("parms:" + params);

        String token = fetchOIDCToken(keycloakUrl, realm, params);

        return token;
    }

    /**
     * Fetch an access token for a user using a refresh token.
     * 
     * @param keycloakUrl  the keycloakUrl to use
     * @param realm        the realm to use
     * @param clientId     the clientId to use
     * @param secret       the secret to use
     * @param refreshToken the refreshToken to use
     * @return GennyToken
     */
    public static GennyToken getToken(String keycloakUrl, String realm, String clientId, String secret,
            String refreshToken) {

        HashMap<String, String> params = new HashMap<>();

        params.put("refresh_token", refreshToken);
        params.put("grant_type", "refresh_token");
        params.put("client_id", clientId);

        if (!StringUtils.isBlank(secret)) {
            params.put("client_secret", secret);
        }

        String token = fetchOIDCToken(keycloakUrl, realm, params);

        GennyToken gennyToken = new GennyToken(token);
        return gennyToken;
    }

    /**
     * Fetch an Impersonated Token for a user.
     *
     * @param userBE     the userBE to get a token for
     * @param gennyToken the gennyToken to use to fetch the token
     * @param project    the project to use to fetch the token
     * @return String
     */
    public String getImpersonatedToken(BaseEntity userBE, GennyToken gennyToken, BaseEntity project) {

        String realm = gennyToken.getRealm();
        String token = gennyToken.getToken();
        String keycloakUrl = gennyToken.getKeycloakUrl();

        if (userBE == null) {
            log.error(ANSIColour.RED + "User BE is NULL" + ANSIColour.RESET);
            return null;
        }

        // grab uuid to fetch token
        String uuid = userBE.getValue("PRI_UUID", null);

        if (uuid == null) {

            log.warn(ANSIColour.YELLOW + "No PRI_UUID found for user " + userBE.getCode()
                    + ", attempting to use PRI_EMAIL instead" + ANSIColour.RESET);

            // grab email to fetch token
            String email = userBE.getValue("PRI_EMAIL", null);

            if (email == null) {
                log.error(ANSIColour.RED + "No PRI_EMAIL found for user " + userBE.getCode() + ANSIColour.RESET);
                return null;
            }

            // use email as backup
            uuid = email;
        } else {
            // use lowercase UUID
            uuid = uuid.toLowerCase();
        }

        // fetch keycloak json from project entity
        String projectCode = project.getCode();
        String keycloakJson = beaUtils.getEntityAttribute(realm, projectCode, "ENV_KEYCLOAK_JSON", false).getValueString();
        JsonReader reader = Json.createReader(new StringReader(keycloakJson));
        String secret = reader.readObject().getJsonObject("credentials").getString("secret");
        reader.close();

        // setup param map
        HashMap<String, String> params = new HashMap<>();
        params.put("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
        params.put("subject_token", token);
        params.put("requested_subject", uuid);
        params.put("client_id", realm);

        if (secret != null && !StringUtils.isBlank(secret)) {
            params.put("client_secret", secret);
        }

        return fetchOIDCToken(keycloakUrl, realm, params);
    }

    /**
     * Fetch an Impersonated Token for a user.
     *
     * @param serviceToken the service user Token
     * @param userCode     the user to use to fetch the token
     * 
     * @return String
     */
    public static String getImpersonatedToken(ServiceToken serviceToken, String userCode) {

        String productCode = serviceToken.getProductCode();
        String keycloakUrl = serviceToken.getKeycloakUrl().replace(":-1", "");

        // grab uuid to fetch token
        String uuid = userCode.substring(Prefix.PER_.length()).toLowerCase();

        // setup param map
        HashMap<String, String> params = new HashMap<>();
        params.put("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
        params.put("subject_token", serviceToken.getToken());
        params.put("requested_subject", uuid);
        params.put("client_id", System.getenv("GENNY_CLIENT_ID"));
        params.put("client_secret", System.getenv("GENNY_CLIENT_SECRET"));

        log.debug("client_secret=" + System.getenv("GENNY_CLIENT_SECRET"));
        String token = fetchOIDCToken(keycloakUrl, productCode, params);
        log.debug("impersonated token=" + token);
        return token;
    }

    /**
     * Fetch an OIDC access token from keycloak.
     *
     * @param keycloakUrl the keycloakUrl to fetch from
     * @param realm       the realm to fetch in
     * @param params      the params to use
     * @return String
     */
    public static String fetchOIDCToken(String keycloakUrl, String realm, Map<String, String> params) {
        log.info("Keycloak Realm is " + realm);

        String uri = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";
        log.info("Fetching OIDC Token from " + uri);

        String str = executeEncodedPostRequest(uri, params);
        // log.info("encodedPostRequest:[" + str + "]");
        JsonObject json = jsonb.fromJson(str, JsonObject.class);
        String token = json.getString("access_token");

        return token;
    }

    /**
     * Perform Custom encoded POST request.
     *
     * @param uri            the uri to request from
     * @param postDataParams the postDataParams to use in rquest
     * @return String
     */
    public static String executeEncodedPostRequest(String uri, Map<String, String> postDataParams) {

        try {

            // setup connection
            URL url = new URL(uri);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(postDataParams));

            // flush and close
            writer.flush();
            writer.close();
            os.close();

            String response = "";
            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = br.readLine()) != null) {
                response += line;
            }
            if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                log.debug("Successful Token Request!");
                return response;
            } else {
                log.error("Bad Token Request: " + conn.getResponseCode() + " " + conn.getResponseMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Build POST query data string.
     *
     * @param params the params to construct the query with
     * @return String
     */
    public static String getPostDataString(Map<String, String> params) {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                log.debug("key: " + entry.getKey() + ", value: " + entry.getValue());
                if (first)
                    first = false;
                else
                    result.append("&");

                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            log.error("Error encoding Post Data String: " + e.getStackTrace());
        }

        return result.toString();
    }

    /**
     * Initialise a Dummy User in keycloak.
     *
     * @param token the token to use to create the user
     * @param realm the realm to create the user under
     * @return the user uuid
     */
    public static String createDummyUser(GennyToken token, String realm) {
        return createDummyUser(token.getToken(), realm);
    }

    /**
     * Initialise a Dummy User in keycloak.
     *
     * @param token the token to use to create the user
     * @param realm the realm to create the user in
     * @return the user uuid
     */
    @Deprecated
    public static String createDummyUser(String token, String realm) {

        String username = UUID.randomUUID().toString().substring(0, 18);
        String email = username + "@gmail.com";
        String defaultPassword = GennySettings.dummyUserPassword();

        String json = "{ " + "\"username\" : \"" + username + "\"," + "\"email\" : \"" + email + "\" , "
                + "\"enabled\" : true, " + "\"emailVerified\" : true, " + "\"firstName\" : \"" + username + "\", "
                + "\"lastName\" : \"" + username + "\", " + "\"groups\" : [" + " \"users\" " + "], "
                + "\"requiredActions\" : [\"terms_and_conditions\"], "
                + "\"realmRoles\" : [\"user\"],\"credentials\": [{"
                + "\"type\":\"password\","
                + "\"value\":\"" + defaultPassword + "\","
                + "\"temporary\":true }]}";

        log.info("CreateUserjsonDummy = " + json);

        String uri = GennySettings.keycloakUrl() + "/admin/realms/" + realm + "/users";

        log.info("Create keycloak user - url:" + uri + ", token:" + token);

        HttpResponse<String> response = HttpUtils.post(uri, json, token);
        if (response == null)
			throw new KeycloakException("Failed to create user: Response is null");

        Integer statusCode = response.statusCode();
        Response.Status status = Response.Status.fromStatusCode(statusCode);
        log.info("Create User Response Status: " + statusCode);

        try {
            // if user already exists, return their id
            if (status == Response.Status.CONFLICT) {
                log.warn("Email already taken: " + email);
                return getKeycloakUserId(token, realm, username);
            }

            if (Response.Status.Family.familyOf(statusCode) == Response.Status.Family.SUCCESSFUL) {
                log.info("Successfully Created User");
                return getKeycloakUserId(token, realm, username);
            }

        } catch (IOException e) {
			throw new KeycloakException("Failed to create user: ", e);
        }

		throw new KeycloakException("Failed to create user: " + response.body());
    }

    /**
     * Fetch a keycloak users Id using a username.
     *
     * @param token    the token to fetch with
     * @param realm    the realm to fetch in
     * @param username the username to fetch for
     * @return Strign
     * @throws IOException if id could not be fetched
     */
    public static String getKeycloakUserId(final String token, final String realm, final String username)
            throws IOException {

        final List<LinkedHashMap<?, ?>> users = fetchKeycloakUser(token, realm, username);
        if (!users.isEmpty()) {
            return (String) users.get(0).get("id");
        }
        return null;
    }

    /**
     * Fetch a keycloak user using a username.
     *
     * @param token    the token to fetch with
     * @param realm    the realm to fetch in
     * @param username the username to fetch for
     * @return List
     */
    public static List<LinkedHashMap<?, ?>> fetchKeycloakUser(final String token, final String realm,
            final String username) {

        String uri = GennySettings.keycloakUrl() + "/admin/realms/" + realm + "/users?username=" + username;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
                .setHeader("Content-Type", "application/json")
                .setHeader("Authorization", "Bearer " + token)
                .GET().build();

        try {

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response == null) {
                log.error("Response was null from keycloak!!!");
                return null;
            }

            int statusCode = response.statusCode();
            log.info("Fetch User Response Status: " + statusCode);

            if (statusCode != 200) {
                log.error("Failed to get users from Keycloak, url:" + uri + ", response code:" + statusCode + ", token:"
                        + token);
                return null;
            }

            List<LinkedHashMap<?, ?>> results = new ArrayList<LinkedHashMap<?, ?>>();

            final InputStream is = new ByteArrayInputStream(response.body().getBytes("UTF-8"));
            try {
                results = JsonSerialization.readValue(is, (new ArrayList<UserRepresentation>()).getClass());
            } finally {
                is.close();
            }

            return results;

        } catch (IOException | InterruptedException e) {
            log.error(e);
        }

        return null;
    }

    /**
     * Update a keycloak user field.
     *
     * @param userToken the userToken to update with
     * @param user      the user to update for
     * @param field     the field to update
     * @param value     the value to update to
     * @return int statusCode
     */
    public int updateUserField(BaseEntity user, String field, String value) {

        String realm = serviceToken.getKeycloakRealm();
		String productCode = user.getRealm();
		String userCode = user.getCode();

		EntityAttribute id = beaUtils.getEntityAttribute(productCode, userCode, Attribute.PRI_UUID);
		if (id == null) {
			throw new ItemNotFoundException(productCode, userCode, Attribute.PRI_UUID);
		}
        String uuid = id.getValueString().toLowerCase();

        String json = "{\"" + field + "\":\"" + value + "\"}";
        String uri = GennySettings.keycloakUrl() + "/admin/realms/" + realm + "/users/" + uuid;
        HttpResponse<String> response = HttpUtils.put(uri, json, serviceToken);

        return response.statusCode();
    }

    /**
     * Update a keycloak user email.
     *
     * @param userToken the userToken to update with
     * @param user      the user to update for
     * @param email     the email to set
     * @return int statusCode
     */
    public int updateUserEmail(BaseEntity user, String email) {

        String realm = serviceToken.getKeycloakRealm();
		String productCode = user.getRealm();
		String userCode = user.getCode();

		EntityAttribute id = beaUtils.getEntityAttribute(productCode, userCode, Attribute.PRI_UUID);
		if (id == null) {
			throw new ItemNotFoundException(productCode, userCode, Attribute.PRI_UUID);
		}
        String uuid = id.getValueString().toLowerCase();

        String json = "{ \"email\" : \"" + email + "\" , \"enabled\" : true, \"emailVerified\" : true}";
        String uri = GennySettings.keycloakUrl() + "/admin/realms/" + realm + "/users/" + uuid;
        HttpResponse<String> response = HttpUtils.put(uri, json, serviceToken);

        return response.statusCode();
    }

}
