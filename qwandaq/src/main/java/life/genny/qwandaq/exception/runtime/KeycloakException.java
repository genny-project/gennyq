package life.genny.qwandaq.exception.runtime;

import life.genny.qwandaq.exception.GennyRuntimeException;

/**
 * This exception is used to help debug errors that pop 
 * up in the log to give us a trace
 */
public class KeycloakException extends GennyRuntimeException {

	public KeycloakException() {
		super();
	}

	public KeycloakException(String message) {
		super(message);
	}
	
	public KeycloakException(String message, Throwable err) {
	    super(message, err);
	}
}
