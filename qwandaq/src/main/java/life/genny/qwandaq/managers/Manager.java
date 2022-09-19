package life.genny.qwandaq.managers;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.utils.callbacks.FILogCallback;

@ApplicationScoped
public abstract class Manager {
	private static Logger log;

	protected static Jsonb jsonb = JsonbBuilder.create();

	@Inject
	protected UserToken userToken;

	@Inject
	protected ServiceToken serviceToken;

	@Inject
	protected QwandaUtils qwandaUtils;

	@Inject
	protected BaseEntityUtils beUtils;

	@Inject
	protected DatabaseUtils dbUtils;

	@PostConstruct
    protected void init() {
        log.info("[!] Initialized " + this.className());
		log = Logger.getLogger(className());
    }
    
    protected String className() {
		String str = this.getClass().getSimpleName();
		int index = str.indexOf('_');
		if(index != -1)
			str = str.substring(0, index);
		return str;
	}
    
	public Logger getLogger() {
		return log;
	}

	public void log(Object o, FILogCallback level) {
		level.log(o);
	}

	public void info(Object o) {
		log(o, log::info);
	}

	public void debug(Object o) {
		log(o, log::debug);
	}

	public void warn(Object o) {
		log(o, log::warn);
	}

	public void error(Object o) {
		log(o, log::error);
	}
}
