package life.genny.qwandaq.security.keycloak.client;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import life.genny.qwandaq.security.keycloak.*;

/**
 * KeycloakHttpClient --- Resteasy client to fetch public certificates 
 * from Keycloak server by realm.
 * E.g, http://keycloak.genny.life/auth/realms/master/protocol/openid-connect/certs
 *
 * @author    hello@gada.io
 */
@RegisterRestClient
public interface KeycloakHttpClient {

    /**
     * Fetch an array of keys provided by keycloak. If call is 
     * successful a CertResponse object will be returned
     *
     * @param realm The realm of interested to obtain public key
     *
     * @return CertResponse
     */
    @GET
    @Path("/realms/{realm}/protocol/openid-connect/certs")
    public CertsResponse fetchRealmCerts(@PathParam String realm);

}
