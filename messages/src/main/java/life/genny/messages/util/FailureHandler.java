package life.genny.messages.util;

import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.models.ANSIColour;
import org.jboss.logging.Logger;

public class FailureHandler {

    private static final Logger log = Logger.getLogger(FailureHandler.class);

    private static <T> T handle(Handler<T> handler, Boolean optional) {
        try {
            return handler.handle();
        } catch (ItemNotFoundException itemNotFoundException) {
            if (!optional) {
                log.error("Exception: " + ANSIColour.doColour(itemNotFoundException.getMessage(), ANSIColour.RED));
                throw itemNotFoundException;
            } else {
                log.error(ANSIColour.doColour(itemNotFoundException.getMessage() + " - Marked as optional, continuing", ANSIColour.RED));
                return null;
            }
        } catch (Exception exception) {
            log.error("Exception: " + ANSIColour.doColour(exception.getMessage(), ANSIColour.RED));
            throw exception;
        }
    }

    private static <T> T handle(Handler<T> handler, Boolean optional, T fallback) {
        try {
            return handler.handle();
        } catch (ItemNotFoundException itemNotFoundException) {
            if (!optional) {
                log.error("Exception: " + ANSIColour.doColour(itemNotFoundException.getMessage(), ANSIColour.RED));
                throw itemNotFoundException;
            } else {
                if (fallback != null) {
                    return fallback;
                } else {
                    return null;
                }
            }
        } catch (Exception exception) {
            log.error("Exception: " + ANSIColour.doColour(exception.getMessage(), ANSIColour.RED));
            throw exception;
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
        T handle() throws ItemNotFoundException;
    }

}



