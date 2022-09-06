package life.genny.qwandaq.entity.search.clause;

/**
 * Clause
 */
public class Clause implements ClauseArgument {

	ClauseArgument a;
	ClauseArgument b;

	public Clause() {
	}

	public Clause(ClauseArgument a, ClauseArgument b) {
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

}
