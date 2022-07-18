package life.genny.qwandaq.exception;

/**
 * This exception is used to notify that an item could not be found
 *
 * @author Jasper Robison
 */
public class IncompleteObjectException extends GennyRuntimeException {

	static String ERR_TEXT = "Field %s has not been initialized";

	public IncompleteObjectException() {
		super();
	}

	public IncompleteObjectException(String code) {
		super(String.format(ERR_TEXT, code));
	}

	public IncompleteObjectException(String code, Throwable err) {
		super(String.format(ERR_TEXT, code), err);
	}

}
