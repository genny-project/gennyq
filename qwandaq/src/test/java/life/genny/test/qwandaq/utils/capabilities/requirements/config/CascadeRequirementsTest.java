package life.genny.test.qwandaq.utils.capabilities.requirements.config;

import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;
import life.genny.qwandaq.intf.ICapabilityFilterable;
import life.genny.qwandaq.utils.testsuite.JUnitTester;
import life.genny.test.qwandaq.utils.capabilities.requirements.BaseRequirementsTest;

import static life.genny.qwandaq.datatype.capability.core.node.PermissionScope.*;

import org.junit.Test;

public class CascadeRequirementsTest extends BaseRequirementsTest {

    public CascadeRequirementsTest() {
        setTestUserCaps(
            CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap(),
            CapabilityBuilder("CAP_TENANT").view(ALL).buildCap()
        );
    }

    @Test
    public void baseCascadeTest() {
        ICapabilityFilterable filterable = createFilterable(
            CapabilityBuilder("CAP_TENANT").view(SELF).buildCap()
        );

        new JUnitTester<ReqConfig, Boolean>()

        .setTest((input) -> {
            return Expected(filterable.requirementsMet(USER_TEST_CAPS, input.input));
        })

        .createTest("Cascading permissions: false")
        .setInput(ReqConfig.builder()
                    .cascadePermissions(true)
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
