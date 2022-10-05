package life.genny.qwandaq.entity.search.trait;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.datatype.capability.Capability;
import life.genny.qwandaq.datatype.capability.CapabilityNode;

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

	public CapabilityRequirement(String code, boolean requiresAll, CapabilityNode... caps) {
		super(code, code);
		this.requiresAll = requiresAll;
		setNodes(caps);
	}
	
	public boolean meetsRequirements(Set<Capability> capabilities) {
		for(Capability cap : capabilities) {
			if(!cap.checkPerms(requiresAll, nodes))
				return false;
		}

		return true;
	}

	public void setNodes(CapabilityNode... caps) {
		this.nodes = new HashSet<>(Arrays.asList(caps));
	}

}
