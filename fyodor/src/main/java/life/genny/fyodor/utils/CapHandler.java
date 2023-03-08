package life.genny.fyodor.utils;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.datatype.capability.core.CapabilitySet;
import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;
import life.genny.qwandaq.entity.search.SearchEntity;
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
		
		if(!searchEntity.hasRequirements()) {
			log.debug("no requirements to check for searchEntity: " + searchEntity.getCode() + ". Skipping");
			return;
		}

		CapabilitySet userCapabilities = capMan.getUserCapabilities();
		refineColumnsFromCapabilities(searchEntity, userCapabilities);
		refineActionsFromCapabilities(searchEntity, userCapabilities);
		refineFiltersFromCapabilities(searchEntity, userCapabilities);
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

	/**
	 * @param searchEntity
	 */
	public void refineFiltersFromCapabilities(SearchEntity searchEntity, CapabilitySet userCapabilities) {
		List<ClauseContainer> containers = searchEntity.getClauseContainers();
		log.info("Filtering " + containers.size() + " filters"); 
		containers = containers.stream()
				.filter(container -> {
					// no filter => no capability requirements => let it through
					if(container.getFilter() == null)
						return true;
					
					log.info("Filtering " + container.getFilter().getCode());
					return container.requirementsMet(userCapabilities);
				})
				.collect(Collectors.toList());

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
		if(!isService) {
			log.info("Checking: " + trait);
			log.info("Requirements: " + CommonUtils.getArrayString(trait.getCapabilityRequirements()));
			return trait.requirementsMet(userCapabilities, reqConfig);
		} else {
			log.info("Service token. Bypassing requirements");
		}
		return true;
	}

	public static boolean hasSecureToken(UserToken userToken) {
		if(GennyConstants.PER_SERVICE.equals(userToken.getUserCode()))
			return true;
			
		if(GennyConstants.PER_SERVICE.equals(userToken.getCode()))
			return true;
		
		if(userToken.hasRole("service"))
			return true;
		
		return false;		
	}

}
