package life.genny.kogito.custom.sendmessage.config;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.transaction.Transactional;

import org.jboss.logging.Logger;
import org.kie.kogito.process.impl.DefaultWorkItemHandlerConfig;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class SendMessageWorkItemHandlerConfig extends DefaultWorkItemHandlerConfig {
    private static final Logger log = Logger.getLogger(SendMessageWorkItemHandlerConfig.class);

    public SendMessageWorkItemHandlerConfig() {
        register("SendMessage", new SendMessageWorkItemHandler());
        log.info("Registered SendMessageWorkItemHandler");
    }

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        log.info("Kogito SendMessageWorkItemHandlerConfig starting");
        register("SendMessage", new SendMessageWorkItemHandler());
        log.info("Registered SendMessageWorkItemHandler");
    }

    @Transactional
    void onShutdown(@Observes ShutdownEvent ev) {
        log.info("Kogito SendMessageWorkItemHandlerConfig Shutting down");

    }

}
