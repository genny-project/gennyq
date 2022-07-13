package life.genny.qwandaq.exception;

/**
 * GennyKeycloakException --- Custom exception to identified in-house commun 
 * issues which were faced before, known issues or expected problems that 
 * can be documented 
 *
 */
public class GennyKeycloakException extends GennyException {

    private String code;

    public GennyKeycloakException(String code, String message) {
        super(message);
        this.setCode(code);
    }

    public GennyKeycloakException(String code, String message, Throwable cause) {
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
