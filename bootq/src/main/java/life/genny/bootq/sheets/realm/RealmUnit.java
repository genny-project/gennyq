package life.genny.bootq.sheets.realm;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;

import com.google.common.collect.Maps;

import life.genny.bootq.sheets.DataUnit;
import life.genny.bootq.sheets.module.GennyModule;

public class RealmUnit extends DataUnit {
    private final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    private String code;
    private GennyModule module;
    private String urlList;
    private String clientSecret;
    private String keycloakUrl;
    private Boolean disable;
    private Boolean skipGoogleDoc;
    private String securityKey;
    private String servicePassword;
    private String uri;

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


    public GennyModule getModule() {
        return module;
    }

    public void setModule(GennyModule module) {
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
            module = new GennyModule(realm.get("sheetID".toLowerCase()));
            Optional<HashMap<String, Map<String, String>>> tmpOptional = module.getDataUnits().stream()
                    .map(moduleUnit -> Maps.newHashMap(moduleUnit.getBaseEntitys()))
                    .reduce(overrideByPrecedence);

            tmpOptional.ifPresent(stringMapHashMap -> super.baseEntitys = stringMapHashMap);

            tmpOptional = module.getDataUnits().stream()
                    .map(moduleUnit -> Maps.newHashMap(moduleUnit.getAttributes()))
                    .reduce(overrideByPrecedence);
            tmpOptional.ifPresent(stringMapHashMap -> super.attributes = stringMapHashMap);

            tmpOptional = module.getDataUnits().stream()
                    .map(moduleUnit -> Maps.newHashMap(moduleUnit.getDef_baseEntitys()))
                    .reduce(overrideByPrecedence);
            tmpOptional.ifPresent(stringMapHashMap -> super.def_baseEntitys= stringMapHashMap);

            tmpOptional = module.getDataUnits().stream()
                    .map(moduleUnit -> Maps.newHashMap(moduleUnit.getDef_entityAttributes()))
                    .reduce(overrideByPrecedence);
            tmpOptional.ifPresent(stringMapHashMap -> super.def_entityAttributes= stringMapHashMap);

            tmpOptional = module.getDataUnits().stream()
                    .map(mm -> Maps.newHashMap(mm.getQuestions()))
                    .reduce(overrideByPrecedence);
            tmpOptional.ifPresent(stringMapHashMap -> super.questions = stringMapHashMap);

            tmpOptional = module.getDataUnits().stream()
                    .map(mm -> Maps.newHashMap(mm.getEntityAttributes()))
                    .reduce(overrideByPrecedence);
            tmpOptional.ifPresent(stringMapHashMap -> super.entityAttributes = stringMapHashMap);

            tmpOptional = module.getDataUnits().stream()
                    .map(mm -> Maps.newHashMap(mm.getQuestionQuestions()))
                    .reduce(overrideByPrecedence);
            tmpOptional.ifPresent(stringMapHashMap -> super.questionQuestions = stringMapHashMap);

            tmpOptional = module.getDataUnits().stream()
                    .map(mm -> Maps.newHashMap(mm.getValidations()))
                    .reduce(overrideByPrecedence);
            tmpOptional.ifPresent(stringMapHashMap -> super.validations = stringMapHashMap);

            tmpOptional = module.getDataUnits().stream()
                    .map(mm -> Maps.newHashMap(mm.getDataTypes()))
                    .reduce(overrideByPrecedence);
            tmpOptional.ifPresent(stringMapHashMap -> super.dataTypes = stringMapHashMap);

            // tmpOptional = module.getDataUnits().stream()
            //         .map(mm -> Maps.newHashMap(mm.messages))
            //         .reduce(overrideByPrecedence);
            // tmpOptional.ifPresent(stringMapHashMap -> super.messages = stringMapHashMap);
        }
    }


    public void clearAll() {
        baseEntitys.clear();
        entityAttributes.clear();
        attributes.clear();
        dataTypes.clear();
        // messages.clear();
        questionQuestions.clear();
        questions.clear();
        validations.clear();
    }

}
