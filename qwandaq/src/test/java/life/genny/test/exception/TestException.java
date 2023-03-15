package life.genny.test.exception;

import life.genny.qwandaq.exception.GennyRuntimeException;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.utils.testsuite.TestCase;

public class TestException extends GennyRuntimeException {
    
    private final String testName;

    private final String msg;

    public TestException(String msg, TestCase<?, ?> test) {
        this(msg, test.getName());
    }

    public TestException(String msg, String testName) {
        this.testName = testName;

        this.msg = msg;
    }

    @Override
    public void printStackTrace() {
        log.error(ANSIColour.doColour("[!] Error executing test: " + testName, ANSIColour.RED));
        if(msg != null) log.error(ANSIColour.doColour("[!] Details: " + msg, ANSIColour.RED));

        super.printStackTrace();
    }

}
