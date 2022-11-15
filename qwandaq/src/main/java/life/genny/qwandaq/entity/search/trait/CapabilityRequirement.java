package life.genny.qwandaq.entity.search.trait;

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
public class CapabilityRequirement extends Trait {

	private CapabilityNode[] nodes;

	private boolean requiresAll;
	
	public CapabilityRequirement() {
		super();
	}

	public CapabilityRequirement(String code, boolean requiresAll, Set<CapabilityNode> caps) {
		this(code, requiresAll, caps.toArray(new CapabilityNode[0]));
	}

	public CapabilityRequirement(String code, boolean requiresAll, CapabilityNode... caps) {
		super(code, code);
		this.requiresAll = requiresAll;
		nodes = caps;
		System.out.println("Attaching " + CommonUtils.getArrayString(caps) + " to Requirement: " + code);
	}
	
	public boolean meetsRequirements(ReqConfig reqConfig) {
		// If no requirements then the requirements are met!
		if(nodes == null)
			return true;
		
		if(reqConfig.needsAllCaps()) {
			for(Capability cap : reqConfig.userCapabilities) {
				if(!cap.checkPerms(requiresAll, nodes))
					return false;
			}
			return true;
		} else {
			for(Capability cap : reqConfig.userCapabilities) {
				if(cap.checkPerms(requiresAll, nodes)) {
					return true;
				}
			}
			return false;
		}

	}

	public CapabilityNode[] getNodes() {
		return nodes;
	}

	public boolean requiresAll() {
		return requiresAll;
	}

	public static CapabilityRequirement fromCapability(Capability capability, boolean requiresAll) {
		return new CapabilityRequirement(capability.code, requiresAll, capability.nodes);
	}

	public String toString() {
		return "REQ: [" + getCode() + "=" + CommonUtils.getArrayString(nodes) + "]";
	}
}
