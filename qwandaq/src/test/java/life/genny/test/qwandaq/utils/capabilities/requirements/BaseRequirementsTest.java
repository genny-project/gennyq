package life.genny.test.qwandaq.utils.capabilities.requirements;

import static life.genny.qwandaq.datatype.capability.core.node.PermissionMode.ALL;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.CapabilityBuilder;
import life.genny.qwandaq.datatype.capability.core.CapabilitySet;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.intf.ICapabilityFilterable;
import life.genny.qwandaq.utils.testsuite.BaseTestCase;

public class BaseRequirementsTest extends BaseTestCase {
    
    protected static CapabilitySet USER_TEST_CAPS;
    private static final BaseEntity DUMMY_BASE_ENTITY = new BaseEntity("DUMMY_CAP_USER", "Idiot Capabilities User");

    static {
        setTestCaps(
            CapabilityBuilder("CAP_ADMIN").add(ALL).buildCap()
        );
    }

    protected static Set<Capability> setTestCaps(Capability... capabilities) {
        CapabilitySet capSet = new CapabilitySet(DUMMY_BASE_ENTITY, Arrays.asList(capabilities));
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

            @Override
            public void setCapabilityRequirements(Set<Capability> requirements) {
                // TODO Auto-generated method stub
            }
        };
    }

}
