package life.genny.qwandaq.exception;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.Logger;

/**
 * Custom Genny System Runtime Exception to identify 
 * common issues within the logic at runtime.
 *
 * @author Jasper Robison
 * @author Bryn Meachem
 */
public abstract class GennyRuntimeException extends RuntimeException implements GennyExceptionIntf {

	protected static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
    
    public GennyRuntimeException() {
        super();
    }

    public GennyRuntimeException(Throwable cause) {
        super(cause);
    }

	public GennyRuntimeException(String message) {
		super(message);
	}

	public GennyRuntimeException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public void printStackTrace() {
        this.printGennyStackTrace(this, true);
    }
}
