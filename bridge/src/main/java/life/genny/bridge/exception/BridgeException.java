package life.genny.bridge.exception;

/**
 * BridgeException --- Custom exception to identified in-house commun 
 * issues which were faced before, known issues or expected problems that 
 * can be documented 
 *
 * @author    hello@gada.io
 *
 */
public class BridgeException extends Exception {

    public static final String NULL_FIELD = "GEN_000";
    public static final String MISSING_PRODUCT_CODE = "GEN_001";
    public static final String MALFORMED_PRODUCT_CODES = "GEN_002";

    private String code;

    public BridgeException(String code, String message) {
        super(message);
        this.setCode(code);
    }

    public BridgeException(String code, String message, Throwable cause) {
        super(message, cause);
        this.setCode(code);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
