package life.genny.qwandaq.exception;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GennyException extends Exception {
    
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
        System.err.println("[!] " + this.getMessage());
		
        if(verbose) {
            super.printStackTrace();
            return;
        }

        StackTraceElement[] stack = this.getStackTrace();
        List<StackTraceElement> nonVerboseStack = Arrays.asList(stack).stream()
        .filter(element -> element.getClassName().startsWith("life.genny"))
        .collect(Collectors.toList());
        
        // If there is nothing to print, print verbose
        if(nonVerboseStack.size() == 0) {
            this.printStackTrace(true);
            return;
        }
        
        nonVerboseStack.stream().forEach(element -> {
            String line = element.getModuleName() + ":" + element.getClassName() + ":" + Integer.toString(element.getLineNumber()) + " - " + element.getMethodName();
            System.err.println(line);
        });
	}
}
