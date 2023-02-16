package life.genny.qwandaq.exception.runtime.config;

import life.genny.qwandaq.exception.GennyRuntimeException;

public class MissingEnvironmentVariableException extends GennyRuntimeException {
    
    private static final String MESSAGE = "Missing environment variable: ";

    public MissingEnvironmentVariableException(String environemtVariable) {
        super(MESSAGE.concat(environemtVariable));
    }
}
