package life.genny.qwandaq.entity.search.trait;

import java.util.Set;

import javax.json.bind.annotation.JsonbTypeAdapter;

import java.util.HashSet;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.intf.ICapabilityFilterable;
import life.genny.qwandaq.serialization.adapters.CapabilityAdapter;

/**
 * Trait
 */
@RegisterForReflection
public abstract class Trait implements ICapabilityFilterable {

	private String code;
	private String name;

	// @JsonbTransient
	@JsonbTypeAdapter(CapabilityAdapter.class)
	private Set<Capability> capReqs = new HashSet<>();

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

	public Set<Capability> getCapabilityRequirements() {
		return capReqs;
	}

	public void setCapabilityRequirements(Set<Capability>  capabilities) {
		this.capReqs = capabilities;
	}

	public Trait addCapabilityRequirement(Capability capability) {
		this.capReqs.add(capability);
		return this;
	}

	public Trait addCapabilityRequirement(String code, CapabilityNode... nodes) {
		Capability req = new Capability(code, nodes);
		return addCapabilityRequirement(req);
	}
}
