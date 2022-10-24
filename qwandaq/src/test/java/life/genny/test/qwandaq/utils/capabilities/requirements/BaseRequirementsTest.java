package life.genny.test.qwandaq.utils.capabilities.requirements;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
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

            public boolean requirementsMet(Set<Capability> userCapabilities, boolean requiresAllCaps, boolean requiresAllModes) {
                Set<Capability> checkCaps = getCapabilityRequirements();

                if(checkCaps.isEmpty())
                    return true;
        
                // foreach capability in the object requirements
                    // scan through user capabilities to find capability with same code
                    // check the nodes
        
                // TODO: Can optimize this into two separate loops if necessary, to save on
                // if checks
                for(Capability reqCap : checkCaps) {
                    Optional<Capability> optCap = userCapabilities.parallelStream()
                        .filter(cap -> cap.code.equals(reqCap.code)).findFirst();
                    if(!optCap.isPresent()) {
                        error("Could not find cap in user caps: " + reqCap.code);
                        return false;
                    }
                    // a set of user capabilities should only have 1 entry per capability code
                    if(!optCap.get().checkPerms(requiresAllModes, reqCap)) {
                        if(requiresAllCaps) {
                            error("Missing cap permissions " + (requiresAllModes ? "allModes " : "") + reqCap);
                            return false;
                        }
                    } else {
                        if(!requiresAllCaps)
                            return true;
                    }
                }

                if(requiresAllCaps) {
                    return true;
                } else
                    return false;
            }
        
        };
    }

}
