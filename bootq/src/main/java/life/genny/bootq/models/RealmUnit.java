package life.genny.bootq.models;

import java.util.List;
import java.util.Map;

public class RealmUnit extends GennySheet {

    private Module module;
    private String urlList;
    private String clientSecret;
    private String keycloakUrl;
    private Boolean disable;
    private Boolean skipGoogleDoc;
    private String securityKey;
    private String servicePassword;

	private List<ModuleUnit> modules;

    public RealmUnit(Map<String, String> map) {
		super(map);
		this.disable = "TRUE".equalsIgnoreCase(map.get("disable"));
		this.skipGoogleDoc = "TRUE".equalsIgnoreCase(map.get("skipGoogleDoc"));
		this.keycloakUrl = map.get("keycloakUrl");
		this.clientSecret = map.get("clientSecret");
		this.urlList = map.get("urlList");
		this.securityKey = map.get("ENV_SECURITY_KEY");
		this.servicePassword = map.get("ENV_SERVICE_PASSWORD");
    }

    public String getUrlList() {
        return urlList;
    }

    public void setUrlList(String urlList) {
        this.urlList = urlList;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getKeycloakUrl() {
        return keycloakUrl;
    }

    public void setKeycloakUrl(String keycloakUrl) {
        this.keycloakUrl = keycloakUrl;
    }

    public Boolean getDisable() {
        return disable;
    }

    public void setDisable(Boolean disable) {
        this.disable = disable;
    }

    public Boolean getSkipGoogleDoc() {
        return skipGoogleDoc;
    }

    public void setSkipGoogleDoc(Boolean skipGoogleDoc) {
        this.skipGoogleDoc = skipGoogleDoc;
    }

    public String getSecurityKey() {
        return securityKey;
    }

    public void setSecurityKey(String securityKey) {
        this.securityKey = securityKey;
    }

    public String getServicePassword() {
        return servicePassword;
    }

    public void setServicePassword(String servicePassword) {
        this.servicePassword = servicePassword;
    }

	public List<ModuleUnit> getModules() {
		return modules;
	}

	public void setModules(List<ModuleUnit> modules) {
		this.modules = modules;
	}

}
