package life.genny.qwandaq.intf;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.jboss.logging.Logger;

import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.CapabilitySet;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;

public interface ICapabilityFilterable {

    public static Logger getLogger() {
        return Logger.getLogger(ICapabilityFilterable.class);
    }
    
    // Please please PLEASE! Do not send these out
    public Set<Capability> getCapabilityRequirements();

    public default void setCapabilityRequirements(Capability... requirements) {
        setCapabilityRequirements(new HashSet<>(Arrays.asList(requirements)));
    }

    public void setCapabilityRequirements(Set<Capability> requirements);

    public default boolean requirementsMet(CapabilitySet userCapabilities, ReqConfig requirementsConfig) {
        return requirementsMetImpl(userCapabilities, getCapabilityRequirements(), requirementsConfig);
    }

    public static boolean requirementsMetImpl(CapabilitySet userCapabilities, Set<Capability> capabilityRequirements, ReqConfig requirementsConfig) {
        if(capabilityRequirements == null || capabilityRequirements.isEmpty()) {
            getLogger().info("No capabilityRequirements found!");
            return true;
        }

        boolean requiresAllCaps = requirementsConfig.needsAllCaps();
        boolean requiresAllModes = requirementsConfig.needsAllModes();
        boolean cascadePermissions = requirementsConfig.cascadePermissions();

        getLogger().info("Testing Capability Config: " + requirementsConfig);

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
            boolean passesCheck = requirementsConfig.checkCapability(cap.nodes, capabilityRequirements.toArray(new CapabilityNode[0]));
            if(!passesCheck) {
                if(requiresAllCaps) {
                    getLogger().warn("Missing cap permissions " + (requiresAllModes ? "allNodes " : "") + reqCap);
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
