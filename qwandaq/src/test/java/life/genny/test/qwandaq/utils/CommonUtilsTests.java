package life.genny.test.qwandaq.utils;

import org.junit.jupiter.api.Test;

import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.testsuite.JUnitTester;

public class CommonUtilsTests extends BaseTestCase {
    
    @Test
    public void removeFromStringArray() {
        new JUnitTester<String[], String>()
        .setTest((input) -> {
            String array = input.input[0];
            String[] entries = input.input[1] != null ? input.input[1].split("::") : null;
            return Expected(CommonUtils.removeFromStringArray(array, entries));
        })

        .createTest("Remove from array blank 1")
        .setInput(new String[] {
            "",
            "INPUT1::INPUT2"
        })
        .setExpected("[]")
        .build()

        .createTest("Remove from array blank 2")
        .setInput(new String[] {
            null,
            "INPUT1::INPUT2"
        })
        .setExpected("[]")
        .build()

        .createTest("Remove from array blank 3")
        .setInput(new String[] {
            " ",
            "INPUT1::INPUT2"
        })
        .setExpected("[]")
        .build()

        .createTest("Remove from array empty 1")
        .setInput(new String[] {
            "[]",
            "INPUT1"
        })
        .setExpected("[]")
        .build()

        .createTest("Remove from array empty entry 1")
        .setInput(new String[] {
            "[]",
            ""
        })
        .setExpected("[]")
        .build()

        .createTest("Remove from array empty entry 2")
        .setInput(new String[] {
            "[]",
            null
        })
        .setExpected("[]")
        .build()

        .createTest("Remove array entries 1")
        .setInput(new String[] {
            "[\"INPUT1\",\"INPUT2\"]"
            ,"INPUT1"
        })
        .setExpected("[\"INPUT2\"]")
        .build()

        .createTest("Remove array entries 2")
        .setInput(new String[] {
            "[\"INPUT1\",\"INPUT2\"]"
            ,"INPUT2"
        })
        .setExpected("[\"INPUT1\"]")
        .build()

        .createTest("Remove array entries 3")
        .setInput(new String[] {
            "[\"INPUT1\",\"INPUT2\",\"INPUT3\",\"INPUT4\"]",
            "INPUT3::INPUT4"
        })
        .setExpected("[\"INPUT1\",\"INPUT2\"]")
        .build()

        .createTest("Remove array entries 4")
        .setInput(new String[] {
            "[\"INPUT1\",\"INPUT2\"]"
            ,"INPUT2::INPUT1::INPUT3"
        })
        .setExpected("[]")
        .build()

        .createTest("Remove single entry 1")
        .setInput(new String[] {
            "[\"INPUT1\"]",
            "INPUT1"
        })
        .setExpected("[]")
        .build()

        .assertAll();
    }

    @Test
    public void testAddToStringArrayNulls() {
        new JUnitTester<String[], String>()
        .setTest((input) -> {
            // static array (until JUnitTester gets an upgrade)
            String array = "[]";
            return Expected(CommonUtils.addToStringArray(array, input.input));
        })

        .createTest("Add nulls to empty array 1")
        .setInput(new String[] {
            null,
            null
        })
        .setExpected("[\"null\",\"null\"]")
        .build()

        .createTest("Add null to empty array 2")
        .setInput(new String [] {})
        .setExpected("[]")
        .build()

        .createTest("Add nulls to preexisting array 1")
        .setTest((input) -> {
            // static array (until JUnitTester gets an upgrade)
            String array = "[\"INPUT1\"]";
            return Expected(CommonUtils.addToStringArray(array, input.input));
        })
        .setInput(new String[] {
            null,
            null
        })
        .setExpected("[\"INPUT1\",\"null\",\"null\"]")
        .build()

        .createTest("Add nulls to null array 1")
        .setTest((input) -> {
            String array = null;
            return Expected(CommonUtils.addToStringArray(array, input.input));
        })
        .setInput(new String[] {
            null,
            null
        })
        // TODO: I am open to suggestion on what we think the system should do here
        .setExpected("[\"null\",\"null\"]")
        .build()

        .createTest("Add a null entry set to null array 1")
        .setTest((input) -> {
            String array = null;
            return Expected(CommonUtils.addToStringArray(array, input.input));
        })
        .setInput(null)
        .setExpected("[]")
        .build()

        .assertAll();
    }

    @Test
    public void testAddToStringArray() {

        new JUnitTester<String[], String>()
        .setTest((input) -> {
            String array = input.input[0];
            String[] entries = input.input[1] != null ? input.input[1].split("::") : null;
            return Expected(CommonUtils.addToStringArray(array, entries));
        })

        .createTest("Add To Array blank 1")
        .setInput(new String[] {
            "",
            "INPUT1::INPUT2"
        })
        .setExpected("[\"INPUT1\",\"INPUT2\"]")
        .build()

        .createTest("Add to array blank 2")
        .setInput(new String[] {
            null,
            "INPUT1::INPUT2"
        })
        .setExpected("[\"INPUT1\",\"INPUT2\"]")
        .build()

        .createTest("Add to array blank 3")
        .setInput(new String[] {
            " ",
            "INPUT1::INPUT2"
        })
        .setExpected("[\"INPUT1\",\"INPUT2\"]")
        .build()


        .createTest("Add to array empty 1")
        .setInput(new String[] {
            "[]",
            "INPUT1"
        })
        .setExpected("[\"INPUT1\"]")
        .build()

        .createTest("Add to array empty entry 1")
        .setInput(new String[] {
            "[]",
            ""
        })
        .setExpected("[\"\"]")
        .build()

        .createTest("Add to array empty entry 2")
        .setInput(new String[] {
            "[]",
            null
        })
        .setExpected("[]")
        .build()

        .createTest("Add to preexisting array entries 1")
        .setInput(new String[] {
            "[\"INPUT1\",\"INPUT2\"]"
            ,"INPUT3"
        })
        .setExpected("[\"INPUT1\",\"INPUT2\",\"INPUT3\"]")
        .build()

        .createTest("Add to preexisting array entries 2")
        .setInput(new String[] {
            "[\"INPUT1\",\"INPUT2\"]"
            ,"INPUT3::INPUT4"
        })
        .setExpected("[\"INPUT1\",\"INPUT2\",\"INPUT3\",\"INPUT4\"]")
        .build()

        .assertAll();
    }
}
