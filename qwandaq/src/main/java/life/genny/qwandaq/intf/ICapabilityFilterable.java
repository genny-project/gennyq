package life.genny.qwandaq.intf;

import java.util.Optional;
import java.util.Set;

import javax.json.bind.annotation.JsonbTransient;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import life.genny.qwandaq.datatype.capability.Capability;

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
                getLogger().warn("Could not find cap in user caps: " + reqCap.code);
                return false;
            }
            // a set of user capabilities should only have 1 entry per capability code
            if(!optCap.get().checkPerms(requiresAllModes, reqCap)) {
                if(requiresAllCaps) {
                    getLogger().warn("Missing cap permissions " + (requiresAllModes ? "allModes " : "") + reqCap);
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


}
