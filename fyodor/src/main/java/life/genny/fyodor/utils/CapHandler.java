package life.genny.fyodor.utils;

import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.datatype.capability.Capability;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.entity.search.clause.ClauseContainer;
import life.genny.qwandaq.entity.search.trait.*;
import life.genny.qwandaq.managers.Manager;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.models.UserToken;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

		List<Column> columns = searchEntity.getColumns();
		info("Filtering " + columns.size() + " columns");
		columns = columns.stream()
				.filter(this::traitCapabilitiesMet)
				.collect(Collectors.toList());

		info("Filtered down to " + columns.size() + " columns");
		searchEntity.setColumns(columns);
	}

	/**
	 * @param searchEntity
	 */
	public void refineSortsFromCapabilities(SearchEntity searchEntity) {
		List<Sort> sorts = searchEntity.getSorts();
		info("Filtering " + sorts.size() + " sorts");

		sorts = sorts.stream()
				.filter(this::traitCapabilitiesMet)
				.collect(Collectors.toList());
		info("Filtered down to " + sorts.size() + " sorts");
		searchEntity.setSorts(sorts);
	}

	/**
	 * @param searchEntity
	 */
	public void refineFiltersFromCapabilities(SearchEntity searchEntity) {
		// TODO: Handle filters and clauses
		List<ClauseContainer> containers = searchEntity.getClauseContainers();
		info("Filtering " + containers.size() + " filters");
		containers = searchEntity.getClauseContainers().stream()
				// .filter(container -> traitCapabilitiesMet(container))
				.collect(Collectors.toList());

		info("Filtered down to " + containers.size() + " clause containers");
		searchEntity.setClauseContainers(containers);
	}

	/**
	 * @param searchEntity
	 */
	public void refineActionsFromCapabilities(SearchEntity searchEntity) {

		List<Action> actions = searchEntity.getActions();
		info("Filtering " + actions.size() + " actions");

		actions = actions.stream()
				.filter(this::traitCapabilitiesMet)
				.collect(Collectors.toList());

		info("Filtered down to " + actions.size() + " actions");
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

		if (GennyConstants.PER_SERVICE.equals(userToken.getUserCode()))
			return true;

		Set<Capability> capabilities = capMan.getUserCapabilities();
		for(CapabilityRequirement capTrait : trait.getCapabilityRequirements()) {
			if(!capTrait.meetsRequirements(capabilities)) {
				return false;
			}
		}
		// TODO: implement capabilities
		//
		return true;
	}

}
