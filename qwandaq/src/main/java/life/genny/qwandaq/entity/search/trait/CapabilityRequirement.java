package life.genny.qwandaq.entity.search.trait;

import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;
import life.genny.qwandaq.utils.CommonUtils;

/**
 * Capability
 */
@RegisterForReflection
public class CapabilityRequirement {

	private CapabilityNode[] nodes;

	private boolean requiresAllNodes;
	
	public CapabilityRequirement() {
		super();
	}

	public CapabilityRequirement(String code, boolean requiresAllNodes, Set<CapabilityNode> caps) {
		this(code, requiresAllNodes, caps.toArray(new CapabilityNode[0]));
	}

	public CapabilityRequirement(String code, boolean requiresAllNodes, CapabilityNode... caps) {
		this.requiresAllNodes = requiresAllNodes;
		nodes = caps;
		System.out.println("Attaching " + CommonUtils.getArrayString(caps) + " to Requirement: " + code);
	}
	
	

	public CapabilityNode[] getNodes() {
		return nodes;
	}

	public boolean requiresAll() {
		return requiresAllNodes;
	}

	public static CapabilityRequirement fromCapability(Capability capability, boolean requiresAll) {
		return new CapabilityRequirement(capability.code, requiresAll, capability.nodes);
	}

	public String toString() {
		return "REQ: [=" + CommonUtils.getArrayString(nodes) + "]";
	}
}
