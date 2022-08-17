package life.genny.test.utils;

import org.jboss.logging.Logger;
import static life.genny.qwandaq.utils.CommonUtils.equalsBreak;

import life.genny.qwandaq.models.ANSIColour;
import life.genny.test.exception.TestException;
import life.genny.test.utils.suite.TestCase;

import static org.junit.Assert.assertEquals;

import java.util.List;

/**
 * 
 * @author Bryn Meachem
 */
public class JUnitTester<Input, Expected> {
	protected final Logger log;
    
    private static final String L10_EQUALS_BREAK = equalsBreak(10);
    private static final String DEFAULT_FORMAT = "  input=%s, expected=%s";

    public JUnitTester(Class<?> clazz) {
        log = Logger.getLogger(clazz != null ? clazz : JUnitTester.class);
    }

    public void testCase(TestCase<Input, Expected> test) {
        testCase(DEFAULT_FORMAT, test);
    }

    public void testCase(String testDetailsFormatString, TestCase<Input, Expected> test) {
        log.infof(ANSIColour.CYAN + testDetailsFormatString + ANSIColour.RESET, test.input, test.expected);
        if(test.testCallback == null) {
            throw new TestException("Test: " + test.name + " has no standard test defined!", test);
        }
        
        Expected result = test.testCallback.test(test.input);
        log.infof("     RESULT: " + result);
        assertEquals(test.expected, result);
    }

    public void testCases(String testTitle, List<TestCase<Input, Expected>> tests) {
        log.info(ANSIColour.PURPLE + L10_EQUALS_BREAK + " " + testTitle + " " + L10_EQUALS_BREAK + ANSIColour.RESET);
        for(TestCase<Input, Expected> test : tests) {
            testCase(test);
        }
    }

    public void testCases(String testTitle, String testDetailsFormatString, List<TestCase<Input, Expected>> tests) {
        log.info("\n" + ANSIColour.PURPLE + L10_EQUALS_BREAK + " " + testTitle + " " + L10_EQUALS_BREAK);
        for(TestCase<Input, Expected> test : tests) {
            testCase("[!] " + testDetailsFormatString, test);
        }
    }    
}
