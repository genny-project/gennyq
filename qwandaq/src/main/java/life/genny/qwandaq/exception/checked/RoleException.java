package life.genny.qwandaq.exception.checked;

import life.genny.qwandaq.exception.GennyRuntimeException;

/**
 * This exception is used in handling role based actions.
 */
public class RoleException extends GennyRuntimeException {

	public RoleException() {
		super();
	}

	public RoleException(String errorMessage) {
		super(errorMessage);
	}
	
	public RoleException(String errorMessage, Throwable err) {
	    super(errorMessage, err);
	}
}
