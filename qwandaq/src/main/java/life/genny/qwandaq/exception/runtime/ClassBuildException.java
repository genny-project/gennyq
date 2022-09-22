package life.genny.qwandaq.exception.runtime;

import life.genny.qwandaq.exception.GennyRuntimeException;

/**
 * This exception is used when errors occur while modifying an object
 */
public class ClassBuildException extends GennyRuntimeException {

	public ClassBuildException() {
		super();
	}

	public ClassBuildException(String errorMessage) {
		super(errorMessage);
	}
	
	public ClassBuildException(String errorMessage, Throwable err) {
	    super(errorMessage, err);
	}
}
