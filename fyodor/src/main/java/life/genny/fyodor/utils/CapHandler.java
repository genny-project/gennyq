package life.genny.fyodor.utils;

import java.util.Iterator;
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

	private int filterClauseContainersBFS(SearchEntity entity, CapabilitySet userCapabilities) {
		Queue<ClauseArgument> queue = new LinkedList<>();
		ReqConfig requirementsConfig = new ReqConfig();

		int numRemoved = 0;

		// offer children of search entity, providing an initial (shallow) filter as well
		Iterator<ClauseContainer> iter = entity.getClauseContainers().iterator();
		if(iter.hasNext()) {
			log.debug("[BFS] Iterating through: " + entity);
			log.debug("\tNum Containers: " + entity.getClauseContainers().size());
			while(iter.hasNext()) {
				ClauseContainer container = iter.next();
				log.debug("[BFS] Container: " + container);
				if(container.getAnd() != null)
					queue.offer(container.getAnd());
				
				if(container.getOr() != null)
					queue.offer(container.getOr());

				if(container.getFilter() != null && !container.getFilter().requirementsMet(userCapabilities, requirementsConfig)) {
					log.debug("[BFS] Removing filter: " + container.getFilter());
					iter.remove();
					numRemoved++;
				} else if(container.getFilter() != null) {
					log.debug("[BFS] Leaving filter: " + container.getFilter().getCode());
				}
			}
		} else {
			log.error("[BFS] Received search entity: " + entity.getCode() + " that has no clause containers! Is this meant to be? Please check the relevant search caching/json of this search entity");
			return numRemoved;
		}

		ClauseArgument currentClause;
		Set<ClauseArgument> visited = new LinkedHashSet<>();

		while(!queue.isEmpty()) {
			currentClause = queue.poll();
			log.debug("[BFS] Iterating through: " + currentClause);
			if(visited.contains(currentClause)) {
				log.debug("[BFS] Already visited: " + currentClause);
				continue;
			}

			// Remove all filters from the current clause (and or or) that do not have their requirements met
			// if any children have ands or ors, visit them
			if(currentClause instanceof Clause clause && clause.hasCapabilityRequirements()) {
				iter = clause.getClauseContainers().iterator();
				if(iter.hasNext()) {
					ClauseContainer child = iter.next();
					while(iter.hasNext()) {
						// offer children of current clause
						if(child.getAnd() != null)
							queue.offer(child.getAnd());

						if(child.getOr() != null)
							queue.offer(child.getOr());

						if(child.getFilter() != null && !child.getFilter().requirementsMet(userCapabilities, requirementsConfig)) {
							log.debug("[BFS] Removing filter: " + child.getFilter().getCode());
							iter.remove();
							++numRemoved;
						} else if(child.getFilter() != null) {
							log.debug("[BFS] Leaving filter: " + child.getFilter().getCode());
						}

						child = iter.next();
					}
				}
			}

			visited.add(currentClause);
		}

		return numRemoved;
	}

	/**
	 * @param searchEntity
	 */
	public void refineClauseContainersFromCapabilities(SearchEntity searchEntity, CapabilitySet userCapabilities) {
		List<ClauseContainer> containers = searchEntity.getClauseContainers();
		log.info("Filtering from " + containers.size() + " surface level clauseContainers"); 
		int numRemoved = filterClauseContainersBFS(searchEntity, userCapabilities);
		log.info("Filtered away " + numRemoved + " filters that do not meet requirements in the clause container tree for: " + searchEntity.getCode());
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
