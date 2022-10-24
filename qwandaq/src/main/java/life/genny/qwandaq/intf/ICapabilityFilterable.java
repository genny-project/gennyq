package life.genny.qwandaq.intf;

import java.util.Optional;
import java.util.Set;

import javax.json.bind.annotation.JsonbTransient;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import life.genny.qwandaq.datatype.capability.Capability;
import life.genny.qwandaq.utils.CommonUtils;

public interface ICapabilityFilterable {
    
    // Please please PLEASE! Do not send these out
    @JsonbTransient
    @JsonIgnore
    public Set<Capability> getCapabilityRequirements();

    public default Logger getLogger() {
        return Logger.getLogger(ICapabilityFilterable.class);
    }

    /**
     * 
     * @param userCaps
     * @param requiresAllCaps
     * @return
     */
    public default boolean requirementsMet(Set<Capability> userCaps, boolean requiresAllCaps) {
        return requirementsMet(userCaps, requiresAllCaps, true);
    }

    /**
     * 
     * @param userCapabilities
     * @param requiresAllCaps
     * @param requiresAllModes
     * @return
     */
    public default boolean requirementsMet(Set<Capability> userCapabilities, boolean requiresAllCaps, boolean requiresAllModes) {
        Set<Capability> checkCaps = getCapabilityRequirements();

        // foreach capability in the object requirements
            // scan through user capabilities to find capability with same code
            // check the nodes

        // TODO: Can optimize this into two separate loops if necessary, to save on
        // if checks
        for(Capability reqCap : checkCaps) {
            Optional<Capability> optCap = userCapabilities.parallelStream()
                .filter(cap -> cap.code.equals(reqCap.code)).findFirst();
            if(!optCap.isPresent()) {
                System.err.println("Could not find cap in user caps: " + reqCap.code);
                return false;
            }
            System.out.println("Checking " + optCap.get().code);
            // a set of user capabilities should only have 1 entry per capability code
            if(!optCap.get().checkPerms(requiresAllModes, reqCap)) {
                if(requiresAllCaps) {
                    getLogger().warn("Capability requirements: " + CommonUtils.getArrayString(checkCaps));
                    return false;
                }
            } else {
                if(!requiresAllCaps)
                    return true;
            }

        }

        getLogger().warn("Capability requirements: " + CommonUtils.getArrayString(checkCaps));
		return false;
    }


}
