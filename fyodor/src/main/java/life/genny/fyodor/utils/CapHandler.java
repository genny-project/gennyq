package life.genny.fyodor.utils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import life.genny.qwandaq.datatype.capability.Capability;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.entity.search.clause.ClauseContainer;
import life.genny.qwandaq.entity.search.trait.Action;
import life.genny.qwandaq.entity.search.trait.CapabilityTrait;
import life.genny.qwandaq.entity.search.trait.Column;
import life.genny.qwandaq.entity.search.trait.Sort;
import life.genny.qwandaq.entity.search.trait.Trait;
import life.genny.qwandaq.managers.Manager;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.models.UserToken;

/**
 * CapHandler
 */
@ApplicationScoped
public class CapHandler extends Manager {

	@Inject
	UserToken userToken;

	@Inject
	CapabilitiesManager capMan;

	/**
	 * @param searchEntity
	 */
	public void refineColumnsFromCapabilities(SearchEntity searchEntity) {
		info("Filtering columns");

		List<Column> columns = searchEntity.getColumns().stream()
				.filter(column -> traitCapabilitiesMet(column))
				.collect(Collectors.toList());

		searchEntity.setColumns(columns);
	}

	/**
	 * @param searchEntity
	 */
	public void refineSortsFromCapabilities(SearchEntity searchEntity) {
		info("Filtering sorts");

		List<Sort> sorts = searchEntity.getSorts().stream()
				.filter(sort -> traitCapabilitiesMet(sort))
				.collect(Collectors.toList());

		searchEntity.setSorts(sorts);
	}
	
	/**
	 * @param searchEntity
	 */
	public void refineFiltersFromCapabilities(SearchEntity searchEntity) {
		info("Filtering filters");
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
		info("Filtering actions");

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
		if(userToken == null) {
			error("[!] No UserToken, cannot verify capabilities");
			return false;
		}

		Set<Capability> capabilities = capMan.getUserCapabilities();
		for(CapabilityTrait capTrait : trait.getCapabilityRequirements()) {
			if(!capTrait.meetsRequirements(capabilities)) {
				return false;
			}
		}
		// TODO: implement capabilities
		return true;
	}

}
