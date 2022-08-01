package life.genny.qwandaq.exception.runtime;

import life.genny.qwandaq.exception.GennyRuntimeException;

/**
 * This exception is used to help debug errors that pop 
 * up in the log to give us a trace
 */
public class DebugException extends GennyRuntimeException {

	public DebugException() {
		super();
	}

	public DebugException(String errorMessage) {
		super(errorMessage);
	}
	
	public DebugException(String errorMessage, Throwable err) {
	    super(errorMessage, err);
	}
}
