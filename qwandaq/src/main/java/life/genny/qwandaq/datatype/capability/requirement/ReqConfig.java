package life.genny.qwandaq.datatype.capability.requirement;

import java.util.Set;

import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.CapabilitySet;

public class ReqConfig {

    // More synchronization of methods that instantiate a ReqConfig
    public static final boolean DEFAULT_ALL_CAPS = true;
    public static final boolean DEFAULT_ALL_MODES = true;
    
    private boolean requiresAllCaps;
    private boolean requiresAllModes;
    public final CapabilitySet userCapabilities;

    public ReqConfig(CapabilitySet userCapabilities) {
        this(userCapabilities, DEFAULT_ALL_CAPS);
    }

    public ReqConfig(CapabilitySet userCapabilities, boolean requiresAllCaps) {
        this(userCapabilities, requiresAllCaps, DEFAULT_ALL_MODES);
    }

    public ReqConfig(CapabilitySet userCapabilities, boolean requiresAllCaps, boolean requiresAllModes) {
        this.userCapabilities = userCapabilities;
        this.requiresAllCaps = requiresAllCaps;
        this.requiresAllModes = requiresAllModes;
    }

    // getters and setters

    public boolean needsAllCaps() {
        return requiresAllCaps;
    }

    public boolean needsAllModes() {
        return requiresAllModes;
    }

    public void setAllModes(boolean requiresAllModes) {
        this.requiresAllModes = requiresAllModes;
    }

    public void setAllCaps(boolean requiresAllCaps) {
        this.requiresAllCaps = requiresAllCaps;
    }

    /**
     * Get the set of capabilities belonging to the user of this ReqConfig object
     * @return
     */
    public Set<Capability> getUserCaps() {
        return userCapabilities;
    }

    public String toString() {
        return new StringBuilder("[RequirementsConfig: {allCaps: ")
        .append(requiresAllCaps)
        .append(", allModes: ")
        .append(requiresAllModes)
        .append("} \n")
        .append(userCapabilities.toString())
        .append(" ]")
        .toString();
    }
}
