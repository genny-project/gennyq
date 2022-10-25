package life.genny.test.qwandaq.utils.capabilities.requirements;

import java.util.Set;

import org.junit.jupiter.api.Test;

import life.genny.qwandaq.datatype.capability.Capability;
import life.genny.qwandaq.datatype.capability.CapabilityBuilder;
import life.genny.qwandaq.intf.ICapabilityFilterable;
import life.genny.qwandaq.utils.CommonUtils;

import life.genny.test.utils.suite.JUnitTester;

import static life.genny.qwandaq.datatype.capability.PermissionMode.*;

public class RequirementsTest extends BaseRequirementsTest {

    // To be run by JUnit
    public RequirementsTest() {
        setTestCaps(
            CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap()
        );
    }

    @Test 
    public void testNoRequirementsFilterableCaps() {

        ICapabilityFilterable filterable = createFilterable();
        setTestCaps(
            CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap()
        );

        new JUnitTester<Set<Capability>, Boolean>()

        .createTest("No requirements test1 - User has Caps. Doesn't require all caps, doesn't require all modes")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, false, false));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(true)
        .build()

        .createTest("No requirements test1 - User has Caps. Requires all caps, doesn't require all modes")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, true, false));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(true)
        .build()

        .createTest("No requirements test1 - User has Caps. Doesn't require all caps, requires all modes")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, false, true));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(true)
        .build()


        .createTest("No requirements test1 - User has Caps. Requires all caps, requires all modes")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, true, true));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(true)
        .build()

        .assertAll();
    }

    @Test
    public void testNoRequirementsFilterableNoCaps() {
        ICapabilityFilterable filterable = createFilterable();
        setTestCaps();

        new JUnitTester<Set<Capability>, Boolean>()

        .createTest("No requirements test1 - User has Caps. Doesn't require all caps, doesn't require all modes")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, false, false));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(true)
        .build()

        .createTest("No requirements test1 - User has Caps. Requires all caps, doesn't require all modes")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, true, false));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(true)
        .build()

        .createTest("No requirements test1 - User has Caps. Doesn't require all caps, requires all modes")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, false, true));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(true)
        .build()


        .createTest("No requirements test1 - User has Caps. Requires all caps, requires all modes")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, true, true));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(true)
        .build()

        .assertAll();
    }

    @Test
    public void testNonEmptyReqsFilterable() {
        ICapabilityFilterable filterable = createFilterable(
            new CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap()
        );
        
        setTestCaps(
            CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap()
        );


        new JUnitTester<Set<Capability>, Boolean>()

        // Test 1
        .createTest("Non Empty Reqs Filterable test 1 REQ ALL CAPS: false, REQ ALL MODES: false")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, false, false));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(true)
        .build()

        // Test 2
        .createTest("Non Empty Reqs Filterable test 1 REQ ALL CAPS: true, REQ ALL MODES: false")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, true, false));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(true)
        .build()

        // Test 3
        .createTest("Non Empty Reqs Filterable test 1 REQ ALL CAPS: false, REQ ALL MODES: true")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, false, true));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(true)
        .build()

        // Test 4
        .createTest("Non Empty Reqs Filterable test 1 REQ ALL CAPS: true, REQ ALL MODES: true")
        .setTest((input) -> {
            return Expected(filterable.requirementsMet(input.input, true, true));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(true)
        .build()

        .assertAll();
    }

    @Test
    public void testEmptyReqsFilterable() {
        ICapabilityFilterable filterable = createFilterable();
        setTestCaps(
            CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap()
        );

        new JUnitTester<Set<Capability>, Boolean>()

        // Test 1
        .createTest("Empty Reqs Filterable test 1 REQ ALL CAPS: false, REQ ALL MODES: false")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, false, false));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(true)
        .build()

        // Test 2
        .createTest("Empty Reqs Filterable test 1 REQ ALL CAPS: true, REQ ALL MODES: false")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, true, false));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(true)
        .build()

        // Test 3
        .createTest("Empty Reqs Filterable test 1 REQ ALL CAPS: false, REQ ALL MODES: true")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, false, true));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(true)
        .build()

        // Test 4
        .createTest("Empty Reqs Filterable test 1 REQ ALL CAPS: true, REQ ALL MODES: true")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, true, true));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(true)
        .build()

        .assertAll();
    }

}
