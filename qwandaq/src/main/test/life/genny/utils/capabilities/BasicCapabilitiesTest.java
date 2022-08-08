package life.genny.test.utils.capabilties;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import life.genny.qwandaq.test.TestCase;

import life.genny.qwanda.datatype.CapabilityMode;

import static life.genny.utils.CapabilityUtilsRefactored.*;
import static life.genny.qwanda.datatype.CapabilityMode.*;

public class CapabilityUtilsTest {
    
    @Test
    public void cleanCapabilityCodeTest() {
        List<TestCase<String, String>> tests = new ArrayList<>();
        tests.add(new TestCase<String, String>("prm_APPLE", "PRM_APPLE"));
        tests.add(new TestCase<String, String>("OWN_APPLE", "PRM_OWN_APPLE"));

        for(TestCase<String, String> test : tests) {
            System.out.println("[!] cleanCapCode testing: " + test.input);
            assertEquals(test.expected, cleanCapabilityCode(test.input));
        }
    }

    private TestCase<String, CapabilityMode[]> createCapModeTest(String input, CapabilityMode... expected) {
        return new TestCase<String, CapabilityMode[]>(input, expected);
    }

    @Test
    public void getCapModeArrayTest() {
        List<TestCase<String, CapabilityMode[]>> tests = new ArrayList<>();
        tests.add(createCapModeTest("EDIT", EDIT));
        tests.add(createCapModeTest("[\"VIEW\",\"ADD\"]", VIEW, ADD));

        for(TestCase<String, CapabilityMode[]> test : tests) {
            System.out.println("[!] getCapModeArrayFromString testing: " + test.input);
            assertArrayEquals(test.expected, getCapModesFromString(test.input));
        }
    }

    private TestCase<CapabilityMode[], String> createCapModeStringTest(String expected, CapabilityMode... input) {
        return new TestCase<CapabilityMode[], String>(input, expected);
    }

    @Test
    public void getCapModeStringTest() {
        List<TestCase<CapabilityMode[], String>> tests = new ArrayList<>();
        tests.add(createCapModeStringTest("[\"EDIT\"]", EDIT));
        tests.add(createCapModeStringTest("[\"VIEW\",\"ADD\"]", VIEW, ADD));

        for(TestCase<CapabilityMode[], String> test : tests) {
            System.out.println("[!] getModeString testing: " + test.expected);
            assertEquals(test.expected, getModeString(test.input));
        }
    }

    private TestCase<CapabilityMode[], CapabilityMode> createHighestPrioCapTest(CapabilityMode expected, CapabilityMode... input) {
        return new TestCase<CapabilityMode[], CapabilityMode>(input, expected);
    }

    @Test
    public void getHighestPriorityCapTest() {
        List<TestCase<CapabilityMode[], CapabilityMode>> tests = new ArrayList<>();
        tests.add(createHighestPrioCapTest(DELETE, // expected delete
                                    EDIT, DELETE, ADD, VIEW)); // input [EDIT, DELETE, ADD, VIEW]

        for(TestCase<CapabilityMode[], CapabilityMode> test : tests) {
            System.out.println("[!] Highest Priority testing: " + getModeString(test.input));
            assertEquals(test.expected, getHighestPriorityCap(test.input));
        }
    }
    
    private TestCase<CapabilityMode, CapabilityMode[]> createLesserModeTest(CapabilityMode input, CapabilityMode... expected) {
        return new TestCase<CapabilityMode, CapabilityMode[]>(input, expected);
    }

    @Test
    public void getLesserModesTest() {
        List<TestCase<CapabilityMode, CapabilityMode[]>> tests = new ArrayList<>();
        tests.add(createLesserModeTest(DELETE, // input delete
            ADD, EDIT, VIEW, NONE)); // expected [add, edit, view, none]

        tests.add(createLesserModeTest(VIEW, // input delete
            NONE)); // expected [none]

        for(TestCase<CapabilityMode, CapabilityMode[]> test : tests) {
            System.out.println("[!] Lesser modes testing: " + test.input.name());
            assertArrayEquals(test.expected, CapabilityMode.getLesserModes(test.input).toArray(new CapabilityMode[0]));
        }
    }
}
