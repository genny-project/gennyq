package life.genny.bridge.exception;

public class ProductCodeException extends BridgeException {
    
    public ProductCodeException(String message) {
        this(BridgeException.MALFORMED_PRODUCT_CODES, message);
    }

    public ProductCodeException(String code, String message) {
        super(code, message);
    }

    public ProductCodeException(String message, Throwable cause) {
        this(BridgeException.MISSING_PRODUCT_CODE, message, cause);
    }

    public ProductCodeException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
