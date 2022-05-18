package life.genny.bridge.model;

import life.genny.qwandaq.utils.CommonUtils;

import java.util.Arrays;
import java.util.Optional;

import javax.json.bind.annotation.JsonbProperty;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.bridge.exception.BridgeException;
import life.genny.bridge.exception.ClientIdException;

/**
 * InitProperties --- The class contains all the fields neccessary to contruct the protocol external clients
 * will use. The information pass to external client will tell paths for saving media files, google map keys,
 * the keycloak server it needs to use and which is trusted in the backends etcs.
 *
 * @author    hello@gada.io
 */
@RegisterForReflection
public class InitProperties {

    @JsonbProperty
    String realm;
    @JsonbProperty("ENV_KEYCLOAK_REDIRECTURI")
    String keycloakRedirectUri;
    @JsonbProperty("ENV_MEDIA_PROXY_URL")
    String mediaProxyUrl;
    @JsonbProperty("api_url")
    String apiUrl;
    @JsonbProperty
    String clientId;

    public InitProperties(String url) throws BridgeException {

        this();
        setMediaProxyUrl(url);
        setApiUrl(url);

		String cid = determineClientId(url);
		if ("internmatch".equals(cid)) {
			cid = "alyson";
		}

		setClientId(cid);
    }

    public InitProperties() throws BridgeException {

        setRealm("internmatch");
        setKeycloakRedirectUri(CommonUtils.getSystemEnv("ENV_KEYCLOAK_REDIRECTURI"));
    }

	public String getRealm() {
		return realm;
	}

    public void setRealm(String realm) throws BridgeException {
        this.realm = throwIfNull(realm,"realm");
    }

	public String getKeycloakRedirectUri() {
		return keycloakRedirectUri;
	}

    public void setKeycloakRedirectUri(String keycloakRedirectUri) throws BridgeException {
        this.keycloakRedirectUri = throwIfNull(keycloakRedirectUri, "ENV_KEYCLOAK_REDIRECTURI");
    }

	public String getMediaProxyUrl() {
		return mediaProxyUrl;
	}

    public void setMediaProxyUrl(String url) {
        this.mediaProxyUrl = url + "web/public";
    }

	public String getApiUrl() {
		return apiUrl;
	}

    public void setApiUrl(String url) {
        this.apiUrl = url;
    }

	public void setClientId(String clientId) throws BridgeException {
		this.clientId = throwIfNull(clientId, "clientId");
	}

    public String getClientId() {
        return clientId;
    }

    /**
     * It will throw a BridgeException error if the required field is null or empty
     *
     * @param val A value of the global field 
     * @param fieldName Name the global field 
     *
     * @return A non empty or null value
     *
     * @throws BridgeException An error if the field is null or empty along with a NullPointerException
     */
    public String throwIfNull(String val,String fieldName) throws BridgeException {

        return Optional.ofNullable(val).orElseThrow(
                () -> new BridgeException(BridgeException.NULL_FIELD, "The value {"+fieldName+"} is compulsory "
                                          + " for the InitProperties class in order to provide the necessary information"
                                          + " to the requested client. This happens when a call is "
                                          + "made to the /api/events/init but the initProperties "
                                          + "do not contain the value",new NullPointerException("Null Field: " + fieldName)));
    }

    /**
     * Determine the client id from the url and the list of PRODUCT_CODES
     * in the envs
     * @param uri Uri to parse the client id from
     * @return a client id from one of the existing PRODUCT_CODES
     * 
     * @throws ClientIdException if the client id cannot be properly parsed from the uri, usually because none of the product
     * codes match the URI
     */
    private String determineClientId(String uri) throws BridgeException {
        String productCodeArray = CommonUtils.getSystemEnv("PRODUCT_CODES");
        throwIfNull(productCodeArray, "PRODUCT_CODES");

        String[] productCodes = productCodeArray.split(":");

        for(String productCode : productCodes) {
            if(uri.contains(productCode)) {
                return productCode;
            }
        }

        if(clientId == null) {
            throw new ClientIdException("Could not determine product code for uri: " + uri, new IllegalArgumentException("bad uri: " + uri));
        }

        return clientId;
    }
}
