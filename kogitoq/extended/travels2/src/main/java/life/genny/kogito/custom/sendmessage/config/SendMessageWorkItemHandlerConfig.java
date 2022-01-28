package life.genny.kogito.custom.sendmessage.config;

import javax.enterprise.context.ApplicationScoped;

import org.kie.kogito.process.impl.DefaultWorkItemHandlerConfig;

@ApplicationScoped
public class SendMessageWorkItemHandlerConfig extends DefaultWorkItemHandlerConfig {
    {
        register("SendMessage", new SendMessageWorkItemHandler());
    }
}
