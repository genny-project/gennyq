package life.genny.qwandaq.utils;

import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.exception.runtime.KeycloakException;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.models.ServiceToken;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.net.ssl.HttpsURLConnection;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    public static GennyToken getGennyToken(String keycloakUrl, String realm, String clientId, String secret, String username, String password) {

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
    public static String getToken(String keycloakUrl, String realm, String clientId, String secret, String username, String password) {

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
    public static GennyToken getToken(String keycloakUrl, String realm, String clientId, String secret, String refreshToken) {

        HashMap<String, String> params = new HashMap<>();

        params.put("refresh_token", refreshToken);
        params.put("grant_type", "refresh_token");
        params.put("client_id", clientId);

        if (!StringUtils.isBlank(secret)) {
            params.put("client_secret", secret);
        }

        String token = fetchOIDCToken(keycloakUrl, realm, params);

        return new GennyToken(token);
    }

    /**
     * Fetch an Impersonated Token for a user.
     *
     * @param userCode   the userBE to get a token for
     * @param gennyToken the gennyToken to use to fetch the token
     * @return String
     */
    public String getImpersonatedToken(String userCode, GennyToken gennyToken) {

        String realm = gennyToken.getRealm();
        String token = gennyToken.getToken();
        String keycloakUrl = gennyToken.getKeycloakUrl();

        if (userCode == null) {
            log.error(ANSIColour.doColour("User code is NULL", ANSIColour.RED));
            return null;
        }

        // grab uuid to fetch token
        String uuid = beaUtils.getEntityAttribute(realm, userCode, "PRI_UUID").getValueString();

        if (uuid == null) {

            log.warn(ANSIColour.doColour("No PRI_UUID found for user " + userCode + ", attempting to use PRI_EMAIL instead", ANSIColour.YELLOW));

            // grab email to fetch token
            String email = beaUtils.getEntityAttribute(realm, userCode, "PRI_EMAIL").getValueString();

            if (email == null) {
                log.error(ANSIColour.doColour("No PRI_EMAIL found for user " + userCode, ANSIColour.RED));
                return null;
            }

            // use email as backup
            uuid = email;
        } else {
            // use lowercase UUID
            uuid = uuid.toLowerCase();
        }

        // fetch keycloak json from project entity
        String keycloakJson = beaUtils.getEntityAttribute(realm, "PRJ_" + realm.toUpperCase(), "ENV_KEYCLOAK_JSON").getValueString();
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
                if (first) first = false;
                else result.append("&");

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
    public String createDummyUser(GennyToken token, String realm) {
        return createDummyUser(token.getToken(), realm);
    }

    /**
     * @param token
     * @param realm
     * @return
     */
    public String createDummyUser(String token, String realm) {

        String username = UUID.randomUUID().toString().substring(0, 18);
        String email = username + "@gmail.com";
        String defaultPassword = GennySettings.dummyUserPassword();

        String json = "{ " + "\"username\" : \"" + username + "\"," + "\"email\" : \"" + email + "\" , " + "\"enabled\" : true, " + "\"emailVerified\" : true, " + "\"firstName\" : \"" + username + "\", " + "\"lastName\" : \"" + username + "\", " + "\"groups\" : [" + " \"users\" " + "], " + "\"requiredActions\" : [\"terms_and_conditions\"], " + "\"realmRoles\" : [\"user\"],\"credentials\": [{" + "\"type\":\"password\"," + "\"value\":\"" + defaultPassword + "\"," + "\"temporary\":true }]}";

        log.info("Dummy User = " + json);

        String uri = GennySettings.keycloakUrl() + "/admin/realms/" + realm + "/users";

        log.info("Create keycloak user - url:" + uri + ", token:" + token);

        HttpResponse<String> response = HttpUtils.post(uri, json, serviceToken);
        String userIdUrl = response.headers().firstValue("Location").orElseThrow(() -> new KeycloakException("Failed to create user: Response is null"));

        if (userIdUrl == null) throw new KeycloakException("Failed to create user: Response is null");

        int statusCode = response.statusCode();
        Response.Status status = Response.Status.fromStatusCode(statusCode);
        log.info("Create User Response Status: " + statusCode);

        // if user already exists, return their id
        if (status == Response.Status.CONFLICT) {
            log.warn("Email already taken: " + email);
            return null;
        }

        if (Response.Status.Family.familyOf(statusCode) == Response.Status.Family.SUCCESSFUL) {
            log.info("Successfully Created User");
            String userId = userIdUrl.replace(uri + "/", "");
            log.debug("keycloak userid: " + userId);
            return userId;
        }

        throw new KeycloakException("Failed to create user: " + response.body());
    }

    /**
     * @param jsonObject
     * @param uuid
     * @param realm
     */
    public void updateUserDetails(JsonObject jsonObject, String uuid, String realm) {
        log.info(ANSIColour.doColour("Updating user details for: " + uuid, ANSIColour.GREEN));
        String json = jsonObject.toString();
        log.info(ANSIColour.doColour("request body: " + json, ANSIColour.GREEN));
        String uri = GennySettings.keycloakUrl() + "/admin/realms/" + realm + "/users/" + uuid.toLowerCase();
        log.info(ANSIColour.doColour("request url: " + uri, ANSIColour.GREEN));
        HttpResponse<String> response = HttpUtils.put(uri, json, serviceToken);
        int statusCode = response.statusCode();
        String body = response.body();
        log.info(ANSIColour.doColour("keycloak response -> statusCode: " + statusCode, ANSIColour.YELLOW));
        log.info(ANSIColour.doColour("keycloak response -> body: " + body, ANSIColour.YELLOW));
        if (Response.Status.Family.familyOf(statusCode) == Response.Status.Family.SUCCESSFUL) {
            log.info(ANSIColour.doColour("Successfully updated user details!", ANSIColour.GREEN));
        } else if (Response.Status.CONFLICT.getStatusCode() == statusCode) {
            log.error(ANSIColour.doColour("User exists with same username or email!", ANSIColour.RED));
            throw new KeycloakException("User exists with same username or email");
        } else {
            log.error(ANSIColour.doColour("Failed updating user details!", ANSIColour.RED));
            throw new KeycloakException("Failed to update user details");
        }
    }

    /**
     * @param uuid
     * @param realm
     * @param password
     * @param askUserToResetPassword
     * @return
     */
    public void updateUserTemporaryPassword(String uuid, String realm, String password, Boolean askUserToResetPassword) {
        try {
            String keycloakUrl = GennySettings.keycloakUrl();
            JsonObject requestObject = Json.createObjectBuilder().add("type", "password").add("temporary", askUserToResetPassword).add("value", password).build();

            String json = requestObject.toString();
            log.info(ANSIColour.doColour("request body: " + json, ANSIColour.GREEN));
            String requestURL = keycloakUrl + "/auth/admin/realms/" + realm + "/users/" + uuid.toLowerCase() + "/reset-password";
            log.info(ANSIColour.doColour("request url: " + requestURL, ANSIColour.GREEN));
            HttpResponse<String> response = HttpUtils.put(requestURL, json, serviceToken);
            int statusCode = response.statusCode();
            String body = response.body();
            log.info(ANSIColour.doColour("keycloak response -> statusCode: " + statusCode, ANSIColour.YELLOW));
            log.info(ANSIColour.doColour("keycloak response -> body: " + body, ANSIColour.YELLOW));
        } catch (Exception ex) {
            throw new KeycloakException("Failed to updating user password");
        }
    }

    /**
     * @param uuid
     * @param askUserToResetPassword
     * @return
     */
    public String updateUserPassword(String uuid, String realm, Boolean askUserToResetPassword) {
        /* Generate a random 15 char password */
        String newPassword = RandomStringUtils.generateRandomString(15);
        updateUserTemporaryPassword(uuid, realm, newPassword, askUserToResetPassword);
        return newPassword;
    }

    /**
     * @param uuid
     * @return
     */
    public String updateUserTemporaryPassword(String uuid, String realm) {
        return updateUserPassword(uuid, realm, true);
    }
}
