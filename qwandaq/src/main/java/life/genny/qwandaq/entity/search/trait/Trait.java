package life.genny.qwandaq.entity.search.trait;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Trait
 */
@RegisterForReflection
public class Trait {

	private String code;
	private String name;

	private Capability capability;

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

	public Capability getCapability() {
		return capability;
	}

	public void setCapability(Capability capability) {
		this.capability = capability;
	}

}
