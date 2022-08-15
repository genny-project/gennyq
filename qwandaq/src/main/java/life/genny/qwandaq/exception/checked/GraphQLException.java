package life.genny.qwandaq.exception.checked;

import life.genny.qwandaq.exception.GennyException;

/**
 * This exception is used in handling graphql related issues.
 */
public class GraphQLException extends GennyException {

	public GraphQLException() {
		super();
	}

	public GraphQLException(String errorMessage) {
		super(errorMessage);
	}
	
	public GraphQLException(String errorMessage, Throwable err) {
	    super(errorMessage, err);
	}
}
