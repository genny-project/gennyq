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
