package life.genny.qwandaq.exception;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;

import org.jboss.logging.Logger;

import static life.genny.qwandaq.constants.GennyConstants.PACKAGE_PREFIX;

/**
 * A genny exception interface for GennyException and GennyRuntimeException.
 *
 * @author Jasper Robison
 * @author Bryn Meachem
 */
public interface GennyExceptionIntf {

	static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
    
    /**
     * Print a stack trace of the exception (only lines from gennyq, unless no lines are present)
     * @param throwable The throwable
     * @param verbose Use verbose output
     */
    public default void printGennyStackTrace(Throwable throwable, Boolean verbose) {

        log.error("[!] " + throwable.getMessage());
        if (verbose) {
            throwable.printStackTrace(System.err);
            return;
        }

        StackTraceElement[] stack = throwable.getStackTrace();
        List<StackTraceElement> elements = Arrays.asList(stack);
        Boolean hasNonVerboseElements = elements.stream().anyMatch(element -> element.getClassName().startsWith(PACKAGE_PREFIX));
        
        // If there is nothing to print, print verbose
        if (!hasNonVerboseElements) {
            printGennyStackTrace(throwable, true);
            return;
        }
        
        for(StackTraceElement element : elements) {
            String line = "on " + element.getModuleName() + ":" + Integer.toString(element.getLineNumber()) + " - " + element.getMethodName();
            if (element.getClassName().startsWith(PACKAGE_PREFIX))
                log.error(line);
            else log.debug(line);
        }
	}
}
