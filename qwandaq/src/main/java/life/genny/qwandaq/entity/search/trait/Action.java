package life.genny.qwandaq.entity.search.trait;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Action
 */
@RegisterForReflection
public class Action extends Trait {

	public static final String PREFIX = "ACT_";

	public Action() {
		super();
	}

	public Action(String code, String name) {
		super(code, name);
	}

}
