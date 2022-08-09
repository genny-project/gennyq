package life.genny.test.qwandaq.utils.capabilities;


import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import life.genny.qwandaq.datatype.CapabilityMode;
import life.genny.qwandaq.utils.CapabilityUtils;

import life.genny.test.utils.JUnitTester;
import life.genny.test.utils.suite.TestCase;
import life.genny.test.utils.suite.TestBuilder;

import static life.genny.qwandaq.datatype.CapabilityMode.*;


public class CapabilityUtilsTest {


    @Test
    public void cleanCapabilityCodeTest() {
        TestBuilder<String, String> builder = new TestBuilder<String, String>();
        JUnitTester<String, String> tester = builder.getTester(CapabilityUtilsTest.class);

        List<TestCase<String, String>> tests = new ArrayList<>();
        tests.add(
            builder.setName("Clean Cap 1")
                    .setInput("OWN_APPLE")
                    .setExpected("PRM_OWN_APPLE")
                    .setTestFunction(CapabilityUtils::cleanCapabilityCode)
                    .build()
        );
        
        tests.add(
            builder.setName("Clean Cap 2")
                    .setInput("oWn_ApplE")
                    .setExpected("PRM_OWN_APPLE")
                    .setTestFunction(CapabilityUtils::cleanCapabilityCode)
                    .build()
        );

        tester.testCases("Clean Capability Code", tests);

    }

    @Test
    public void getCapModeArrayTest() {
        TestBuilder<String, CapabilityMode[]> builder = new TestBuilder<String, CapabilityMode[]>();
        JUnitTester<String, CapabilityMode[]> tester = builder.getTester(CapabilityUtilsTest.class);
        
        CapabilityMode[] expected1 = {EDIT};
        List<TestCase<String, CapabilityMode[]>> tests = new ArrayList<>();
        tests.add(
            builder.setName("Create Cap Mode 1")
                .setInput("EDIT")
                .setExpected(expected1)
                .setTestFunction(CapabilityUtils::getCapModesFromString)
                .build()
        );
    
        // TODO: Need to figure out a better way to do this
        CapabilityMode[] expected2 = {VIEW, ADD};
        tests.add(
            builder.setName("Create Cap Mode 2")
                .setInput("[\"VIEW\",\"ADD\"]")
                .setExpected(expected2)
                .setTestFunction(CapabilityUtils::getCapModesFromString)
                .build()
        );

        tester.testCases("Deserialize CapabilityMode[] String", tests);
    }

    @Test
    public void getCapModeStringTest() {
        TestBuilder<CapabilityMode[], String> builder = new TestBuilder<CapabilityMode[], String>();
        JUnitTester<CapabilityMode[], String    > tester = builder.getTester(CapabilityUtilsTest.class);
        
        List<TestCase<CapabilityMode[], String>> tests = new ArrayList<>();
        CapabilityMode[] input1 = {EDIT};
        tests.add(
            builder.setName("Serialise CapabilityMode[] array 1")
                .setInput(input1)
                .setExpected("[\"EDIT\"]")
                .setTestFunction(CapabilityUtils::getModeString)
                .build()
        );
    
        // TODO: Need to figure out a better way to do this
        CapabilityMode[] input2 = {VIEW, ADD};
        tests.add(
            builder.setName("Serialise CapabilityMode[] array 2")
                .setInput(input2)
                .setExpected("[\"VIEW\",\"ADD\"]")
                .setTestFunction(CapabilityUtils::getModeString)
                .build()
        );

        tester.testCases("Serialize CapabilityMode[] array", tests);
    }
}
