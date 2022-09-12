package life.genny.qwandaq.entity.search.clause;

import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Trait;

/**
 * ClauseArgument
 */
public class ClauseArgument extends Trait {

	private Filter filter;
	private And and;
	private Or or;

	public ClauseArgument() {
	}

	public ClauseArgument(Filter filter) {
		this.filter = filter;
	}

	public ClauseArgument(And and) {
		this.and = and;
	}

	public ClauseArgument(Or or) {
		this.or = or;
	}

	public Filter getFilter() {
		return filter;
	}

	public void setFilter(Filter filter) {
		this.filter = filter;
	}

	public And getAnd() {
		return and;
	}

	public void setAnd(And and) {
		this.and = and;
	}

	public Or getOr() {
		return or;
	}

	public void setOr(Or or) {
		this.or = or;
	}

}
