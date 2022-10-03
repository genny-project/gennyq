package life.genny.qwandaq.entity.search.trait;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.datatype.Capability;
import life.genny.qwandaq.datatype.CapabilityNode;

/**
 * Capability
 */
@RegisterForReflection
public class CapabilityTrait extends Trait {

	private Set<CapabilityNode> capabilityRequirements;

	private boolean requiresAll;

	public CapabilityTrait() {
		super();
	}

	public CapabilityTrait(String code, boolean requiresAll, CapabilityNode... caps) {
		super(code, code);
		this.requiresAll = requiresAll;
		setCapabilityRequirements(caps);
	}
	
	public boolean meetsRequirements(Set<Capability> capabilities) {
		for(Capability cap : capabilities) {
			if(!cap.checkPerms(requiresAll, capabilityRequirements))
				return false;
		}

		return true;
	}

	public void setCapabilityRequirements(CapabilityNode... caps) {
		this.capabilityRequirements = new HashSet<>(Arrays.asList(caps));
	}

}
