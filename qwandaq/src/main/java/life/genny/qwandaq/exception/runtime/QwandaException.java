package life.genny.qwandaq.exception.runtime;

import life.genny.qwandaq.exception.GennyRuntimeException;

/**
 * This exception is used to help notify when something 
 * has gone wrong with the core Qwanda functions and helpers.
 *
 * @author Jasper Robison
 */
public class QwandaException extends GennyRuntimeException {

	public QwandaException() {
		super();
	}

	public QwandaException(String message) {
		super(message);
	}
	
	public QwandaException(String message, Throwable err) {
	    super(message, err);
	}
}
