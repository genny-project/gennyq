package life.genny.messages.live.data;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import life.genny.messages.process.MessageProcessor;
import life.genny.qwandaq.message.QMessageGennyMSG;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.serviceq.Service;
import life.genny.serviceq.intf.GennyScopeInit;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbException;

@ApplicationScoped
public class InternalConsumer {

    @Inject
    Logger log;

    Jsonb jsonb = JsonbBuilder.create();

    @Inject
    GennyScopeInit scope;

    @Inject
    Service service;

    @Inject
    MessageProcessor messageProcessor;

    void onStart(@Observes StartupEvent ev) {
        service.fullServiceInit();
    }

    void onStop(@Observes ShutdownEvent ev) {
        log.info("The application is stopping...");
    }

    @Incoming("messages")
    public void getFromMessages(String data) {
        log.info(ANSIColour.doColour("################################################################", ANSIColour.GREEN));
        log.info(ANSIColour.doColour(">>>>>>>>>>>>>>>>>> PROCESSING NEW MESSAGE <<<<<<<<<<<<<<<<<<<<<<", ANSIColour.GREEN));
        log.info(ANSIColour.doColour("################################################################", ANSIColour.GREEN));

        // Log entire data for debugging purposes
        scope.init(data);
        QMessageGennyMSG message = null;

        // Try Catch to stop consumer from dying upon error
        try {
            log.info("Deserializing Message");
            message = jsonb.fromJson(data, QMessageGennyMSG.class);
            log.error(ANSIColour.doColour("messageCode: " + message.getTemplateCode(), ANSIColour.GREEN));
        } catch (JsonbException e) {
            log.error(ANSIColour.doColour("Message deserialization Failed!", ANSIColour.RED));
            log.error(ANSIColour.doColour(ExceptionUtils.getStackTrace(e), ANSIColour.RED));
            scope.destroy();
            return;
        }
        messageProcessor.processGenericMessage(message);
        scope.destroy();
    }
}
