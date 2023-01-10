package life.genny.test.qwandaq.utils.capabilities.requirements;

import static life.genny.qwandaq.datatype.capability.core.node.PermissionMode.*;

import org.junit.jupiter.api.Test;

import life.genny.qwandaq.datatype.capability.core.CapabilityBuilder;
import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;
import life.genny.qwandaq.intf.ICapabilityFilterable;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.testsuite.JUnitTester;

public class RequirementsTest extends BaseRequirementsTest {

    // To be run by JUnit
    public RequirementsTest() {
        setTestUserCaps(
            CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap()
        );
    }

    private static final boolean[][] combinations = new boolean[][] {
        {false, false},
        {false, true},
        {true, false},
        {true, true}
    };

    private static String testName(String tag, boolean[] reqCombinations) {
        return new StringBuilder(tag)
        .append(reqCombinations[0] ? "Requires " : "Doesn't require ")
        .append("all caps, ")
        .append(reqCombinations[1] ? "Requires " : "Doesn't require ")
        .append("all modes")
        .toString();
    }

    @Test 
    public void testNoRequirementsFilterableCaps() {

        ICapabilityFilterable filterable = createFilterable();
        setTestUserCaps(
            CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap()
        );

        String[] testTags = new String[] {
            "No requirements test1 - User has Caps. ",
            "No requirements test2 - User has Caps. ",
            "No requirements test3 - User has Caps. ",
            "No requirements test4 - User has Caps. "
        };


        JUnitTester<ReqConfig, Boolean> tester = new JUnitTester<ReqConfig, Boolean>()
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(USER_TEST_CAPS, input.input));
        });

        for(int i = 0; i < testTags.length; i++) {
            tester.createTest(testName(testTags[i], combinations[i]))
            .setInput(new ReqConfig(combinations[i][0], combinations[i][1]))
            .setExpected(true)
            .build();
        }

        tester.assertAll();
    }

    @Test
    public void testNoRequirementsFilterableNoCaps() {
        ICapabilityFilterable filterable = createFilterable();
        setTestUserCaps();

        String[] testTags = new String[] {
            "No requirements test1 - User has No Caps. ",
            "No requirements test2 - User has No Caps. ",
            "No requirements test3 - User has No Caps. ",
            "No requirements test4 - User has No Caps. "
        };


        JUnitTester<ReqConfig, Boolean> tester = new JUnitTester<ReqConfig, Boolean>()
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(USER_TEST_CAPS, input.input));
        });

        for(int i = 0; i < testTags.length; i++) {
            tester.createTest(testName(testTags[i], combinations[i]))
            .setInput(new ReqConfig(combinations[i][0], combinations[i][1]))
            .setExpected(true)
            .build();
        }
        
        tester.assertAll();
    }

    @Test
    public void testNonEmptyReqsFilterable() {
        ICapabilityFilterable filterable = createFilterable(
            new CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap()
        );
        
        setTestUserCaps(
            CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap()
        );

        String[] testTags = new String[] {
            "Non Empty Reqs Filterable test 1 ",
            "Non Empty Reqs Filterable test 2 ",
            "Non Empty Reqs Filterable test 3 ",
            "Non Empty Reqs Filterable test 4 "
        };


        JUnitTester<ReqConfig, Boolean> tester = new JUnitTester<ReqConfig, Boolean>()
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(USER_TEST_CAPS, input.input));
        });

        for(int i = 0; i < testTags.length; i++) {
            tester.createTest(testName(testTags[i], combinations[i]))
            .setInput(new ReqConfig(combinations[i][0], combinations[i][1]))
            .setExpected(true)
            .build();
        }
        
        tester.assertAll();
    }

    // @Test
    // public void miscReqTests() {
    //     Set<CapabilityNode> capabilitySet = (Set<CapabilityNode>)new SetBuilder<CapabilityNode>()
    //         .add(CapabilityNode.get(CapabilityMode.EDIT, PermissionMode.ALL))
    //         .build();
    //     assertEquals(CapabilitiesManager.checkCapability(capabilitySet, false, new CapabilityNode[] {
    //         CapabilityNode.get(EDIT, ALL),
    //         CapabilityNode.get(VIEW, ALL),
    //         CapabilityNode.get(DELETE, ALL),
    //         CapabilityNode.get(ADD, ALL)
    //     }), true);
    // }

    @Test
    public void testEmptyReqsFilterable() {
        ICapabilityFilterable filterable = createFilterable();
        setTestUserCaps(
            CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap()
        );

        // 
        String[] testTags = new String[] {
            "Empty Reqs Filterable test 1 ",
            "Empty Reqs Filterable test 2 ",
            "Empty Reqs Filterable test 3 ",
            "Empty Reqs Filterable test 4 "
        };

        JUnitTester<ReqConfig, Boolean> tester = new JUnitTester<ReqConfig, Boolean>()
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(USER_TEST_CAPS, input.input));
        });

        for(int i = 0; i < testTags.length; i++) {
            tester.createTest(testName(testTags[i], combinations[i]))
            .setInput(new ReqConfig(combinations[i][0], combinations[i][1]))
            .setExpected(true)
            .build();
        }
        
        tester.assertAll();
    }

}
