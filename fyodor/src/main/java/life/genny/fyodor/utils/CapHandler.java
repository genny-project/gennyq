package life.genny.fyodor.utils;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.entity.search.clause.ClauseContainer;
import life.genny.qwandaq.entity.search.trait.Action;
import life.genny.qwandaq.entity.search.trait.Column;
import life.genny.qwandaq.entity.search.trait.Sort;
import life.genny.qwandaq.entity.search.trait.Trait;

/**
 * CapHandler
 */
@ApplicationScoped
public class CapHandler {

	/**
	 * @param searchEntity
	 */
	public void refineColumnsFromCapabilities(SearchEntity searchEntity) {

		List<Column> columns = searchEntity.getColumns().stream()
				.filter(column -> traitCapabilitiesMet(column))
				.collect(Collectors.toList());

		searchEntity.setColumns(columns);
	}

	/**
	 * @param searchEntity
	 */
	public void refineSortsFromCapabilities(SearchEntity searchEntity) {

		List<Sort> sorts = searchEntity.getSorts().stream()
				.filter(sort -> traitCapabilitiesMet(sort))
				.collect(Collectors.toList());

		searchEntity.setSorts(sorts);
	}

	/**
	 * @param searchEntity
	 */
	public void refineFiltersFromCapabilities(SearchEntity searchEntity) {

		// TODO: Handle filters and clauses
		List<ClauseContainer> containers = searchEntity.getClauseContainers().stream()
				// .filter(container -> traitCapabilitiesMet(container))
				.collect(Collectors.toList());

		searchEntity.setClauseContainers(containers);
	}

	/**
	 * @param searchEntity
	 */
	public void refineActionsFromCapabilities(SearchEntity searchEntity) {

		List<Action> actions = searchEntity.getActions().stream()
				.filter(action -> traitCapabilitiesMet(action))
				.collect(Collectors.toList());

		searchEntity.setActions(actions);
	}

	/**
	 * @param trait
	 * @return
	 */
	public Boolean traitCapabilitiesMet(Trait trait) {

		// TODO: implement capabilities
		return true;
	}

}
