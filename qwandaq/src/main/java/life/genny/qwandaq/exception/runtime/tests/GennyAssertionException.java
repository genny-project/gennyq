package life.genny.qwandaq.exception.runtime.tests;

import life.genny.qwandaq.exception.GennyRuntimeException;

public class GennyAssertionException extends GennyRuntimeException {
    
	public GennyAssertionException(String testName, Throwable err) {
        super("Error asserting TestCase: " + testName, err);
    }

    public void printStackTrace() {
        this.printGennyStackTrace(this, true);
    }
}
