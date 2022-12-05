package life.genny.qwandaq.intf;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.jboss.logging.Logger;

import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;

public interface ICapabilityFilterable {

    public static Logger getLogger() {
        return Logger.getLogger(ICapabilityFilterable.class);
    }
    
    public Set<Capability> getCapabilityRequirements();

    public default void setCapabilityRequirements(Capability... requirements) {
        setCapabilityRequirements(new HashSet<>(Arrays.asList(requirements)));
    }

    public void setCapabilityRequirements(Set<Capability> requirements);

    public default boolean requirementsMet(ReqConfig requirementsConfig) {
        return requirementsMetImpl(getCapabilityRequirements(), requirementsConfig);
    }

    public static boolean requirementsMetImpl(Set<Capability> capabilityRequirements, ReqConfig requirementsConfig) {
        Set<Capability> checkCaps = capabilityRequirements;

        if(checkCaps == null || checkCaps.isEmpty()) {
            getLogger().debug("No capabilityRequirements found!");
            return true;
        }

        Set<Capability> userCapabilities = requirementsConfig.getUserCaps();
        boolean requiresAllCaps = requirementsConfig.needsAllCaps();
        boolean requiresAllModes = requirementsConfig.needsAllModes();

        getLogger().debug("Testing Capability Config: { AllCaps: " + requiresAllCaps + ", AllModes: " + requiresAllModes + "}");

        // TODO: Can optimize this into two separate loops if necessary, to save on
        // if checks
        for(Capability reqCap : checkCaps) {
            Optional<Capability> optCap = userCapabilities.parallelStream()
                .filter(cap -> cap.code.equals(reqCap.code)).findFirst();
            if(!optCap.isPresent()) {
                getLogger().warn("Could not find cap in user caps: " + reqCap.code);
                return false;
            }
            // a set of user capabilities should only have 1 entry per capability code
            if(!optCap.get().checkPerms(requiresAllModes, reqCap.nodes)) {
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
