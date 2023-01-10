package life.genny.qwandaq.exception.runtime.search;

import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.exception.GennyRuntimeException;

public class SearchException extends GennyRuntimeException {
    
    public SearchException(String entityCode, String message, Throwable throwable) {
        super(errorMessage(entityCode, message), throwable);
    }

    public SearchException(SearchEntity entity, String message, Throwable throwable) {
        this(entity.getCode(), message, throwable);
    }

    public SearchException(SearchEntity entity, String message) {
        this(entity.getCode(), message);
    }

    public SearchException(String entityCode, String message) {
        super(errorMessage(entityCode, message));
    }

    private static String errorMessage(String entityCode, String message) {
        return new StringBuilder("Exception with Search: ")
        .append(entityCode)
        .append(" - ")
        .append(message)
        .toString();
    }
}
