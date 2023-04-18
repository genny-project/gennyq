package life.genny.fyodor.utils;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.datatype.capability.core.CapabilitySet;
import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.entity.search.clause.Clause;
import life.genny.qwandaq.entity.search.clause.ClauseArgument;
import life.genny.qwandaq.entity.search.clause.ClauseContainer;
import life.genny.qwandaq.entity.search.trait.Action;
import life.genny.qwandaq.entity.search.trait.Column;
import life.genny.qwandaq.entity.search.trait.Sort;
import life.genny.qwandaq.entity.search.trait.Trait;
import life.genny.qwandaq.managers.Manager;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.CommonUtils;

/**
 * CapHandler
 */
@ApplicationScoped
public class CapHandler extends Manager {

	@Inject
	Logger log;

	@Inject
	UserToken userToken;

	@Inject
	CapabilitiesManager capMan;

	/**
	 * 
	 * @param searchEntity
	 */
	public void refineSearchFromCapabilities(SearchEntity searchEntity) {
		// NOTE: This line may be a double up, but there are issues otherwise.
		if (hasSecureToken(userToken))
			return;
		
		if(!searchEntity.hasCapabilityRequirements()) {
			log.debug("no requirements to check for searchEntity: " + searchEntity.getCode() + ". Skipping");
			return;
		} else {
			log.debug("FOUND Requirements for " + searchEntity.getCode() + " generating user capabilities");
		}

		CapabilitySet userCapabilities = capMan.getUserCapabilities();
		refineColumnsFromCapabilities(searchEntity, userCapabilities);
		refineActionsFromCapabilities(searchEntity, userCapabilities);
		refineClauseContainersFromCapabilities(searchEntity, userCapabilities);
		refineSortsFromCapabilities(searchEntity, userCapabilities);

	}

	/**
	 * @param searchEntity
	 */
	public void refineColumnsFromCapabilities(SearchEntity searchEntity, CapabilitySet userCapabilities) {

		List<Column> columns = searchEntity.getTraits(Column.class);
		log.info("Filtering " + columns.size() + " columns");
		columns = columns.stream()
				.filter(column -> traitCapabilitiesMet(column, userCapabilities))
				.collect(Collectors.toList());

				log.info("Filtered down to " + columns.size() + " columns");
		searchEntity.setTraits(Column.class, columns);
	}

	/**
	 * @param searchEntity
	 */
	public void refineSortsFromCapabilities(SearchEntity searchEntity, CapabilitySet userCapabilities) {
		List<Sort> sorts = searchEntity.getTraits(Sort.class);
		log.info("Filtering " + sorts.size() + " sorts");

		sorts = sorts.stream()
				.filter(sort -> traitCapabilitiesMet(sort, userCapabilities))
				.collect(Collectors.toList());
				log.info("Filtered down to " + sorts.size() + " sorts");
		searchEntity.setTraits(Sort.class, sorts);
	}

	private void filterClauseContainerBFS(ClauseContainer container, CapabilitySet userCapabilities) {
		Queue<ClauseArgument> queue = new LinkedList<>();
		ReqConfig requirementsConfig = new ReqConfig();

		// offer children of container
		if(container.getAnd() != null)
			queue.offer(container.getAnd());
		
		if(container.getOr() != null)
			queue.offer(container.getOr());

		if(container.getFilter() != null && !container.getFilter().requirementsMet(userCapabilities, requirementsConfig)) {
			container.setFilter(null);
		}

		ClauseArgument currentClause;
		Set<ClauseArgument> visited = new LinkedHashSet<>();
		while(!queue.isEmpty()) {
			currentClause = queue.poll();
			log.trace("[BFS] Iterating through: " + currentClause);
			if(visited.contains(currentClause)) {
				log.trace("[BFS] Already visited: " + currentClause);
				continue;
			}

			if(currentClause instanceof Clause clause && clause.hasCapabilityRequirements()) {
				for(ClauseContainer child : clause.getClauseContainers()) {
					if(child.getFilter() != null && !child.getFilter().requirementsMet(userCapabilities, requirementsConfig)) {
						child.setFilter(null);
					}
				}
			}

			visited.add(currentClause);

			// offer children of container
			if(container.getAnd() != null)
				queue.offer(container.getAnd());
			
			if(container.getOr() != null)
				queue.offer(container.getOr());
		}
	}

	/**
	 * @param searchEntity
	 */
	public void refineClauseContainersFromCapabilities(SearchEntity searchEntity, CapabilitySet userCapabilities) {
		List<ClauseContainer> containers = searchEntity.getClauseContainers();
		log.info("Filtering " + containers.size() + " clauseContainers"); 
		for(ClauseContainer container : containers) {
			filterClauseContainerBFS(container, userCapabilities);
		}
		// containers = containers.stream()
		// 		.filter(container -> {
		// 			log.info("Filtering " + container.getFilter().getCode());
		// 			return container.requirementsMet(userCapabilities);
		// 		})
		// 		.collect(Collectors.toList());

		log.info("Filtered down to " + containers.size() + " filters");
		searchEntity.setClauseContainers(containers);
	}

	/**
	 * @param searchEntity
	 */
	public void refineActionsFromCapabilities(SearchEntity searchEntity, CapabilitySet userCapabilities) {

		List<Action> actions = searchEntity.getTraits(Action.class);
		log.info("Filtering " + actions.size() + " actions");
		
		actions = actions.stream()
				.filter(action -> traitCapabilitiesMet(action, userCapabilities))
				.collect(Collectors.toList());

		log.info("Filtered down to " + actions.size() + " actions");
		searchEntity.setTraits(Action.class, actions);
	}
	
	/**
	 * @param trait
	 * @param userCapabilities
	 * @return
	 */
	public boolean traitCapabilitiesMet(Trait trait, CapabilitySet userCapabilities) {
		return traitCapabilitiesMet(trait, userCapabilities, ReqConfig.builder().build());
	}

	/**
	 * @param trait
	 * @return
	 */
	public boolean traitCapabilitiesMet(Trait trait, CapabilitySet userCapabilities, ReqConfig reqConfig) {

		if(userToken == null) {
			log.error("[!] No UserToken, cannot verify capabilities");
			return false;
		}

		//  TODO: Get rid of this service code check. Not ideal
		// TODO: We also need to consolidate what it means to be a service user
		boolean isService = hasSecureToken(userToken);
		if(isService) {
			log.info("Service token. Bypassing requirements");
			return true;
		}

		log.info("Checking: " + trait);
		log.info("Requirements: " + CommonUtils.getArrayString(trait.getCapabilityRequirements()));
		return trait.requirementsMet(userCapabilities, reqConfig);
	}

	public static boolean hasSecureToken(UserToken userToken) {
		if(GennyConstants.PER_SERVICE.equals(userToken.getUserCode()))
			return true;
		
		if(userToken.hasRole("service"))
			return true;
		
		return false;		
	}

}
