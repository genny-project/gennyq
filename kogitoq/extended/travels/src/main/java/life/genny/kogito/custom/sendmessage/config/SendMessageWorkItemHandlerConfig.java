package life.genny.kogito.custom.sendmessage.config;

import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.jboss.logging.Logger;
import org.kie.kogito.process.impl.DefaultWorkItemHandlerConfig;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import life.genny.kogito.custom.askquestions.config.AskQuestionsWorkItemHandler;
import life.genny.serviceq.Service;

@ApplicationScoped
public class SendMessageWorkItemHandlerConfig extends DefaultWorkItemHandlerConfig {

    private static final Logger log = Logger.getLogger(SendMessageWorkItemHandlerConfig.class);

    @Inject
    Service service;

    public SendMessageWorkItemHandlerConfig() {
        super();
        register("SendMessage", new SendMessageWorkItemHandler());
        register("AskQuestions", new AskQuestionsWorkItemHandler());
        log.info("Registered SendMessageWorkItemHandler");
    }

    @Transactional
    void onStart(@Observes StartupEvent ev) {

        service.fullServiceInit();

        log.info("Kogito SendMessageWorkItemHandlerConfig starting");
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
