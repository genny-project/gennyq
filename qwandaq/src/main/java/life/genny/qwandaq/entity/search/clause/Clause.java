package life.genny.qwandaq.entity.search.clause;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.entity.search.trait.Filter;

/**
 * Clause
 */
@RegisterForReflection
public class Clause implements ClauseArgument {

	private static final Logger log = Logger.getLogger(SearchEntity.class);

	private ClauseContainer a, b;

	public Clause() {
		log.info("Empty");
	}

	public Clause(ClauseArgument a, ClauseArgument b) {
		this.a = createContainer(a);
		this.b = createContainer(b);
	}

	public ClauseContainer getA() {
		return a;
	}

	public void setA(ClauseContainer a) {
		this.a = a;
	}

	public ClauseContainer getB() {
		return b;
	}

	public void setB(ClauseContainer b) {
		this.b = b;
	}

	private ClauseContainer createContainer(ClauseArgument clauseArgument) {

		ClauseContainer container = new ClauseContainer();
		if (clauseArgument instanceof And)
			container.setAnd((And) clauseArgument);
		if (clauseArgument instanceof Or)
			container.setOr((Or) clauseArgument);
		if (clauseArgument instanceof Filter)
			container.setFilter((Filter) clauseArgument);

		return container;
	}

}
