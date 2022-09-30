package life.genny.qwandaq.entity.search.trait;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Trait
 */
@RegisterForReflection
public abstract class Trait {

	private String code;
	private String name;

	private List<CapabilityTrait> capabilities;

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

	public List<CapabilityTrait> getCapabilities() {
		return capabilities;
	}

	public void setCapabilities(List<CapabilityTrait> capabilities) {
		this.capabilities = capabilities;
	}

	public Trait add(CapabilityTrait capability) {
		this.capabilities.add(capability);
		return this;
	}

}
