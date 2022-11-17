package life.genny.fyodor;

import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.Test;
import life.genny.fyodor.utils.CapHandler;
import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.CapabilityBuilder;
import life.genny.qwandaq.datatype.capability.core.CapabilitySet;
import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.search.trait.Action;
import life.genny.qwandaq.entity.search.trait.Trait;
import life.genny.qwandaq.utils.testsuite.Expected;
import life.genny.qwandaq.utils.testsuite.JUnitTester;

import static life.genny.qwandaq.datatype.capability.core.node.PermissionMode.*;

public class CapHandlerTest {
    
    protected static CapabilitySet USER_TEST_CAPS;
    private static final BaseEntity DUMMY_BASE_ENTITY = new BaseEntity("DUMMY_CAP_USER", "Idiot Capabilities User");

    static {
        setTestCaps(
            new CapabilityBuilder("CAP_ADMIN").add(ALL).edit(ALL).view(ALL).delete(ALL).buildCap()
        );
    }

    protected static Set<Capability> setTestCaps(Capability... capabilities) {
        CapabilitySet capSet = new CapabilitySet(DUMMY_BASE_ENTITY, Arrays.asList(capabilities));
        USER_TEST_CAPS = capSet;
        return capSet;
    }

    
    @Test
    public void testPassCapHandler() {
        setTestCaps(
            new CapabilityBuilder("CAP_PROPERTY").view(ALL).edit(ALL).buildCap()
        );

        ReqConfig reqCon = new ReqConfig(USER_TEST_CAPS);
        new JUnitTester<Trait, Boolean>()
            .setTest((input) -> {
                return new Expected<>(CapHandler.traitCapabilitiesMet(reqCon, input.input));
            })
            
            .createTest("Success 1")
            .setInput(new Action("EDIT", "EDIT")
                .addCapabilityRequirement(CapabilityBuilder
                    .code("CAP_PROPERTY")
                    .edit(ALL)
                    .edit(SELF)
                    .buildCap()
                )
            )
            .setExpected(true)
            .build()

            .createTest("Success 2")
            .setInput(new Action("EDIT", "EDIT")
                .addCapabilityRequirement(CapabilityBuilder
                    .code("CAP_PROPERTY")
                    .edit(SELF)
                    .buildCap()
                )
            )
            .setExpected(true)
            .build()

            .createTest("No reqs Success 3")
            .setInput(new Action("EDIT", "EDIT")
                .addCapabilityRequirement(CapabilityBuilder
                    .code("CAP_PROPERTY")
                    .buildCap()
                )
            )
            .setExpected(true)
            .build()

            .assertAll();
    }

    @Test
    public void useCaseTests() {

        setTestCaps(
            new CapabilityBuilder("CAP_PROPERTY").view(ALL).buildCap()
        );
        ReqConfig reqCon = new ReqConfig(USER_TEST_CAPS);

        new JUnitTester<Trait, Boolean>()
            .setTest((input) -> {
                return new Expected<>(CapHandler.traitCapabilitiesMet(reqCon, input.input));
            })

            .createTest("Fail Tenant")
            .setInput(new Action("EDIT", "EDIT")
                .addCapabilityRequirement(
                    CapabilityBuilder
                        .code("CAP_PROPERTY")
                        .edit(ALL)
                        .buildCap()
                ))
            .setExpected(false)
            .build()

            .assertAll();
    }

    @Test
    public void testFailCapHandler() {

        setTestCaps(
            new CapabilityBuilder("CAP_PROPERTY").view(ALL).edit(ALL).buildCap()
        );
        ReqConfig reqCon = new ReqConfig(USER_TEST_CAPS);

        new JUnitTester<Trait, Boolean>()
            .setTest((input) -> {
                return new Expected<>(CapHandler.traitCapabilitiesMet(reqCon, input.input));
            })

            .createTest("Fail requiresall")
            .setInput(new Action("EDIT", "EDIT")
                .addCapabilityRequirement(
                    CapabilityBuilder
                        .code("CAP_PROPERTY")
                        .add(ALL).edit(ALL)
                        .buildCap()
                )
            )
            .setExpected(false)
            .build()

            .createTest("Fail missing cap")
            .setInput(new Action("EDIT", "EDIT")
                        .addCapabilityRequirement(CapabilityBuilder
                            .code("CAP_MISSINGCAP")
                            .add(ALL).buildCap()
                    )
            )
            .setExpected(false)
            .build()

            .createTest("Fail hasNone")
            .setInput(new Action("EDIT", "EDIT")
                        .addCapabilityRequirement(
                            CapabilityBuilder
                                .code("CAP_PROPERTY")
                                .add(ALL).delete(ALL)
                                .buildCap()
                        ))
            .setExpected(false)
            .build()

            .assertAll();
    }
}
