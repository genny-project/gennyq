package life.genny.qwandaq.exception;

/**
 * This exception is used to notify of a null parameter that must have a value.
 *
 * @author Jasper Robison
 */
public class NullParameterException extends GennyRuntimeException {

	static String ERR_SUFFIX = " passed is null";

	public NullParameterException() {
		super();
	}

	public NullParameterException(String parameter) {
		super(parameter + ERR_SUFFIX);
	}
	
	public NullParameterException(String parameter, Throwable err) {
		super(parameter + ERR_SUFFIX, err);
	}
}
