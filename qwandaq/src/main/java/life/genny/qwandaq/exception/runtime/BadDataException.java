package life.genny.qwandaq.exception.runtime;

import life.genny.qwandaq.exception.GennyRuntimeException;

/**
 * An Exception to signify malformed of incorrect data.
 */
public class BadDataException extends GennyRuntimeException {

	public BadDataException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public BadDataException(Throwable cause) {
		super(cause);
	}

	public BadDataException(String message) {
		super(message);
	}

	public BadDataException() {
		super();
	}

}
