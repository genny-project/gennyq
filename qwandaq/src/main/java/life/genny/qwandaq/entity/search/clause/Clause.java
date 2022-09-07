package life.genny.qwandaq.entity.search.clause;

/**
 * Clause
 */
public abstract class Clause extends ClauseArgument {

	ClauseArgument a, b;
	
	protected ClauseType type;

	public static enum ClauseType {
		AND,
		OR
	}

	public Clause() {
	}

	public Clause(ClauseType type) {
	}

	public Clause(ClauseArgument a, ClauseArgument b, ClauseType type) {
		this.a = a;
		this.b = b;
	}

	public ClauseArgument getA() {
		return a;
	}

	public void setA(ClauseArgument a) {
		this.a = a;
	}

	public ClauseArgument getB() {
		return b;
	}

	public void setB(ClauseArgument b) {
		this.b = b;
	}

	public ClauseType getType() {
		return type;
	}

	public void setType(ClauseType type) {
		this.type = type;
	}

}
