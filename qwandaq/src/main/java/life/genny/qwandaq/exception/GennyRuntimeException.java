package life.genny.qwandaq.exception;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.Logger;

/**
 * Custom Genny System Runtime Exception to identify 
 * common issues within the logic at runtime.
 *
 * @author Jasper Robison
 */
public class GennyRuntimeException extends RuntimeException {

	static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
    
    public GennyRuntimeException() {
        super();
    }

	public GennyRuntimeException(String message) {
		super(message);
	}

	public GennyRuntimeException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    @Override
    public void printStackTrace() {
        GennyExceptionBase.printStackTrace(this, false);
    }
}
