package life.genny.qwandaq.exception.runtime;

import life.genny.qwandaq.exception.GennyRuntimeException;

/**
 * This exception is used to notify of an issue during launch.
 *
 * @author Jasper Robison
 */
public class LaunchException extends GennyRuntimeException {

	public LaunchException() {
		super();
	}

	public LaunchException(String message) {
		super();
	}
	
	public LaunchException(String message, Throwable err) {
		super(message, err);
	}

}
