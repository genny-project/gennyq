package life.genny.test.qwandaq.utils;

import org.junit.jupiter.api.Test;

import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.testsuite.JUnitTester;

public class CommonUtilsTests extends BaseTestCase {
    
    @Test
    public void testAddToStringArray() {

        new JUnitTester<String[], String>()
        .setTest((input) -> {
            String array = input.input[0];
            String[] entries = input.input[1].split("::");
            return Expected(CommonUtils.addToStringArray(array, entries));
        })

        .createTest("Add To Array empty 1")
        .setInput(new String[] {
            "",
            "INPUT1::INPUT2"
        })
        .setExpected("[\"INPUT1\",\"INPUT2\"]")
        .build()

        .createTest("Add to array empty 2")
        .setInput(new String[] {
            null,
            "INPUT1::INPUT2"
        })
        .setExpected("[\"INPUT1\",\"INPUT2\"]")
        .build()

        .assertAll();
    }
}
