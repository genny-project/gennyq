package life.genny.qwandaq.exception.checked;

import life.genny.qwandaq.exception.GennyException;

/**
 * This exception is used in handling role based actions.
 */
public class RoleException extends GennyException {

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
