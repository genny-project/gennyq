package life.genny.test.qwandaq.json;

import org.junit.jupiter.api.Test;

import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.CapabilityBuilder;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.utils.testsuite.Expected;
import life.genny.qwandaq.utils.testsuite.JUnitTester;

import static life.genny.qwandaq.datatype.capability.core.node.PermissionMode.*;
import static life.genny.qwandaq.datatype.capability.core.node.CapabilityMode.*;

public class CapabilityAdapterTest extends SerialisationTest<Capability> {

    @Test
    public void serializationTest() {

        new JUnitTester<Capability, Capability>()
        .setTest((input) -> {
            Capability inputCap = input.input;
            String json = jsonb.toJson(inputCap);
            Capability outputCap = jsonb.fromJson(json, Capability.class);

            return new Expected<>(outputCap);
        })

        .createTest("serialisation 1")
        .setInput(CapabilityBuilder.code("CAP_PROPERTY").add(ALL).buildCap())
        .setExpected(CapabilityBuilder.code("CAP_PROPERTY").add(ALL).buildCap())
        .build()

        .createTest("serialisation 2")
        .setInput(CapabilityBuilder.code("CAP_PROPERTY").buildCap())
        .setExpected(CapabilityBuilder.code("CAP_PROPERTY").buildCap())
        .build()

        .createTest("serialisation 3")
        .setInput(CapabilityBuilder.code("CAP_PROPERTY").add(ALL).edit(SELF).buildCap())
        .setExpected(CapabilityBuilder.code("CAP_PROPERTY").add(ALL).edit(SELF).buildCap())
        .build()

        .createTest("no code Serialisation 1")
        .setInput(new Capability("", CapabilityNode.get(ADD, ALL)))
        .setExpected(new Capability("", CapabilityNode.get(ADD, ALL)))
        .build()

        .createTest("no code Serialisation 2")
        .setInput(CapabilityBuilder.code("").buildCap())
        .setExpected(CapabilityBuilder.code("").buildCap())
        .build()

        .assertAll();
    }

}
