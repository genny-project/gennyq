package life.genny.qwandaq.entity.search.clause;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Or
 */
@RegisterForReflection
public class Or extends Clause {

	public Or() {
		super();
	}

	public Or(ClauseArgument a, ClauseArgument b) {
		super(a, b);
	}

}
