package life.genny.bridge.exception;

public class ClientIdException extends BridgeException {

    public ClientIdException(String message) {
        this(BridgeException.MISSING_PRODUCT_CODE, message);
    }

    public ClientIdException(String code, String message) {
        super(code, message);
    }

    public ClientIdException(String message, Throwable cause) {
        this(BridgeException.MISSING_PRODUCT_CODE, message, cause);
    }

    public ClientIdException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }

}
