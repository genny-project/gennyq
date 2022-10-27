package life.genny.test.qwandaq.utils.capabilities;

import org.junit.jupiter.api.Test;

import life.genny.qwandaq.converter.CapabilityConverter;
import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.CapabilityBuilder;
import life.genny.qwandaq.utils.SetBuilder;
import life.genny.test.qwandaq.utils.BaseTestCase;
import life.genny.test.utils.suite.JUnitTester;

import static life.genny.qwandaq.datatype.capability.core.node.PermissionMode.*;

import java.util.Set;

public class CapabilitiesColumnTests extends BaseTestCase {
    private static class CapTester {
        public static CapabilityConverter convert = new CapabilityConverter();

        public static String convertToDatabaseColumn(Set<Capability> attribute) {
            return convert.convertToDatabaseColumn(attribute);
        }
    
        public static Set<Capability> convertToEntityAttribute(String dbData) {
            return convert.convertToEntityAttribute(dbData);
        }
    }

    @Test
    public void serialize() {
        new JUnitTester<Set<Capability>, String>()
        .setTest((input) -> {
            return Expected(CapTester.convertToDatabaseColumn(input.input));
        })
        .createTest("Serialize Database Capability 1")
        .setInput(
            new SetBuilder<Capability>()
            .add(new CapabilityBuilder("CAP_ADMIN").add(ALL).edit(SELF).buildCap())
            .build()
        )
        .setExpected("CAP_ADMIN[\"A:A\",\"E:S\"]")
        .build()

        .createTest("Serialize Database Capability 2")
        .setInput(
            new SetBuilder<Capability>()
            .add(new CapabilityBuilder("CAP_TEST_CAP").buildCap())
            .add(new CapabilityBuilder("CAP_TEST_CAP2").add(ALL).buildCap())
            .build()
        )
        .setExpected("CAP_TEST_CAP[]  CAP_TEST_CAP2[\"A:A\"]")
        .build()

        .assertAll();
    }

    @Test
    public void deserialize() {
        new JUnitTester<String, Set<Capability>>()
        .setTest((input) -> {
            return Expected(CapTester.convertToEntityAttribute(input.input));
        })
        .createTest("Serialize Database Capability 1")
        .setInput("CAP_ADMIN[\"A:A\",\"E:S\"]  CAP_ADMIN2[\"A:S\",\"E:N\"]")
        .setExpected(
            new SetBuilder<Capability>()
            .add(new CapabilityBuilder("CAP_ADMIN").add(ALL).edit(SELF).buildCap())
            .add(new CapabilityBuilder("CAP_ADMIN2").add(SELF).edit(NONE).buildCap())
            .build()
        )
        .build()

        .createTest("Serialize Database Capability 2")
        .setInput("CAP_TEST_CAP[]")
        .setExpected(
            new SetBuilder<Capability>()
            .add(new CapabilityBuilder("CAP_TEST_CAP").buildCap())
            .build()
        )
        .build()

        .assertAll();
    }
}
