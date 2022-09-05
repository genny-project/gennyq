package life.genny.qwandaq.entity.search;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Capability
 */
@RegisterForReflection
public class Capability extends Trait {

	public Capability() {
		super();
	}

	public Capability(String code) {
		super(code, code);
	}

}
