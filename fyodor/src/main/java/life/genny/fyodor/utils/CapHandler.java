package life.genny.fyodor.utils;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.entity.search.clause.ClauseContainer;
import life.genny.qwandaq.entity.search.trait.Action;
import life.genny.qwandaq.entity.search.trait.CapabilityRequirement;
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

		List<Column> columns = searchEntity.getColumns();
		info("Filtering " + columns.size() + " columns");
		columns = columns.stream()
				.filter(column -> traitCapabilitiesMet(column))
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
				.filter(sort -> traitCapabilitiesMet(sort))
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
				.filter(action -> traitCapabilitiesMet(action))
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

		//  TODO: Get rid of this service code check. Not ideal
		boolean isService = GennyConstants.PER_SERVICE.equals(userToken.getUserCode()) || GennyConstants.PER_SERVICE.equals(userToken.getCode()) || userToken.hasRole("service");
		if(!isService) {
			ReqConfig reqConfig = capMan.getUserCapabilities();
			for(CapabilityRequirement capTrait : trait.getCapabilityRequirements()) {
				if(!capTrait.meetsRequirements(reqConfig)) {
					return false;
				}
			}
		}
		return true;
	}

}
