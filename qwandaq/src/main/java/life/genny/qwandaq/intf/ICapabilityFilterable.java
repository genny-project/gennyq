package life.genny.qwandaq.intf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jboss.logging.Logger;

import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.CapabilitySet;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityMode;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;
import life.genny.qwandaq.utils.CommonUtils;

public interface ICapabilityFilterable {

    public static Logger getLogger() {
        return Logger.getLogger(ICapabilityFilterable.class);
    }
    
    public Set<Capability> getCapabilityRequirements();

    /**
     * Get the capability requirements for this filterable which contain at least one of these modes
     * @param filterModes - whether or not to filter down the modes of the found capability requirements to only the supplied
     *  modes
     * @param modes - modes to filter by
     * @return a subset of the capability requirements that contain at least one of the supplied capability modes
     */
    public default Set<Capability> getCapabilityRequirements(boolean filterModes, CapabilityMode... modes) {
        Set<Capability> requirements = getCapabilityRequirements();
        Set<Capability> filteredRequirements = new HashSet<>(requirements.size());
        
        for(Capability requirement : requirements) {
            for(CapabilityMode mode : modes) {
                if(requirement.hasNode(mode)) {
                    if(filterModes) {
                        // Grab only nodes specified in args
                        List<CapabilityNode> nodes = new ArrayList<>(requirement.nodes.size());
                        for(CapabilityNode node : requirement.nodes) {
                            if(CommonUtils.isInArray(modes, node.capMode))
                                nodes.add(node);
                        }

                        requirement = new Capability(requirement.code, nodes);
                    }

                    filteredRequirements.add(requirement);
                    break;
                }
            }
        }

        return filteredRequirements;
    }

    public default void setCapabilityRequirements(Capability... requirements) {
        setCapabilityRequirements(new HashSet<>(Arrays.asList(requirements)));
    }

    public void setCapabilityRequirements(Set<Capability> requirements);

    public default boolean requirementsMet(CapabilitySet userCapabilities) {
        return requirementsMet(userCapabilities, new ReqConfig());
    }

    public default boolean requirementsMet(CapabilitySet userCapabilities, ReqConfig requirementsConfig) {
        return requirementsMetImpl(userCapabilities, getCapabilityRequirements(), requirementsConfig);
    }

    public static boolean requirementsMetImpl(CapabilitySet userCapabilities, Set<Capability> capabilityRequirements, ReqConfig requirementsConfig) {
        if(capabilityRequirements == null || capabilityRequirements.isEmpty()) {
            getLogger().debug("No capabilityRequirements found!");
            return true;
        }

        boolean requiresAllCaps = requirementsConfig.needsAllCaps();
        boolean requiresAllModes = requirementsConfig.needsAllNodes();

        getLogger().debug("Testing Capability Config: " + requirementsConfig);

        // TODO: Can optimize this into two separate loops if necessary, to save on
        // if checks
        for(Capability reqCap : capabilityRequirements) {
            Optional<Capability> optCap = userCapabilities.parallelStream()
                .filter(cap -> cap.code.equals(reqCap.code)).findFirst();
            if(!optCap.isPresent()) {
                getLogger().warn("Could not find cap in user caps: " + reqCap.code);
                return false;
            }

            // a set of user capabilities should only have 1 entry per capability code
            Capability cap = optCap.get();

            boolean passesCheck = requirementsConfig.checkCapability(cap.nodes, 
                reqCap.nodes.toArray(new CapabilityNode[0]));
            
            // negate test
            // if reqCap has negate we have success if passesCheck is false
            // if reqCap doesn't have negate, then false (don't check pass)
            

            if(!passesCheck) {
                if(requiresAllCaps) {
                    getLogger().warn("Missing cap permissions " + reqCap);
                    getLogger().info("User perms: " + cap);
                    getLogger().info("ReqConfig: " + requirementsConfig);
                    return false;
                }
            } else {
                if(!requiresAllCaps)
                    return true;
            }
        }

        return requiresAllCaps;
    }

}
