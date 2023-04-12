package life.genny.qwandaq.utils;

import life.genny.qwandaq.models.ANSIColour;
import org.jboss.logging.Logger;

public class FailureHandler {

    private static final Logger log = Logger.getLogger(FailureHandler.class);

    private static <T> T handle(Handler<T> handler, Boolean optional, T fallback) {
        try {
            T value = handler.handle();
            if(!optional && value == null){
                throw new NullPointerException();
            }else{
                if (fallback != null) {
                    return fallback;
                } else {
                    return value;
                }
            }
        } catch (RuntimeException runtimeException) {
            if (!optional) {
                log.error("Exception: " + ANSIColour.doColour(runtimeException.getMessage(), ANSIColour.RED));
                throw runtimeException;
            } else {
                if (fallback != null) {
                    return fallback;
                } else {
                    return null;
                }
            }
        }
    }

    public static <T> T required(Handler<T> handler) {
        return handle(handler, false, null);
    }

    public static <T> T optional(Handler<T> handler) {
        return handle(handler, true, null);
    }

    public static <T> T optional(Handler<T> handler, T fallback) {
        return handle(handler, true, fallback);
    }

    public interface Handler<T> {
        T handle() throws RuntimeException;
    }
}



