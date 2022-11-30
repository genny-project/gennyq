package life.genny.qwandaq.entity.search.trait;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.constants.Prefix;

/**
 * Action
 */
@RegisterForReflection
public class Action extends Trait {

	public static final String PREFIX = Prefix.ACT;

	public static final String VIEW = "VIEW";
	public static final String EDIT = "EDIT";

	public Action() {
		super();
	}

	public Action(String code, String name) {
		super(code, name);
	}

}
