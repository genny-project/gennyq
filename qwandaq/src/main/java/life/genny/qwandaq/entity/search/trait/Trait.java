package life.genny.qwandaq.entity.search.trait;

import java.util.HashSet;
import java.util.Set;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Trait
 */
@RegisterForReflection
public abstract class Trait {

	private String code;
	private String name;

	private Set<CapabilityTrait> capabilityRequirements = new HashSet<>();

	public Trait() {
	}

	public Trait(String code, String name) {
		this.code = code;
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<CapabilityTrait> getCapabilityRequirements() {
		return capabilityRequirements;
	}

	public void setCapabilityRequirements(Set<CapabilityTrait> capabilities) {
		this.capabilityRequirements = capabilities;
	}

	public Trait addCapabilityRequirement(CapabilityTrait capability) {
		this.capabilityRequirements.add(capability);
		return this;
	}

}
