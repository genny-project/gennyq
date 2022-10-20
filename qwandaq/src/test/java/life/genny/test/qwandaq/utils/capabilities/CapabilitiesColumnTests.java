package life.genny.test.qwandaq.utils.capabilities;

import org.junit.jupiter.api.Test;

import life.genny.qwandaq.converter.CapabilityConverter;
import life.genny.qwandaq.datatype.capability.Capability;
import life.genny.qwandaq.datatype.capability.CapabilityBuilder;
import life.genny.test.qwandaq.utils.BaseTestCase;
import life.genny.test.utils.suite.JUnitTester;

import static life.genny.qwandaq.datatype.capability.PermissionMode.*;

import static life.genny.test.utils.suite.TestCase.Expected;

public class CapabilitiesColumnTests extends BaseTestCase {
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
    public void serialize() {
        new JUnitTester<Capability, String>()
        .setTest((input) -> {
            return new Expected<>(CapTester.convertToDatabaseColumn(input.input));
        })
        .createTest("Serialize Database Capability 1")
        .setInput(new CapabilityBuilder("CAP_ADMIN").add(ALL).edit(SELF).buildCap())
        .setExpected("CAP_ADMIN[\"A:A\",\"E:S\"]")
        .build()

        .createTest("Serialize Database Capability 2")
        .setInput(new CapabilityBuilder("CAP_TEST_CAP").buildCap())
        .setExpected("CAP_TEST_CAP[]")
        .build()

        .assertAll();
    }

    @Test
    public void deserialize() {
        new JUnitTester<String, Capability>()
        .setTest((input) -> {
            return new Expected<>(CapTester.convertToEntityAttribute(input.input));
        })
        .createTest("Serialize Database Capability 1")
        .setInput("CAP_ADMIN[\"A:A\",\"E:S\"]")
        .setExpected(new CapabilityBuilder("CAP_ADMIN").add(ALL).edit(SELF).buildCap())
        .build()

        .createTest("Serialize Database Capability 2")
        .setInput("CAP_TEST_CAP[]")
        .setExpected(new CapabilityBuilder("CAP_TEST_CAP").buildCap())
        .build()

        .assertAll();
    }
}
