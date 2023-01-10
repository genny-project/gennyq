package life.genny.qwandaq.exception.runtime;

import life.genny.qwandaq.exception.GennyRuntimeException;

/**
 * This exception is used to notify of an issue mail merging.
 *
 * @author Jasper Robison
 */
public class MergeException extends GennyRuntimeException {

	public MergeException() {
		super();
	}

	public MergeException(String message) {
		super();
	}
	
	public MergeException(String message, Throwable err) {
		super(message, err);
	}

}
