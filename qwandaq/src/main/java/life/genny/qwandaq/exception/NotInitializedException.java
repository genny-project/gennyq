package life.genny.qwandaq.exception;

/**
 * This exception is used to help notify when something 
 * has not been initialized before trying to access or use it.
 */
public class NotInitializedException extends GennyException {

	public NotInitializedException() {
		super();
	}

	public NotInitializedException(String errorMessage) {
		super(errorMessage);
	}
	
	public NotInitializedException(String errorMessage, Throwable err) {
	    super(errorMessage, err);
	}
}
