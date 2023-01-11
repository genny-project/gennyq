package life.genny.bootq.exception;

import life.genny.qwandaq.exception.GennyRuntimeException;

/**
 * This exception is used in bootq.
 */
public class BootQException extends GennyRuntimeException {

	public BootQException() {
		super();
	}

	public BootQException(String msg) {
		super(msg);
	}
	
	public BootQException(String msg, Throwable err) {
	    super(msg, err);
	}
}
