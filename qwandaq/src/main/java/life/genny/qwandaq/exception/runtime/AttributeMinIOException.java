package life.genny.qwandaq.exception.runtime;

import life.genny.qwandaq.exception.GennyRuntimeException;

/**
 * An Exception to signify malformed of incorrect data.
 */
public class AttributeMinIOException extends GennyRuntimeException {

	public AttributeMinIOException() {
		super();
	}

	public AttributeMinIOException(String message) {
		super(message);
	}

	public AttributeMinIOException(String errorMessage, Throwable err) {
		super(errorMessage, err);
	}
}
