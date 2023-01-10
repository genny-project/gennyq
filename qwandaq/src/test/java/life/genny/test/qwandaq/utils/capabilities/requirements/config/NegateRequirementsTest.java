package life.genny.test.qwandaq.utils.capabilities.requirements.config;

import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;
import life.genny.qwandaq.intf.ICapabilityFilterable;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.testsuite.JUnitTester;
import life.genny.test.qwandaq.utils.capabilities.requirements.BaseRequirementsTest;
import static life.genny.qwandaq.datatype.capability.core.node.PermissionMode.*;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.Test;


public class NegateRequirementsTest extends BaseRequirementsTest {

    public NegateRequirementsTest() {
        setTestUserCaps(
            CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap(),
            CapabilityBuilder("CAP_TENANT").view(ALL).buildCap()
        );
    }    
    
    @Test
    public void baseNegateTest() {
        ReqConfig reqConfig = ReqConfig.builder()
            .cascadePermissions(false)
            .build();

        new JUnitTester<ICapabilityFilterable, Boolean>()

        .setTest((input) -> {
            return Expected(input.input.requirementsMet(USER_TEST_CAPS, reqConfig));
        })

        .createTest("Negating permissions 1")
        .setInput(
            createFilterable(
                // test to make sure user *doesn't* have VIEW:ALL in CAP_TENANT
                CapabilityBuilder("CAP_TENANT").view(ALL, true).buildCap() 
            )
        )
        .setExpected(false)
        .build()

        .createTest("Negating permissions 2")
        .setInput(
            createFilterable(
                // test to make sure user *doesn't* have VIEW:SELF in CAP_TENANT
                CapabilityBuilder("CAP_TENANT").view(SELF, true).buildCap()
            )
        )
        .setExpected(true)
        .build()

        .createTest("Negating permissions 3")
        .setInput(
            createFilterable(
                CapabilityBuilder("CAP_TENANT").view(ALL, false).buildCap()
            )
        )
        .setExpected(true)
        .build()

        .createTest("Negating Permissions 4")
        .setInput(
            createFilterable(
                CapabilityBuilder("CAP_TENANT").view(ALL, true).buildCap(),
                CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap()
            )
        )
        .setExpected(false)
        .build()
        
        .assertAll();
    }

    @Test
    public void negateTestCascadingPermissions() {
        ReqConfig reqConfig = ReqConfig.builder()
            .cascadePermissions(true)
            .build();

        new JUnitTester<ICapabilityFilterable, Boolean>()
        .setTest((input) -> {
            return Expected(input.input.requirementsMet(USER_TEST_CAPS, reqConfig));
        })

        /** Check cascaded permissions are also negated (sanity test) */
        .createTest("Negate Test Cascading Permissions 1")
        .setInput(createFilterable(
            CapabilityBuilder("CAP_TENANT").view(SELF, true).buildCap()
        ))
        .setExpected(false)
        .build()

        /** Check standard permissions still work (sanity test) */
        .createTest("Negate Test Cascading Permissions 2")
        .setInput(createFilterable(
            CapabilityBuilder("CAP_ADMIN").add(SELF).buildCap()
        ))
        .setExpected(true)
        .build()

        /** Test multiple capabilities. This should fail because the user has CAP_TENANT = [VIEW:SELF]
         *  and the requirements for this negate that
         */
        .createTest("Negate Test Cascading Permissions 3 (Multiple Capabilities)")
        .setInput(createFilterable(
            CapabilityBuilder("CAP_ADMIN").add(SELF).buildCap(),
            CapabilityBuilder("CAP_TENANT").view(SELF, true).buildCap()
        ))
        .setExpected(false)
        .build()

        .assertAll();

    }

    @Test
    public void negateMultipleCapabilitiesSingleNodeTest() {
        setTestUserCaps(
            CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap(),
            CapabilityBuilder("CAP_TENANT").view(ALL).delete(ALL).buildCap()
        );
        ReqConfig reqConfig = ReqConfig.builder()
                                    .allCaps(true)
                                    .allNodes(false)
                                    .cascadePermissions(false)
                                    .build();

        new JUnitTester<ICapabilityFilterable, Boolean>()
        .setTest((input) -> {
            return Expected(input.input.requirementsMet(USER_TEST_CAPS, reqConfig));
        })

        .createTest("Negate Multiple Capabilities Single Node 1")
        /** This should fail on CAP_TENANT since the user has VIEW:ALL, however since the user also has DELETE (all)
         * and the requirements config states the user doesn't have to have all nodes, this should pass
         */
        .setInput(createFilterable(
            CapabilityBuilder("CAP_TENANT").view(ALL, true).delete(ALL).buildCap(),
            CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap()
        ))
        .setExpected(true)
        .build()

        /** Should fail on CAP_TENANT VIEW:ALL, DELETE:ALL since user has both of these capability nodes,
         * so no node should pass
         */
        .createTest("Negate Multiple Capabilities Single Node 2")
        .setInput(createFilterable(
            CapabilityBuilder("CAP_TENANT").view(ALL, true).delete(ALL, true).buildCap()
        ))
        .setExpected(false)
        .build()

        .assertAll();
    }

    @Test
    public void negateTest2() {

        setTestUserCaps(
            CapabilityBuilder("CAP_TENANT").view(SELF).buildCap()
        );

        ICapabilityFilterable filterable = createFilterable(
            // test to make sure user *doesn't* have VIEW:SELF in CAP_TENANT
            CapabilityBuilder("CAP_TENANT").view(ALL, true).buildCap() 
        );

        new JUnitTester<ReqConfig, Boolean>()
        .setTest((input) -> {
            return Expected(filterable.requirementsMet(USER_TEST_CAPS, input.input));
        })

        .createTest("Negating permissions: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()))
        .setInput(ReqConfig.builder()
                    .cascadePermissions(true)
                    .allNodes(false)
                    .build())
        .setExpected(false)
        .build()
        
        .assertAll();
    }
}
