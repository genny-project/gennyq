package life.genny.kogito.custom.sendmessage.config;

import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.kie.kogito.process.impl.DefaultWorkItemHandlerConfig;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import life.genny.kogito.custom.askquestions.config.AskQuestionsWorkItemHandler;
import life.genny.kogito.intf.KafkaBean;
import life.genny.qwandaq.data.GennyCache;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.KeycloakUtils;

@ApplicationScoped
public class SendMessageWorkItemHandlerConfig extends DefaultWorkItemHandlerConfig {
    private static final Logger log = Logger.getLogger(SendMessageWorkItemHandlerConfig.class);

    public SendMessageWorkItemHandlerConfig() {
        super();
        register("SendMessage", new SendMessageWorkItemHandler());
        register("AskQuestions", new AskQuestionsWorkItemHandler());
        log.info("Registered SendMessageWorkItemHandler");
    }

    @ConfigProperty(name = "genny.keycloak.url", defaultValue = "https://keycloak.gada.io")
    String baseKeycloakUrl;

    @ConfigProperty(name = "genny.keycloak.realm", defaultValue = "genny")
    String keycloakRealm;

    @ConfigProperty(name = "genny.service.username", defaultValue = "service")
    String serviceUsername;

    @ConfigProperty(name = "genny.service.password", defaultValue = "password")
    String servicePassword;

    @ConfigProperty(name = "genny.oidc.client-id", defaultValue = "backend")
    String clientId;

    @ConfigProperty(name = "genny.oidc.credentials.secret", defaultValue = "secret")
    String secret;

    @Inject
    EntityManager entityManager;

    @Inject
    GennyCache cache;

    @Inject
    KafkaBean kafkaBean;

    GennyToken serviceToken;

    BaseEntityUtils beUtils;

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        log.info("Kogito SendMessageWorkItemHandlerConfig starting");
        if (cache == null) {
            log.error("GennyCache cache is still NULL!!");
        } else {
            log.info("GennyCache cache is NOT NULL!!");

            GennyToken serviceToken = KeycloakUtils.getToken("https://keycloak.gada.io", "internmatch",
                    "admin-cli", null,
                    "service", System.getenv("GENNY_SERVICE_PASSWORD"));
            log.info("ServiceToken = " + serviceToken.getToken());
            BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken);
            if (entityManager != null) {
                log.info("entityManager is NOT NULL!!");
                CacheUtils.init(cache);
                DatabaseUtils.init(entityManager);

                if (kafkaBean != null) {
                    log.info("KafkaBean is NOT NULL!!");
                    KafkaUtils.init(kafkaBean);
                    // QwandaUtils.init(serviceToken); // This has database class issues
                }
            }
        }
        // register("SendMessage", new SendMessageWorkItemHandler());

        Collection<String> names = this.names();
        for (String name : names) {
            log.info("Registered ---> " + name);
        }

    }

    @Transactional
    void onShutdown(@Observes ShutdownEvent ev) {
        log.info("Kogito SendMessageWorkItemHandlerConfig Shutting down");

    }
}
