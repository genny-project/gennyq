package life.genny.kogito.custom.sendmessage.config;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.kie.kogito.process.impl.DefaultWorkItemHandlerConfig;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import life.genny.qwandaq.data.GennyCache;
import life.genny.qwandaq.utils.CacheUtils;

@ApplicationScoped
public class SendMessageWorkItemHandlerConfig extends DefaultWorkItemHandlerConfig {
    private static final Logger log = Logger.getLogger(SendMessageWorkItemHandlerConfig.class);

    // @ConfigProperty(name = "quarkus.datasource.jdbc.url")
    // String jdbc_url;

    // @ConfigProperty(name = "quarkus.datasource.username")
    // String jdbc_username;

    // @ConfigProperty(name = "quarkus.datasource.password")
    // String jdbc_password;

    @Inject
    GennyCache cache;

    // @Inject
    // EntityManager entityManager;

    // public SendMessageWorkItemHandlerConfig() {
    // if (entityManager == null) {
    // log.error("entityManager is NULL!");
    // log.error("jdbc_url=" + jdbc_url);
    // log.error("jdbc_username=" + jdbc_username);
    // log.error("jdbc_password=" + jdbc_password);

    // }
    // DatabaseUtils.init(entityManager);

    // GennyToken serviceToken = new
    // KeycloakUtils().getToken("https://keycloak.gada.io", "internmatch",
    // "admin-cli",
    // null,
    // "service", System.getenv("GENNY_SERVICE_PASSWORD"), null);
    // System.out.println("ServiceToken = " + serviceToken.getToken());
    // BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken, serviceToken);

    // QwandaUtils.init(serviceToken);
    // DefUtils.init(beUtils);
    {
        // GennyCache cache = new GennyCache();
        if (cache == null) {
            log.error("GennyCache cache is NULL!!");
        } else {
            CacheUtils.init(cache);
        }
        register("SendMessage", new SendMessageWorkItemHandler());
        log.info("Registered SendMessageWorkItemHandler");
    }

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        log.info("Kogito SendMessageWorkItemHandlerConfig starting");
        // CacheUtils.init(cache);
        // register("SendMessage", new SendMessageWorkItemHandler());
        log.info("Registered SendMessageWorkItemHandler");
    }

    @Transactional
    void onShutdown(@Observes ShutdownEvent ev) {
        log.info("Kogito SendMessageWorkItemHandlerConfig Shutting down");

    }

}
