package life.genny.kogito.common.service;

public class ServiceException extends RuntimeException {

    public ServiceException(String message) {
        super(message);
    }
}