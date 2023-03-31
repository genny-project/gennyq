package life.genny.kogito.common.config;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.kie.kogito.process.impl.CachedWorkItemHandlerConfig;

import life.genny.kogito.common.workitem.MessageWorkItemHandler;

@ApplicationScoped
public class WorkItemHandlerConfig extends CachedWorkItemHandlerConfig{

    @Inject
    MessageWorkItemHandler messageWorkItemHandler;
    
    @PostConstruct
    void init () {
        register("MessageTask", messageWorkItemHandler);
    }
}
