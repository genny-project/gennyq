package life.genny.qwandaq.exception.runtime;

import life.genny.qwandaq.exception.GennyRuntimeException;

/**
 * This exception is used to notify of an error related to definitions
 *
 * @author Jasper Robison
 */
public class DefinitionException extends GennyRuntimeException {

	public DefinitionException() {
		super();
	}

	public DefinitionException(String message) {
		super(message);
	}
	
	public DefinitionException(String message, Throwable err) {
		super(message, err);
	}
}
