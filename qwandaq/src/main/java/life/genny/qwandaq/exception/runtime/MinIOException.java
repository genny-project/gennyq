package life.genny.qwandaq.exception.runtime;

import life.genny.qwandaq.exception.GennyRuntimeException;

public class MinIOException extends GennyRuntimeException {

    public MinIOException() {
        super();
    }

    public MinIOException(String message) {
        super(message);
    }

    public MinIOException(String message, Throwable err) {
        super(message, err);
    }
}
