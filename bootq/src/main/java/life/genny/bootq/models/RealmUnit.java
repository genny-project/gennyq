package life.genny.bootq.models;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;

import com.google.common.collect.Maps;

public class RealmUnit extends DataUnit {
    private final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    private String code;
    private String name;
    private Module module;
    private String urlList;
    private String clientSecret;
    private String keycloakUrl;
    private Boolean disable;
    private Boolean skipGoogleDoc;
    private String securityKey;
    private String servicePassword;
    private String uri;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getServicePassword() {
        return servicePassword;
    }

    public void setServicePassword(String servicePassword) {
        this.servicePassword = servicePassword;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String name) {
        this.code = name;
    }


    public Module getModule() {
        return module;
    }

    public void setModule(Module module) {
        this.module = module;
    }

    private BinaryOperator<HashMap<String, Map<String, String>>> overrideByPrecedence
            = (weakModule, strongModule) -> {
        strongModule.entrySet().forEach(data -> {
            if (weakModule.containsKey(data.getKey())) {
//                log.warn("For Module Name: " + code + ", Key:" + data.getKey() + " This will be overrided ");
//                System.out.println("For Module Name: " + code);
//                System.out.println(data.getKey() + " This will be overrided ");
            }
        });
        weakModule.putAll(strongModule);
        return weakModule;
    };


    public RealmUnit(Map<String, String> realm) {
        Optional<String> disabelStr = Optional.ofNullable(realm.get("disable"));
        Boolean disableProject = disabelStr.map(Boolean::valueOf).orElse(false);
        Optional<String> skipGoogleDocStr = Optional.ofNullable(realm.get("skipGoogleDoc".toLowerCase().replaceAll("^\"|\"$|_|-", "")));
        boolean skipgoogledoc = skipGoogleDocStr.map(Boolean::valueOf).orElse(false);

        setKeycloakUrl(realm.get("keycloakUrl".toLowerCase().replaceAll("^\"|\"$|_|-", "")));
        setClientSecret(realm.get("clientSecret".toLowerCase().replaceAll("^\"|\"$|_|-", "")));
        setCode(realm.get("code".toLowerCase().replaceAll("^\"|\"$|_|-", "")));
        setName(realm.get("name".toLowerCase().replaceAll("^\"|\"$|_|-", "")));
        setUrlList(realm.get("urlList".toLowerCase().replaceAll("^\"|\"$|_|-", "")));
        setDisable(disableProject);
        setUri(realm.get("sheetID".toLowerCase()));
        setSkipGoogleDoc(skipgoogledoc);
        setSecurityKey(realm.get("ENV_SECURITY_KEY".toLowerCase().replaceAll("^\"|\"$|_|-", "")));
        setServicePassword(realm.get("ENV_SERVICE_PASSWORD".toLowerCase().replaceAll("^\"|\"$|_|-", "")));

        if (skipgoogledoc) {
            log.info("Skipping google doc for realm " + this.name);
        } else {
            module = new Module(realm.get("sheetID".toLowerCase()));
            Optional<HashMap<String, Map<String, String>>> tmpOptional = module.getDataUnits().stream()
                    .map(moduleUnit -> Maps.newHashMap(moduleUnit.baseEntitys))
                    .reduce(overrideByPrecedence);

            tmpOptional.ifPresent(stringMapHashMap -> super.baseEntitys = stringMapHashMap);

            tmpOptional = module.getDataUnits().stream()
                    .map(moduleUnit -> Maps.newHashMap(moduleUnit.attributes))
                    .reduce(overrideByPrecedence);
            tmpOptional.ifPresent(stringMapHashMap -> super.attributes = stringMapHashMap);

            tmpOptional = module.getDataUnits().stream()
                    .map(moduleUnit -> Maps.newHashMap(moduleUnit.def_baseEntitys))
                    .reduce(overrideByPrecedence);
            tmpOptional.ifPresent(stringMapHashMap -> super.def_baseEntitys= stringMapHashMap);

            tmpOptional = module.getDataUnits().stream()
                    .map(moduleUnit -> Maps.newHashMap(moduleUnit.def_entityAttributes))
                    .reduce(overrideByPrecedence);
            tmpOptional.ifPresent(stringMapHashMap -> super.def_entityAttributes= stringMapHashMap);

            tmpOptional = module.getDataUnits().stream()
                    .map(mm -> Maps.newHashMap(mm.attributeLinks))
                    .reduce(overrideByPrecedence);
            tmpOptional.ifPresent(stringMapHashMap -> super.attributeLinks = stringMapHashMap);


            tmpOptional = module.getDataUnits().stream()
                    .map(mm -> Maps.newHashMap(mm.notifications))
                    .reduce(overrideByPrecedence);
            tmpOptional.ifPresent(stringMapHashMap -> super.notifications = stringMapHashMap);

            tmpOptional = module.getDataUnits().stream()
                    .map(mm -> Maps.newHashMap(mm.entityEntitys))
                    .reduce(overrideByPrecedence);
            tmpOptional.ifPresent(stringMapHashMap -> super.entityEntitys = stringMapHashMap);

            tmpOptional = module.getDataUnits().stream()
                    .map(mm -> Maps.newHashMap(mm.questions))
                    .reduce(overrideByPrecedence);
            tmpOptional.ifPresent(stringMapHashMap -> super.questions = stringMapHashMap);

            tmpOptional = module.getDataUnits().stream()
                    .map(mm -> Maps.newHashMap(mm.entityAttributes))
                    .reduce(overrideByPrecedence);
            tmpOptional.ifPresent(stringMapHashMap -> super.entityAttributes = stringMapHashMap);

            tmpOptional = module.getDataUnits().stream()
                    .map(mm -> Maps.newHashMap(mm.asks))
                    .reduce(overrideByPrecedence);
            tmpOptional.ifPresent(stringMapHashMap -> super.asks = stringMapHashMap);

            tmpOptional = module.getDataUnits().stream()
                    .map(mm -> Maps.newHashMap(mm.questionQuestions))
                    .reduce(overrideByPrecedence);
            tmpOptional.ifPresent(stringMapHashMap -> super.questionQuestions = stringMapHashMap);

            tmpOptional = module.getDataUnits().stream()
                    .map(mm -> Maps.newHashMap(mm.validations))
                    .reduce(overrideByPrecedence);
            tmpOptional.ifPresent(stringMapHashMap -> super.validations = stringMapHashMap);

            tmpOptional = module.getDataUnits().stream()
                    .map(mm -> Maps.newHashMap(mm.dataTypes))
                    .reduce(overrideByPrecedence);
            tmpOptional.ifPresent(stringMapHashMap -> super.dataTypes = stringMapHashMap);

            tmpOptional = module.getDataUnits().stream()
                    .map(mm -> Maps.newHashMap(mm.messages))
                    .reduce(overrideByPrecedence);
            tmpOptional.ifPresent(stringMapHashMap -> super.messages = stringMapHashMap);
        }
    }


    public void clearAll() {
        asks.clear();
        baseEntitys.clear();
        entityAttributes.clear();
        attributeLinks.clear();
        attributes.clear();
        dataTypes.clear();
        messages.clear();
        notifications.clear();
        questionQuestions.clear();
        questions.clear();
        validations.clear();
    }

}
