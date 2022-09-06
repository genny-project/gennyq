package life.genny.qwandaq.entity.search.clause;

/**
 * Or
 */
public class Or extends Clause {

	public Or() {
		super(ClauseType.OR);
	}

	public Or(ClauseArgument a, ClauseArgument b) {
		super(a, b, ClauseType.OR);
	}
	
}
