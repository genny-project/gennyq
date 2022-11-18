package life.genny.test.qwandaq.models.search;

import org.junit.jupiter.api.Test;

import life.genny.qwandaq.datatype.capability.core.CapabilityBuilder;
import life.genny.qwandaq.entity.search.trait.Action;
import life.genny.qwandaq.entity.search.trait.Trait;

import life.genny.qwandaq.utils.testsuite.Expected;
import life.genny.qwandaq.utils.testsuite.JUnitTester;

import static life.genny.qwandaq.datatype.capability.core.node.PermissionMode.*;

public class EqualityTests {

    @Test
    public void traitEqualityTests() {
        new JUnitTester<Trait[], Boolean>()
        .setTest((input) -> {
            return new Expected<>(input.input[0].equals(input.input[1]));
        })

        .createTest("Equality 1")
        .setInput(
            new Trait[] {new Action("EDIT", "EDIT")
                .addCapabilityRequirement(CapabilityBuilder.code("CAP_PROPERTY").add(ALL).buildCap()),
                new Action("EDIT", "EDIT")
                .addCapabilityRequirement(CapabilityBuilder.code("CAP_PROPERTY").add(ALL).buildCap())
            }
        )
        .setExpected(true)
        .build()
        
        .createTest("Equality 2")
        .setInput(
            new Trait[] {new Action("EDIT", "EDIT")
                .addCapabilityRequirement(CapabilityBuilder.code("CAP_PROPERTY").add(ALL).buildCap()),

                new Action("EDIT", "EDIT")
                .addCapabilityRequirement(CapabilityBuilder.code("CAP_PROPERTY").buildCap())
            }
        )
        .setExpected(false)
        .build()

        .createTest("Equality 3")
        .setInput(
            new Trait[] {new Action("EDIT", "EDIT")
                .addCapabilityRequirement(CapabilityBuilder.code("CAP_PROPERTY").add(ALL).buildCap()),

                new Action("EDIT", "EDIT")
                .addCapabilityRequirement(CapabilityBuilder.code("CAP_PROPERTY2").add(ALL).buildCap())
            }
        )
        .setExpected(false)
        .build()

        .createTest("Equality 4")
        .setInput(
            new Trait[] {new Action("EDIT2", "EDIT")
                .addCapabilityRequirement(CapabilityBuilder.code("CAP_PROPERTY").add(ALL).buildCap()),

                new Action("EDIT", "EDIT")
                .addCapabilityRequirement(CapabilityBuilder.code("CAP_PROPERTY").add(ALL).buildCap())
            }
        )
        .setExpected(false)
        .build()
        
        .createTest("Equality 5")
        .setInput(
            new Trait[] {new Action("EDIT", "EDIT2")
                .addCapabilityRequirement(CapabilityBuilder.code("CAP_PROPERTY").add(ALL).buildCap()),

                new Action("EDIT", "EDIT")
                .addCapabilityRequirement(CapabilityBuilder.code("CAP_PROPERTY").add(ALL).buildCap())
            }
        )
        .setExpected(false)
        .build()
        
        .createTest("Equality 6")
        .setInput(
            new Trait[] {new Action("EDIT", "EDIT")
                .addCapabilityRequirement(CapabilityBuilder.code("CAP_PROPERTY").buildCap()),

                new Action("EDIT", "EDIT")
                .addCapabilityRequirement(CapabilityBuilder.code("CAP_PROPERTY").buildCap())
            }
        )
        .setExpected(true)
        .build()
        
        .createTest("Equality 7")
        .setInput(
            new Trait[] {new Action("EDIT", "EDIT"),
                new Action("EDIT", "EDIT")
                .addCapabilityRequirement(CapabilityBuilder.code("CAP_PROPERTY").buildCap())
            }
        )
        .setExpected(false)
        .build()

        .createTest("Equality 8")
        .setInput(
            new Trait[] {
                new Action("EDIT", "EDIT"),
                new Action("EDIT", "EDIT")
            }
        )
        .setExpected(true)
        .build()

        .createTest("Equality 9")
        .setInput(
            new Trait[] {
                new Action("EDIT2", "EDIT"),
                new Action("EDIT", "EDIT")
            }
        )
        .setExpected(false)
        .build()

        .assertAll();

    }
}
