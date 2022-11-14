package life.genny.qwandaq.entity.search.trait;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;

/**
 * Capability
 */
@RegisterForReflection
public class CapabilityRequirement extends Trait {

	private Set<CapabilityNode> nodes;

	private boolean requiresAll;

	public CapabilityRequirement() {
		super();
	}

	
	public CapabilityRequirement(String code, boolean requiresAll, Set<CapabilityNode> caps) {
		super(code, code);
		this.requiresAll = requiresAll;
		setNodes(caps);
	}

	public CapabilityRequirement(String code, boolean requiresAll, CapabilityNode... caps) {
		this(code, requiresAll, new HashSet<>(Arrays.asList(caps)));
	}
	
	public boolean meetsRequirements(ReqConfig reqConfig) {
		for(Capability cap : reqConfig.userCapabilities) {
			if(!cap.checkPerms(requiresAll, nodes))
				return false;
		}

		return true;
	}

	public void setNodes(CapabilityNode... caps) {
		this.nodes = new HashSet<>(Arrays.asList(caps));
	}

	public void setNodes(Set<CapabilityNode> caps) {
		this.nodes = caps;
	}

	public static CapabilityRequirement fromCapability(Capability capability, boolean requiresAll) {
		return new CapabilityRequirement(capability.code, requiresAll, capability.nodes);
	}

}
