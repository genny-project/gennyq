package life.genny.shleemy.live.data;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.annotations.Blocking;

import life.genny.qwandaq.message.QScheduleMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.serviceq.Service;
import life.genny.serviceq.intf.GennyScopeInit;
import life.genny.shleemy.quartz.TaskBean;

@ApplicationScoped
public class InternalConsumer {

	static final Logger log = Logger.getLogger(InternalConsumer.class);

    static Jsonb jsonb = JsonbBuilder.create();

	@Inject
	Service service;

	@Inject
	GennyScopeInit scope;

	@Inject
	UserToken userToken;

	@Inject
	TaskBean taskBean;

    void onStart(@Observes StartupEvent event) {

		service.showConfiguration();

		service.initToken();
		service.initKafka();
		log.info("[*] Finished Startup!");
    }

	@Incoming("schedule")
	@Blocking
	public void getSchedule(String data) {

		scope.init(data);

		log.info("Received incoming Schedule Message... ");
		log.debug(data);

		QScheduleMessage msg = jsonb.fromJson(data, QScheduleMessage.class);

		try {
			taskBean.addSchedule(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}

		scope.destroy();
	}
}
