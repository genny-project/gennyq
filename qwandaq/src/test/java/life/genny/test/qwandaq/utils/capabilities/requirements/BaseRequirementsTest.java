package life.genny.test.qwandaq.utils.capabilities.requirements;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import life.genny.qwandaq.datatype.capability.Capability;
import life.genny.qwandaq.datatype.capability.CapabilityBuilder;
import life.genny.qwandaq.intf.ICapabilityFilterable;
import life.genny.test.qwandaq.utils.BaseTestCase;

import static life.genny.qwandaq.datatype.capability.PermissionMode.*;

public class BaseRequirementsTest extends BaseTestCase {
    
    protected static Set<Capability> USER_TEST_CAPS;

    static {
        setTestCaps(
            CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap()
        );
    }

    protected static Set<Capability> setTestCaps(Capability... capabilities) {
        Set<Capability> capSet = new HashSet<>(Arrays.asList(capabilities));
        USER_TEST_CAPS = capSet;
        return capSet;
    }

    protected static CapabilityBuilder CapabilityBuilder(String capCode) {
        return new CapabilityBuilder(capCode);
    }

    protected ICapabilityFilterable createFilterable(Capability... requirementsArray) {
        Set<Capability> requirements = new HashSet<Capability>(Arrays.asList(requirementsArray));
        return new ICapabilityFilterable() {

            @Override
            public Set<Capability> getCapabilityRequirements() {
                return requirements;
            }
        };
    }

}
