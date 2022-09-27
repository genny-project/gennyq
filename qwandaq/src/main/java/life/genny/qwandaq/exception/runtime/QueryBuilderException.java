package life.genny.qwandaq.exception.runtime;

import life.genny.qwandaq.exception.GennyRuntimeException;

/**
 * This exception is used to notify of an error during query building
 * @author Jasper Robison
 */
public class QueryBuilderException extends GennyRuntimeException {


	public QueryBuilderException() {
		super();
	}

	public QueryBuilderException(String message) {
		super(message);
	}
	
	public QueryBuilderException(String message, Throwable err) {
		super(message, err);
	}
}
