package life.genny.qwandaq.entity.search.clause;

/**
 * And
 */
public class And extends Clause {

	public And() {
		super(ClauseType.AND);
	}

	public And(ClauseArgument a, ClauseArgument b) {
		super(a, b, ClauseType.AND);
	}

}
