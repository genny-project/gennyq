package life.genny.test.qwandaq.utils.capabilities;


import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import life.genny.qwandaq.datatype.CapabilityMode;
import life.genny.qwandaq.utils.CapabilityUtils;
import life.genny.test.utils.callbacks.test.FITestCallback;
import life.genny.test.utils.suite.TestCase;

import static life.genny.test.utils.suite.TestCase.Builder;
import static life.genny.test.utils.suite.TestCase.Input;
import static life.genny.test.utils.suite.TestCase.Expected;

import static life.genny.qwandaq.datatype.CapabilityMode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;


public class CapabilityUtilsTest {

	static final Logger log = Logger.getLogger(CapabilityUtils.class);


    @Test
    public void cleanCapabilityCodeTest() {
        Builder<String, String> builder = new Builder<String, String>();

        FITestCallback<Input<String>, Expected<String>> testFunction = (Input<String> input) -> {
            return new Expected<String>(CapabilityUtils.cleanCapabilityCode(input.input));
        };
        
        List<TestCase<String, String>> tests = new ArrayList<>();
        tests.add(
            builder.setName("Clean Cap 1")
                    .setInput("OWN_APPLE")
                    .setExpected("CAP_OWN_APPLE")
                    .setTest(testFunction)
                    .build()
        );
        
        tests.add(
            builder.setName("Clean Cap 2")
                    .setInput("oWn_ApplE")
                    .setExpected("CAP_OWN_APPLE")
                    .setTest(testFunction)
                    .build()
        );

        for(TestCase<String, String> test : tests) {
            assertEquals(test.getExpected(), test.test());
        }

    }

    @Test
    public void getCapModeArrayTest() {
        Builder<String, CapabilityMode[]> builder = new Builder<String, CapabilityMode[]>();
        
        List<TestCase<String, CapabilityMode[]>> tests = new ArrayList<>();

        FITestCallback<Input<String>, Expected<CapabilityMode[]>> testFunction = (Input<String> input) -> {
            return new Expected<CapabilityMode[]>(CapabilityUtils.getCapModesFromString(input.input));
        };
        
        tests.add(
            builder.setName("Create Cap Mode 1")
                .setInput("EDIT")
                .setExpected(new CapabilityMode[] {EDIT})
                .setTest(testFunction)
                .build()
        );
    
        // TODO: Need to figure out a better way to do this
        tests.add(
            builder.setName("Create Cap Mode 2")
                .setInput("[\"VIEW\",\"ADD\"]")
                .setExpected(new CapabilityMode[] {VIEW, ADD})
                .setTest(testFunction)
                .build()
        );

        for(TestCase<String, CapabilityMode[]> test : tests) {
            log.info(test.name);
            assertArrayEquals(test.getExpected(), test.test());
        }
    }

    @Test
    public void getCapModeStringTest() {
        Builder<CapabilityMode[], String> builder = new Builder<CapabilityMode[], String>();
        
        List<TestCase<CapabilityMode[], String>> tests = new ArrayList<>();

        FITestCallback<Input<CapabilityMode[]>, Expected<String>> testFunction = (input) -> {
            return new Expected<String>(CapabilityUtils.getModeString(input.input));
        };

        tests.add(
            builder.setName("Serialise CapabilityMode[] array 1")
                .setInput(new CapabilityMode[] {EDIT})
                .setExpected("[\"EDIT\"]")
                .setTest(testFunction)
                .build()
        );
    
        // TODO: Need to figure out a better way to do this
        tests.add(
            builder.setName("Serialise CapabilityMode[] array 2")
                .setInput(new CapabilityMode[] {VIEW, ADD})
                .setExpected("[\"VIEW\",\"ADD\"]")
                .setTest(testFunction)
                .build()
        );

        for(TestCase<CapabilityMode[], String> test : tests) {
            assertEquals(test.getExpected(), test.test());
        }
    }
}
