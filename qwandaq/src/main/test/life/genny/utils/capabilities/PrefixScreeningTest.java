package life.genny.test.utils.capabilties;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import life.genny.utils.CapabilityUtilsRefactored;

import static org.junit.Assert.assertEquals;

public class PrefixScreeningTest {

    class TestCase<L, R> {
        public final L input;
        public final R expected;

        public TestCase(L input, R expected) {
            this.input = input;
            this.expected = expected;
        }
    }

    @Test
    public void testPrefixScreen() {
        List<TestCase<String, Boolean>> tests = new ArrayList<>();
        tests.add(new TestCase<String, Boolean>("PER_TESTER", true));
        tests.add(new TestCase<String, Boolean>("DEF_TESTER", true));
        tests.add(new TestCase<String, Boolean>("DOC_TESTER", false));
        tests.add(new TestCase<String, Boolean>("ROL_TESTER", true));
        tests.add(new TestCase<String, Boolean>("TNT_TESTER", false));

        for(TestCase<String, Boolean> test : tests) {
            System.out.println("[!] Prefix Screen testing: " + test.input + ". Expected: " + test.expected);
            assertEquals(test.expected, CapabilityUtilsRefactored.isAllowedToHaveCapabilities(test.input));
        }
    }
}
