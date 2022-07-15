package life.genny.qwandaq.exception;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import static life.genny.qwandaq.constants.GennyConstants.PACKAGE_PREFIX;

/**
 * A base exception class utility used as common logic 
 * for GennyException and GennyRuntimeException.
 *
 * @author Jasper Robison
 */
public class GennyExceptionBase {

	static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
    
    /**
     * Print a stack trace of the exception (only lines from gennyq, unless no lines are present)
     * @param throwable The throwable
     * @param verbose Use verbose output
     */
    public static void printStackTrace(Throwable throwable, Boolean verbose) {

        log.error("[!] " + throwable.getMessage());
        if (verbose) {
            throwable.printStackTrace();
            return;
        }

        StackTraceElement[] stack = throwable.getStackTrace();
        Stream<StackTraceElement> elementStream = Arrays.asList(stack).stream();
        Boolean hasNonVerboseElements = elementStream.anyMatch(element -> element.getClassName().startsWith(PACKAGE_PREFIX));
        
        // If there is nothing to print, print verbose
        if (!hasNonVerboseElements) {
            throwable.printStackTrace();
            return;
        }
        
        elementStream.forEach(element -> {
            String line = "on " + element.getModuleName() + ":" + Integer.toString(element.getLineNumber()) + " - " + element.getMethodName();
            if (element.getClassName().startsWith(PACKAGE_PREFIX))
                log.error(line);
            else log.debug(line);
        });
	}
}
