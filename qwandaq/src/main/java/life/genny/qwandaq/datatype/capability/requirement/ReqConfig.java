package life.genny.qwandaq.datatype.capability.requirement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityMode;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.datatype.capability.core.node.PermissionMode;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.utils.CommonUtils;

public class ReqConfig {

    // More synchronization of methods that instantiate a ReqConfig
    public static final boolean DEFAULT_ALL_CAPS = true;
    public static final boolean DEFAULT_ALL_MODES = true;
    public static final boolean DEFAULT_CASCADE_PERMS = true;
    
    private boolean requiresAllCaps;
    private boolean requiresAllModes;
    private boolean cascadePermissions;

	public ReqConfig() {
		this(DEFAULT_ALL_CAPS);
	}

    public ReqConfig(boolean requiresAllCaps) {
        this(requiresAllCaps, DEFAULT_ALL_MODES);
    }

    public ReqConfig(boolean requiresAllCaps, boolean requiresAllModes) {
        this(requiresAllCaps, requiresAllModes, DEFAULT_CASCADE_PERMS);
    }

    public ReqConfig(boolean requiresAllCaps, boolean requiresAllModes, boolean cascadePermissions) {
        this.requiresAllCaps = requiresAllCaps;
        this.requiresAllModes = requiresAllModes;
        this.cascadePermissions = cascadePermissions;
    }

	// Builder instantiation
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Check a single EntityAttribute capability if it has one or all of given
	 * capability modes
	 * 
	 * @param capability
	 * @param hasAll
	 * @param checkModes
	 * @return
	 */
	public boolean checkCapability(EntityAttribute capability, Collection<CapabilityNode> checkModes) {
		if (StringUtils.isBlank(capability.getValueString())) {
			return false;
		}

		String modeString = capability.getValueString();
		Set<CapabilityNode> nodes = CapabilitiesManager.deserializeCapSet(modeString);

		return checkCapability(nodes, checkModes.toArray(new CapabilityNode[0]));
	}

    // public boolean checkCapability(Collection<Capability> targetNodes, Collection<CapabilityNode> checkModes) {
    //     return checkCapability(targetNodes, checkModes.toArray(new CapabilityNode[0]));
    // }

	public boolean checkCapability(Collection<CapabilityNode> userNodes, CapabilityNode... checkModes) {
		if (checkModes == null || checkModes.length == 0)
			return true;

		if (cascadePermissions)
            userNodes = cascadeCapabilities(userNodes);

		System.out.println("User Nodes: " + CommonUtils.getArrayString(userNodes));

		if (requiresAllModes) {
			for (CapabilityNode checkMode : checkModes) {
				boolean hasMode = userNodes.contains(checkMode);
				if (!hasMode) {
					return false;
				}
			}

			return true;
		} else {
			for (CapabilityNode checkMode : checkModes) {
				boolean hasMode = userNodes.contains(checkMode);
				if (hasMode) {
					return true;
				}
			}

			System.out.println("Doesn't have at least one of " + CommonUtils.getArrayString(checkModes) + " in "
					+ CommonUtils.getArrayString(userNodes));
			return false;
		}

	}

	private static Collection<CapabilityNode> cascadeCapabilities(Collection<CapabilityNode> capSet) {
		// Allocate new list with max size of all combinations of CapMode and PermMode
		List<CapabilityNode> newCaps = new ArrayList<>(
				capSet.size() * CapabilityMode.values().length * PermissionMode.values().length);
		for (CapabilityNode node : capSet) {
			newCaps.addAll(Arrays.asList(node.getLesserNodes()));
		}

		capSet.addAll(newCaps);
		return capSet;
	}

    // getters and setters

    public boolean needsAllCaps() {
        return requiresAllCaps;
    }

    public boolean needsAllModes() {
        return requiresAllModes;
    }

    public boolean cascadePermissions() {
        return cascadePermissions;
    }

    public void setAllModes(boolean requiresAllModes) {
        this.requiresAllModes = requiresAllModes;
    }

    public void setAllCaps(boolean requiresAllCaps) {
        this.requiresAllCaps = requiresAllCaps;
    }

    public String toString() {
        return new StringBuilder("[RequirementsConfig: {allCaps: ")
        .append(requiresAllCaps)
        .append(", allModes: ")
        .append(requiresAllModes)
        .append(", cascade: ")
        .append(cascadePermissions)
        .append("}")
        .append(" ]")
        .toString();
    }

	public static class Builder {
		private ReqConfig reqConfig = new ReqConfig();
		
		public Builder allCaps(boolean requiresAllCaps) {
			reqConfig.requiresAllCaps = requiresAllCaps;
			return this;
		}

		public Builder allModes(boolean requiresAllModes) {
			reqConfig.requiresAllModes = requiresAllModes;
			return this;
		}

		public Builder cascadePermissions(boolean cascadePermissions) {
			reqConfig.cascadePermissions = cascadePermissions;
			return this;
		}

		public ReqConfig build() {
			return reqConfig;
		}
	}
}
