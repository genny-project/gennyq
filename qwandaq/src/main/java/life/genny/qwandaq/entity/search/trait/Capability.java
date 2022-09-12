package life.genny.qwandaq.entity.search.trait;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.datatype.CapabilityMode;

/**
 * Capability
 */
@RegisterForReflection
public class Capability extends Trait {

	List<CapabilityMode> modes = new ArrayList<>();

	public Capability() {
		super();
	}

	public Capability(String code, CapabilityMode... modes) {
		super(code, code);
		this.modes = Arrays.asList(modes);
	}

	public List<CapabilityMode> getModes() {
		return modes;
	}

	public void setModes(List<CapabilityMode> modes) {
		this.modes = modes;
	}

}
