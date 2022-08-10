package life.genny.qwandaq.exception.runtime;

import life.genny.qwandaq.exception.GennyRuntimeException;

/**
 * This exception is used to help notify when something 
 * has not been initialized before trying to access or use it.
 *
 * @author Jasper Robison
 */
public class NotInitializedException extends GennyRuntimeException {

	public NotInitializedException() {
		super();
	}

	public NotInitializedException(String errorMessage) {
		super(errorMessage);
	}
	
	public NotInitializedException(String errorMessage, Throwable err) {
	    super(errorMessage, err);
	}
}
