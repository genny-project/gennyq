package life.genny.qwandaq.exception;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

/**
 * Custom Genny System exception to identify 
 * common issues within the logic.
 */
public class GennyException extends RuntimeException {
	static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
    public static final String PACKAGE_PREFIX = "life.genny";
    
    public GennyException() {
        super();
    }

	public GennyException(String message) {
		super(message);
	}

	public GennyException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    @Override
    public void printStackTrace() {
        printStackTrace(false);
    }

    /**
     * Print a non verbose stack trace of the exception (only lines from gennyq, unless no lines are present)
     * @param verbose
     */
    public void printStackTrace(Boolean verbose) {
        log.error("[!] " + this.getMessage());
		
        if(verbose) {
            super.printStackTrace();
            return;
        }

        StackTraceElement[] stack = this.getStackTrace();
        
        Stream<StackTraceElement> elementStream = Arrays.asList(stack).stream();
        
        Boolean hasNonVerboseElements = elementStream.anyMatch(element -> element.getClassName().startsWith(PACKAGE_PREFIX));
        
        // If there is nothing to print, print verbose
        if(!hasNonVerboseElements) {
            this.printStackTrace(true);
            return;
        }
        
        elementStream.forEach(element -> {
            String line = "on " + element.getModuleName() + ":" + Integer.toString(element.getLineNumber()) + " - " + element.getMethodName();
            if(element.getClassName().startsWith(PACKAGE_PREFIX))
                log.error(line);
            else log.debug(line);
        });
	}
}
