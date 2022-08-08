package life.genny.qwandaq.exception.runtime;

import life.genny.qwandaq.exception.GennyRuntimeException;

/**
 * An Exception to signify malformed of incorrect data.
 */
public class BadDataException extends GennyRuntimeException {

	public BadDataException() {
		super();
	}

	public BadDataException(String message) {
		super(message);
	}
}
