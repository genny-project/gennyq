package life.genny.qwandaq.security.keycloak;

import io.vertx.core.json.JsonObject;
import life.genny.qwandaq.utils.HttpUtils;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jose4j.base64url.Base64;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * KeycloakTokenPayload --- The bearer token will be base64 encoded with a header body and signature
 * and then joined by a dot '.'
 *
 * <p>This class will decode the body or extract the claims from a {@link JsonWebToken}
 *
 * @author hello@gada.io
 */
public class KeycloakTokenPayload {

  public String sub;
  public String iss;
  public String typ;
  public String preferredUsername;
  public String locale;
  public String sid;
  public String acr;
  public Set<String> roles;
  public String azp;
  public String scope;
  public Long exp;
  public String sessionState;
  public Long iat;
  public String jti;
  public String email;
  public Boolean emailVerified;
  public String groups;
  public String givenName;
  public String aud;
  public String name;
  public String familyName;
  public String realm;
  public String token;

  public KeycloakTokenPayload(JsonWebToken payload) {

    this.sub = payload.getClaim("sub");
    setIss(payload.getClaim("iss"));
    this.typ = payload.getClaim("typ");
    this.preferredUsername = payload.getClaim("preferred_username");
    this.locale = payload.getClaim("locale");
    this.sid = payload.getClaim("sid");
    this.acr = payload.getClaim("acr");
    this.roles =
            new JsonObject(payload.getClaim("realm_access").toString())
                    .getJsonArray("roles").stream().map(Object::toString).collect(Collectors.toSet());

    this.azp = payload.getClaim("azp");
    this.scope = payload.getClaim("scope");
    this.exp = payload.getClaim("exp");
    this.sessionState = payload.getClaim("session_state");
    this.iat = payload.getClaim("iat");
    this.jti = payload.getClaim("jti");
    this.email = payload.getClaim("email");
    this.emailVerified = payload.getClaim("email_verified");
    this.givenName = payload.getClaim("given_name");
    this.aud = payload.getClaim("aud").toString();
    this.name = payload.getClaim("name");
    this.familyName = payload.getClaim("family_name");
    this.token = payload.getRawToken();
  }

  public KeycloakTokenPayload() {}

  /**
   * This static method will return an object of this class type {@link KeycloakTokenPayload}. It is
   * recommended to uses this static method to parse the JWT token to a POJO providing more type
   * safety.
   *
   * @param token Base64 encoded bearer token
   * @return {@link KeycloakTokenPayload}
   */
  public static KeycloakTokenPayload decodeToken(String token) {
    /* Below declaration checks If it is from Auth headers then
    extract the token otherwise return same token */
    token = HttpUtils.extractTokenFromHeaders(token);
    KeycloakTokenPayload keycloakTokenPayload = new KeycloakTokenPayload();
    String payload = new String(Base64.decode(token.split("\\.")[1]));
    JsonObject jsonPayload = new JsonObject(payload);
    keycloakTokenPayload.setIss(jsonPayload.getString("iss"));
    keycloakTokenPayload.sessionState = jsonPayload.getString("session_state");
    keycloakTokenPayload.token = token;
    keycloakTokenPayload.sid = jsonPayload.getString("sid");
    keycloakTokenPayload.sub = jsonPayload.getString("sub");
    keycloakTokenPayload.jti = jsonPayload.getString("jti");
    return keycloakTokenPayload;
  }

  void setIss(String iss) {
    this.iss = iss;
    String[] deconstructBySlash = iss.split("/");
    this.realm = deconstructBySlash[deconstructBySlash.length - 1];
  }
}
