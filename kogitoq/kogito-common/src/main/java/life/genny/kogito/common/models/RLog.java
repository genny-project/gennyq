package life.genny.kogito.common.models;

import org.jboss.logging.Logger;
import org.kie.api.runtime.rule.RuleContext;

public final class RLog {

	private RLog() { }

	protected static Logger getLogger(final RuleContext drools) {

		final String pkg = drools.getRule().getPackageName() + "." + drools.getRule().getName();
		final Logger logger = Logger.getLogger(pkg);

		return logger;
	}

	public static void info(final RuleContext drools, final String message) {

		final Logger logger = getLogger(drools);
		logger.info(message);
	}

	public static void debug(final RuleContext drools, final String message) {

		final Logger logger = getLogger(drools);
		logger.debug(message);
	}

	public static void error(final RuleContext drools, final String message) {

		final Logger logger = getLogger(drools);
		logger.error(message);
	}

	public static void fired(final RuleContext drools) {

		final Logger logger = getLogger(drools);
		logger.info("Rule Fired -> " + drools.getRule().getName());
	}
}

