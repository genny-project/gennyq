package life.genny.qwandaq.security.keycloak;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * CertsResponse --- The keys associated with a realm 
 * @author    hello@gada.io
 *
 */
@RegisterForReflection
public class CertsResponse {

    public List<KeycloakRealmKey> keys;

	public CertsResponse() { }

	public void setKeys(List<KeycloakRealmKey> keys) {
		this.keys = keys;
	}

	public List<KeycloakRealmKey> getKeys() {
		return keys;
	}

}
