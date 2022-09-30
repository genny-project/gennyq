package life.genny.qwandaq.entity.search.trait;

import java.util.Arrays;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.datatype.Capability;

/**
 * Capability
 */
@RegisterForReflection
public class CapabilityTrait extends Trait {

	private List<Capability> capabilities;

	public CapabilityTrait() {
		super();
	}

	public CapabilityTrait(String code, Capability... caps) {
		super(code, code);
		setCapabilities(caps);
	}

	public List<Capability> getRequirements() {
		return capabilities;
	}

	public void setCapabilities(Capability... caps) {
		this.capabilities = Arrays.asList(caps);
	}

}
