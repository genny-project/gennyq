package life.genny.test.qwandaq.utils.capabilities.requirements.config;

import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;
import life.genny.qwandaq.intf.ICapabilityFilterable;
import life.genny.qwandaq.utils.testsuite.JUnitTester;
import life.genny.test.qwandaq.utils.capabilities.requirements.BaseRequirementsTest;

import static life.genny.qwandaq.datatype.capability.core.node.PermissionMode.*;

import org.junit.Test;

public class CascadeRequirementsTest extends BaseRequirementsTest {

    public CascadeRequirementsTest() {
        setTestCaps(
            CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap(),
            CapabilityBuilder("CAP_TENANT").add(SELF).buildCap()
        );
    }

    @Test
    public void baseCascadeTest() {
        ICapabilityFilterable filterable = createFilterable(
            CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap(),
            CapabilityBuilder("CAP_TENANT").add(ALL).buildCap()
        );

        new JUnitTester<ReqConfig, Boolean>()

        .setTest((input) -> {
            return Expected(filterable.requirementsMet(USER_TEST_CAPS, input.input));
        })

        .createTest("Cascading permissions: false")
        .setInput(ReqConfig.builder()
                    .cascadePermissions(false)
                    .build()
        )
        .setExpected(true)
        .build()

        .assertAll();
    }

    @Test
    public void basicNonCascadeTest() {

    }
}
