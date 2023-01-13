package life.genny.test.qwandaq.utils.common;

import org.junit.Test;

import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.testsuite.JUnitTester;
import life.genny.test.qwandaq.utils.BaseTestCase;

public class StringTests extends BaseTestCase {
    
    @Test
    public void substitutePrefix() {
        new JUnitTester<String[], String>()

        .setTest(input -> {
            return Expected(CommonUtils.substitutePrefix(input.input[0], input.input[1])); // code, prefix
        })

        .createTest("Test CAP_ DEF_TEST -> CAP_TEST")
        .setInput(new String[] {"DEF_TEST", "CAP_"})
        .setExpected("CAP_TEST")
        .build()

        .createTest("Test CAP DEF_TEST -> CAP_TEST")
        .setInput(new String[] {"DEF_TEST", "CAP"})
        .setExpected("CAP_TEST")
        .build()

        .createTest("Test CA DEF_TEST -> BAD")
        .setInput(new String[] {"DEF_TEST", "CA"})
        .setExpected("DEF_TEST")
        .build()

        .createTest("Test CAPP DEF_TEST -> BAD")
        .setInput(new String[] {"DEF_TEST", "CAPP"})
        .setExpected("DEF_TEST")
        .build()

        .assertAll();
    }
}
