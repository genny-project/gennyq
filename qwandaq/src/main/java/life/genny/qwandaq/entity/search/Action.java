package life.genny.qwandaq.entity.search;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Action
 */
@RegisterForReflection
public class Action extends Trait {

	public Action() {
		super();
	}

	public Action(String code, String name) {
		super(code, name);
	}

}
