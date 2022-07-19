package life.genny.messages.live.data;

import org.jboss.logging.Logger;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.apache.commons.lang3.exception.ExceptionUtils;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.ShutdownEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;

import java.util.List;

import life.genny.messages.process.MessageProcessor;
import life.genny.qwandaq.message.QCommsMessage;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.serviceq.intf.GennyScopeInit;
import life.genny.serviceq.Service;

@ApplicationScoped
public class InternalConsumer {

	private static final Logger log = Logger.getLogger(InternalConsumer.class);

	private static final ManagedExecutor executor = ManagedExecutor.builder()
	.propagated(ThreadContext.CDI)
	.build();

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	GennyScopeInit scope;

	@Inject
	Service service;

	@Inject
	MessageProcessor mp;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	UserToken userToken;

    void onStart(@Observes StartupEvent ev) {
		service.fullServiceInit();
    }

    void onStop(@Observes ShutdownEvent ev) {
		List<Runnable> unexecutedTasks = executor.shutdownNow();
        log.info("The application is stopping...");
    }

	@Incoming("messages")
	public void getFromMessages(String data) {


		log.info("Received EVENT :" + (System.getenv("PROJECT_REALM") == null ? "tokenRealm" : System.getenv("PROJECT_REALM")));

		log.info("################################################################");
		log.info(">>>>>>>>>>>>>>>>>> PROCESSING NEW MESSAGE <<<<<<<<<<<<<<<<<<<<<<");
		log.info("################################################################");

		// Log entire data for debugging purposes
		log.info("data ----> " + data);

		
		//executor.runAsync(() -> {
			scope.init(data);
			if (userToken == null) {
				log.error("UserToken is null!");
				return;
			}
			QCommsMessage message = null;

			// Try Catch to stop consumer from dying upon error
			try {
				log.info("Deserialising Message");
				message = jsonb.fromJson(data, QCommsMessage.class);
			} catch (Exception e) {
				log.error(ANSIColour.RED+"Message Deserialisation Failed!!!!!"+ANSIColour.RESET);
				log.error(ANSIColour.RED+ExceptionUtils.getStackTrace(e)+ANSIColour.RESET);
			}

			// Try Catch to stop consumer from dying upon error
			try {
				log.info("Processing Message");
				mp.processGenericMessage(message);
			} catch (Exception e) {
				log.error(ANSIColour.RED+"Message Processing Failed!!!!!"+ANSIColour.RESET);
				log.error(ANSIColour.RED+ExceptionUtils.getStackTrace(e)+ANSIColour.RESET);
			}
			scope.destroy();
		//});

	}
}
