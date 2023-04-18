package life.genny.qwandaq.entity.search.clause;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.exception.runtime.QueryBuilderException;

/**
 * Clause
 */
@RegisterForReflection
public class Clause implements ClauseArgument {

	private static final Logger log = Logger.getLogger(SearchEntity.class);

	private List<ClauseContainer> clauseContainers;

	public Clause() {
	}

	public Clause(ClauseArgument... clauseArguments) {

		if (clauseArguments.length <= 1)
			throw new QueryBuilderException("Clause must have at least two arguments");

		this.clauseContainers = Arrays.asList(clauseArguments).stream()
				.map(ca -> createContainer(ca))
				.collect(Collectors.toList());
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

	public List<ClauseContainer> getClauseContainers() {
		return clauseContainers;
	}

	public void setClauseContainers(List<ClauseContainer> clauseContainers) {
		this.clauseContainers = clauseContainers;
	}

	public boolean hasCapabilityRequirements() {
		for(ClauseContainer c : clauseContainers) {
			if(c.hasCapabilityRequirements())
				return true;
		}
		return false;
	}

}
