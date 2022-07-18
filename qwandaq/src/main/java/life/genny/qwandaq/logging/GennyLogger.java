package life.genny.qwandaq.logging;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * A Bridge Info storage class used with BridgeSwitch to map individual user bridge Ids
 * 
 * @author Jasper Robison
 */
@RegisterForReflection
public abstract class GennyLogger extends Logger {

    protected GennyLogger(final String name) {
		super(name);
	}


    public void doLog(Level level, String loggerClassName, Object message, Object[] parameters, Throwable thrown) {

	}

    // public void doLogf(Level level, String loggerClassName, String format, Object[] parameters, Throwable thrown) {

	// }

    // public boolean isEnabled(Logger.Level level) {
		// return true;
	// }
}
