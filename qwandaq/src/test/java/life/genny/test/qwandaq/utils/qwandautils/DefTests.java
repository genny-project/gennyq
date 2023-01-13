package life.genny.test.qwandaq.utils.qwandautils;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import life.genny.qwandaq.entity.Definition;
import life.genny.qwandaq.utils.testsuite.JUnitTester;

public class DefTests extends BaseDefTest {

    static {
        initDefaultDefs();
    }

    @Test
    public void testParents() {
        new JUnitTester<String, String[]>()
        .setTest((input) -> {
            // input be code
            // output array of parent codes if lnk include exists
            Definition be = getDefinition(input.input);
            if(be == null)
                throw new AssertionError(input.input + " not loaded");
            return Expected(be.getParentCodes());
        })
        .setAssertion((result, expected) -> assertArrayEquals(expected, result))

        .createTest("Test DEF_TEST Parents")
        .setInput("DEF_TEST")
        .setExpected(new String[] {"DEF_TEST_FATHER"})
        .build()

        .createTest("Test DEF_TEST_FATHER Parents")
        .setInput("DEF_TEST_FATHER")
        .setExpected(new String[] {"DEF_TEST_GRANDFATHER"})
        .build()

        .createTest("Test DEF_TEST_GRANDFATHER Parents")
        .setInput("DEF_TEST_GRANDFATHER")
        .setExpected(new String[] {"DEF_TEST_ANCIENT1", "DEF_TEST_ANCIENT2"})
        .build()

        .createTest("Test DEF_TEST_ANCIENT1 Parents")
        .setInput("DEF_TEST_ANCIENT1")
        .setExpected(new String[] {"DEF_TEST_ANCIENT2"})
        .build()

        .createTest("Test DEF_TEST_ANCIENT2 Parents")
        .setInput("DEF_TEST_ANCIENT2")
        .setExpected(new String[] {})
        .build()

        .assertAll();
    }
    
}
