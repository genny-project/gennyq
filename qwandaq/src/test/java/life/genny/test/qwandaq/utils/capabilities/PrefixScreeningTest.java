package life.genny.test.qwandaq.utils.capabilities;

import life.genny.qwandaq.utils.testsuite.JUnitTester;


public class PrefixScreeningTest {

    // @Test
    public void testPrefixScreen() {
        new JUnitTester<String, Boolean>()
        // Need to add a test function
        .createTest("Prefix Screening 1")
        .setInput("PER_TESTER")
        .setExpected(true)
        .build()

        .createTest("Prefix Screening 2")
        .setInput("DOC_TESTER")
        .setExpected(false)
        .build()

        .createTest("Prefix Screening 3")
        .setInput("ROL_TESTER")
        .setExpected(true)
        .build()
        
        .createTest("Prefix Screening 4")
        .setInput("TNT_TESTER")
        .setExpected(false)
        .build()

        .assertAll();

    }
}
