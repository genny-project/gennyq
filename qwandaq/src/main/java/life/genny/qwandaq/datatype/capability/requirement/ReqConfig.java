package life.genny.qwandaq.datatype.capability.requirement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.logging.Logger;

import life.genny.qwandaq.datatype.capability.core.node.CapabilityMode;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.datatype.capability.core.node.PermissionScope;
import life.genny.qwandaq.utils.CommonUtils;

public class ReqConfig {
	private static final Logger log = Logger.getLogger(ReqConfig.class.getCanonicalName());

    // More synchronization of methods that instantiate a ReqConfig
    public static final boolean DEFAULT_ALL_CAPS = true;
    public static final boolean DEFAULT_ALL_NODES = true;
    public static final boolean DEFAULT_CASCADE_PERMS = false;
    
	/**
	 * Whether or not all Capabilities are required to pass the check (for multiple capability requirements)
	 */
    private boolean requiresAllCaps;

	/**
	 * Whether or not all capability nodes are required or at least one to pass a single capability check
	 */
    private boolean requiresAllNodes;

	/**
	 * Whether or not to cascade the permission modes on a single capability node.
	 * e.g a capabilityNode with PermissionMode of ALL and cascading permissions will have:
	 * <ul>
	 * 	<li>ALL</li>
	 *  <li>SELF</li>
	 *  <li>NONE</li>
	 * </ul>
	 */
    private boolean cascadePermissions;

	public ReqConfig() {
		this(DEFAULT_ALL_CAPS);
	}

    public ReqConfig(boolean requiresAllCaps) {
        this(requiresAllCaps, DEFAULT_ALL_NODES);
    }

    public ReqConfig(boolean requiresAllCaps, boolean requiresAllModes) {
        this(requiresAllCaps, requiresAllModes, DEFAULT_CASCADE_PERMS);
    }

    public ReqConfig(boolean requiresAllCaps, boolean requiresAllModes, boolean cascadePermissions) {
        this.requiresAllCaps = requiresAllCaps;
        this.requiresAllNodes = requiresAllModes;
        this.cascadePermissions = cascadePermissions;
    }

	// Builder instantiation
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Check a single capability's nodes if it has one or all of the given
	 * capability modes
	 * 
	 * @param userNodes - 
	 * @param checkNodes - modes to check the usernodes for, according to this RequirementsConfig
	 * @return whether or not these userNodes pass the checkModes according to this RequirementsConfig
	 * 
	 * @see {@link ReqConfig#requiresAllCaps}, {@link ReqConfig#requiresAllNodes}, {@link ReqConfig#cascadePermissions} 
	 */	
	public boolean checkCapability(Collection<CapabilityNode> userNodes, CapabilityNode... checkNodes) {
		if (checkNodes == null || checkNodes.length == 0)
			return true;

		if (cascadePermissions)
            userNodes = cascadeCapabilities(userNodes);

		if (requiresAllNodes) {
			for (CapabilityNode checkNode : checkNodes) {
				boolean hasMode = userNodes.contains(checkNode);
				if (!hasMode) {
					return (checkNode.negate);
				} else {
					// if the checkNode exists in the user nodes
					// and the checkNode is negating we don't want it 
					if(checkNode.negate)
						return false;
				}
			}

			return true;
		} else {
			for (CapabilityNode checkNode : checkNodes) {
				boolean hasMode = userNodes.contains(checkNode);
				if (hasMode && !checkNode.negate) {
					return true;
				}
			}

			log.debug("Doesn't have at least one of " + CommonUtils.getArrayString(checkNodes) + " in "
					+ CommonUtils.getArrayString(userNodes));
			return false;
		}

	}

	private static Collection<CapabilityNode> cascadeCapabilities(Collection<CapabilityNode> capSet) {
		// Allocate new list with max size of all combinations of CapMode and PermMode
		List<CapabilityNode> newCaps = new ArrayList<>(
				capSet.size() * CapabilityMode.values().length * PermissionScope.values().length);
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

    public boolean needsAllNodes() {
        return requiresAllNodes;
    }

    public boolean cascadePermissions() {
        return cascadePermissions;
    }

    public void setAllModes(boolean requiresAllModes) {
        this.requiresAllNodes = requiresAllModes;
    }

    public void setAllCaps(boolean requiresAllCaps) {
        this.requiresAllCaps = requiresAllCaps;
    }

    public String toString() {
        return new StringBuilder("[RequirementsConfig: {allCaps: ")
        .append(requiresAllCaps)
        .append(", allNodes: ")
        .append(requiresAllNodes)
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

		public Builder allNodes(boolean requiresAllNodes) {
			reqConfig.requiresAllNodes = requiresAllNodes;
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
