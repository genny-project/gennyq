package life.genny.test.qwandaq.utils.capabilities;

import org.junit.jupiter.api.Test;

import life.genny.qwandaq.converter.CapabilityConverter;
import life.genny.qwandaq.datatype.capability.Capability;
import life.genny.qwandaq.datatype.capability.CapabilityBuilder;
import life.genny.test.utils.callbacks.test.FITestCallback;
import life.genny.test.utils.suite.TestCase;

import static life.genny.qwandaq.datatype.capability.PermissionMode.*;

import static life.genny.test.utils.suite.TestCase.Builder;
import static life.genny.test.utils.suite.TestCase.Input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.List;

import static life.genny.test.utils.suite.TestCase.Expected;

public class CapabilitiesColumnTests {
    private static class CapTester {
        public static CapabilityConverter convert = new CapabilityConverter();

        public static String convertToDatabaseColumn(Capability attribute) {
            return convert.convertToDatabaseColumn(attribute);
        }
    
        public static Capability convertToEntityAttribute(String dbData) {
            return convert.convertToEntityAttribute(dbData);
        }
    }

    @Test
    public void deserialize() {
        Builder<Capability, String> builder = new Builder<>();

        FITestCallback<Input<Capability>, Expected<String>> testFunction = (input) -> {
            return new Expected<>(CapTester.convertToDatabaseColumn(input.input));
        };
        
        List<TestCase<Capability, String>> tests = new ArrayList<>();
        tests.add(
            builder.setName("Deserialize Database Capability 1")
                    .setInput(new CapabilityBuilder("CAP_ADMIN").add(ALL).edit(SELF).buildCap())
                    .setExpected("CAP_ADMIN[\"A:A\",\"E:S\"]")
                    .setTest(testFunction)
                    .build()
        );

        tests.add(
            builder.setName("Deserialize Database Capability 2")
                    .setInput(new CapabilityBuilder("CAP_TEST_CAP").buildCap())
                    .setExpected("CAP_TEST_CAP[]")
                    .setTest(testFunction)
                    .build()
        );

        for(TestCase<Capability, String> test : tests) {
            assertEquals(test.getExpected(), test.test());
        }
    }

    @Test
    public void serialize() {
        Builder<String, Capability> builder = new Builder<>();

        FITestCallback<Input<String>, Expected<Capability>> testFunction = (input) -> {
            return new Expected<>(CapTester.convertToEntityAttribute(input.input));
        };
        
        List<TestCase<String, Capability>> tests = new ArrayList<>();
        tests.add(
            builder.setName("Serialize Database Capability 1")
                    .setInput("CAP_ADMIN[\"A:A\",\"E:S\"]")
                    .setExpected(new CapabilityBuilder("CAP_ADMIN").add(ALL).edit(SELF).buildCap())
                    .setTest(testFunction)
                    .build()
        );

        tests.add(
            builder.setName("Serialize Database Capability 2")
                    .setInput("CAP_TEST_CAP[]")
                    .setExpected(new CapabilityBuilder("CAP_TEST_CAP").buildCap())
                    .setTest(testFunction)
                    .build()
        );

        for(TestCase<String, Capability> test : tests) {
            assertEquals(test.getExpected(), test.test());
        }
    }
}
