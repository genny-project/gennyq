package life.genny.test.qwandaq.utils.capabilities.requirements;

import org.junit.jupiter.api.Test;

import life.genny.qwandaq.datatype.capability.Capability;
import life.genny.qwandaq.intf.ICapabilityFilterable;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.test.utils.suite.JUnitTester;

import static life.genny.qwandaq.datatype.capability.PermissionMode.*;

import java.util.Set;

public class MultipleRequirementsTests extends BaseRequirementsTest {

    // To be run by JUnit
    public MultipleRequirementsTests() {
        setTestCaps(
            CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap(),
            CapabilityBuilder("CAP_TENANT").add(ALL).edit(SELF).buildCap()
        );
    }

    @Test
    public void multipleRequirementsTestSuccessful() {
        ICapabilityFilterable filterable = createFilterable(
            CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap(),
            CapabilityBuilder("CAP_TENANT").add(ALL).buildCap()
        );


        new JUnitTester<Set<Capability>, Boolean>()

        // Test 1
        .createTest("Multiple Non Empty Reqs Filterable test 1 REQ ALL CAPS: false, REQ ALL MODES: false")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, false, false));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(true)
        .build()

        // Test 2
        .createTest("Multiple Non Empty Reqs Filterable test 1 REQ ALL CAPS: true, REQ ALL MODES: false")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, true, false));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(true)
        .build()

        // Test 3
        .createTest("Multiple Non Empty Reqs Filterable test 1 REQ ALL CAPS: false, REQ ALL MODES: true")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, false, true));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(true)
        .build()

        // Test 4
        .createTest("Multiple Non Empty Reqs Filterable test 1 REQ ALL CAPS: true, REQ ALL MODES: true")
        .setTest((input) -> {
            return Expected(filterable.requirementsMet(input.input, true, true));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(true)
        .build()

        .assertAll();
    }

    @Test
    public void multipleRequirementsTestLockout() {
        setTestCaps(
            CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap(),
            CapabilityBuilder("CAP_TENANT").edit(SELF).buildCap()
        );
        ICapabilityFilterable filterable = createFilterable(
            CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap(),
            CapabilityBuilder("CAP_TENANT").add(ALL).buildCap()
        );


        JUnitTester<Set<Capability>, Boolean> unitTester = new JUnitTester<Set<Capability>, Boolean>()

        // Test 1
        .createTest("Lockout Multiple Non Empty Reqs Filterable test 1 REQ ALL CAPS: false, REQ ALL MODES: false")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, false, false));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(true)
        .build()

        // Test 2
        .createTest("Lockout Multiple Non Empty Reqs Filterable test 1 REQ ALL CAPS: true, REQ ALL MODES: false")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, true, false));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(false)
        .build()

        // Test 3
        .createTest("Lockout Multiple Non Empty Reqs Filterable test 1 REQ ALL CAPS: false, REQ ALL MODES: true")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, false, true));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(true)
        .build()

        // Test 4
        .createTest("Lockout Multiple Non Empty Reqs Filterable test 1 REQ ALL CAPS: true, REQ ALL MODES: true")
        .setTest((input) -> {
            return Expected(filterable.requirementsMet(input.input, true, true));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(false)
        .build()

        .assertAll();
    }

    @Test
    public void multipleRequirementsTestAllLockout() {
        setTestCaps(
            CapabilityBuilder("CAP_ADMIN").add(SELF).buildCap(),
            CapabilityBuilder("CAP_TENANT").edit(SELF).buildCap()
        );
        ICapabilityFilterable filterable = createFilterable(
            CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap(),
            CapabilityBuilder("CAP_TENANT").add(ALL).buildCap()
        );


        new JUnitTester<Set<Capability>, Boolean>()

        // Test 1
        .createTest("Force Lockout Multiple Non Empty Reqs Filterable test 1 REQ ALL CAPS: false, REQ ALL MODES: false")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, false, false));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(false)
        .build()

        // Test 2
        .createTest("Force Lockout Multiple Non Empty Reqs Filterable test 1 REQ ALL CAPS: true, REQ ALL MODES: false")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, true, false));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(false)
        .build()

        // Test 3
        .createTest("Force Lockout Multiple Non Empty Reqs Filterable test 1 REQ ALL CAPS: false, REQ ALL MODES: true")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, false, true));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(false)
        .build()

        // Test 4
        .createTest("Force Lockout Multiple Non Empty Reqs Filterable test 1 REQ ALL CAPS: true, REQ ALL MODES: true")
        .setTest((input) -> {
            return Expected(filterable.requirementsMet(input.input, true, true));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(false)
        .build()

        .assertAll();
    }
    

    @Test
    public void multipleRequirementsTestNoCaps() {
        setTestCaps();

        ICapabilityFilterable filterable = createFilterable(
            CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap(),
            CapabilityBuilder("CAP_TENANT").add(ALL).buildCap()
        );


        new JUnitTester<Set<Capability>, Boolean>()

        // Test 1
        .createTest("No Caps Multiple Reqs Filterable test 1 REQ ALL CAPS: false, REQ ALL MODES: false")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, false, false));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(false)
        .build()

        // Test 2
        .createTest("No Caps Multiple Reqs Filterable test 1 REQ ALL CAPS: true, REQ ALL MODES: false")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, true, false));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(false)
        .build()

        // Test 3
        .createTest("No Caps Multiple Reqs Filterable test 1 REQ ALL CAPS: false, REQ ALL MODES: true")
        .setTest((input) -> {
            log("Requirements: " + CommonUtils.getArrayString(filterable.getCapabilityRequirements()));
            return Expected(filterable.requirementsMet(input.input, false, true));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(false)
        .build()

        // Test 4
        .createTest("No Caps Multiple Reqs Filterable test 1 REQ ALL CAPS: true, REQ ALL MODES: true")
        .setTest((input) -> {
            return Expected(filterable.requirementsMet(input.input, true, true));
        })
        .setInput(USER_TEST_CAPS)
        .setExpected(false)
        .build()

        .assertAll();
    }
    
}
