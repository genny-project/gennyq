package life.genny.fyodor.utils;

import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.entity.search.clause.ClauseContainer;
import life.genny.qwandaq.entity.search.trait.Action;
import life.genny.qwandaq.entity.search.trait.Column;
import life.genny.qwandaq.entity.search.trait.Sort;
import life.genny.qwandaq.entity.search.trait.Trait;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
				.filter(this::traitCapabilitiesMet)
				.collect(Collectors.toList());

		searchEntity.setColumns(columns);
	}

	/**
	 * @param searchEntity
	 */
	public void refineSortsFromCapabilities(SearchEntity searchEntity) {

		List<Sort> sorts = searchEntity.getSorts().stream()
				.filter(this::traitCapabilitiesMet)
				.collect(Collectors.toList());

		searchEntity.setSorts(sorts);
	}

	/**
	 * @param searchEntity
	 */
	public void refineFiltersFromCapabilities(SearchEntity searchEntity) {

		// TODO: Handle filters and clauses
		List<ClauseContainer> containers = new ArrayList<>(searchEntity.getClauseContainers());

		searchEntity.setClauseContainers(containers);
	}

	/**
	 * @param searchEntity
	 */
	public void refineActionsFromCapabilities(SearchEntity searchEntity) {

		List<Action> actions = searchEntity.getActions().stream()
				.filter(this::traitCapabilitiesMet)
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
