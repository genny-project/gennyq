package life.genny.lauchy.live.data;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import life.genny.lauchy.Validator;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.SecurityUtils;
import life.genny.serviceq.Service;
import life.genny.serviceq.intf.GennyScopeInit;

@ApplicationScoped
public class InternalConsumer {

    @Inject
    Logger log;

	@Inject
	GennyScopeInit scope;
    
	@Inject
	Service service;
    
	@ConfigProperty(name = "genny.enable.blacklist", defaultValue = "true")
	Boolean enableBlacklist;

    @Inject
    Validator validator;

	void onStart(@Observes StartupEvent ev) {

		if (service.showValues()) {
			log.info("Blacklist        :" + (enableBlacklist ? "ON" : "OFF"));
		}
		service.fullServiceInit();
		log.info("[*] Finished Lauchy Startup!");
	}
    
	@Incoming("data")
	public void filter(String data) {
        scope.init(data);
        log.info("Received Message: ".concat(SecurityUtils.obfuscate(data)));

        if(!validator.validateData(data)) {
            log.error("Received message not valid!");
            return;
        }

        data = validator.handleDependentDropdowns(data);

        log.info("Forwarding valid message");
        KafkaUtils.writeMsg(KafkaTopic.VALID_DATA, data);
	}

}
