package life.genny.kogito.common.service;

import static life.genny.kogito.common.utils.KogitoUtils.UseService.GADAQ;
import static life.genny.qwandaq.attribute.Attribute.PRI_NAME;
import static life.genny.qwandaq.entity.PCM.PCM_CONTENT;
import static life.genny.qwandaq.entity.PCM.PCM_PROCESS;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import life.genny.qwandaq.constants.FilterConst;
import life.genny.qwandaq.managers.CacheManager;
import org.jboss.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;

import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.PCM;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.entity.search.trait.Ord;
import life.genny.qwandaq.entity.search.trait.Sort;

@ApplicationScoped
public class SearchService extends KogitoService {

	@Inject
	Logger log;

	@Inject
	CacheManager cacheManager;

	/**
	 * Perform a Bucket search.
	 */
	public void sendBuckets() {
		String userCode = userToken.getUserCode();

		JsonObject payload = Json.createObjectBuilder()
				.add("sourceCode", userCode)
				.add("targetCode", userCode)
				.add("pcmCode", PCM_PROCESS)
				.add("parent", PCM_CONTENT)
				.add("location", PCM.location(1))
				.build();

		kogitoUtils.triggerWorkflow(GADAQ, "processQuestions", payload);
	}

	/**
	 * Perform a Detail View search.
	 *
	 * @param code The code of the target to display
	 */
	public void sendTable(String code) {

		code = StringUtils.replaceOnce(code, Prefix.QUE_, Prefix.PCM_);
		log.info("Sending Table :: " + code);

		String userCode = userToken.getUserCode();
		JsonObject payload = Json.createObjectBuilder()
				.add("sourceCode", userCode)
				.add("targetCode", userCode)
				.add("pcmCode", code)
				.add("parent", PCM_CONTENT)
				.add("location", PCM.location(1))
				.build();

		kogitoUtils.triggerWorkflow(GADAQ, "processQuestions", payload);
	}

	/**
	 * Perform a Detail View search.
	 *
	 * @param targetCode The code of the target to display
	 */
	public void sendDetailView(String targetCode) {

		// fetch target and find it's definition
		BaseEntity target = beUtils.getBaseEntity(targetCode, true);
		BaseEntity definition = defUtils.getDEF(target);
		String type = StringUtils.removeStart(definition.getCode(), Prefix.DEF_);

		// construct template and question codes from type
		String pcmCode = new StringBuilder(Prefix.PCM_).append(type).append("_DETAIL_VIEW").toString();

		// send pcm with correct info
		String userCode = userToken.getUserCode();

		JsonObject payload = Json.createObjectBuilder()
				.add("sourceCode", userCode)
				.add("targetCode", targetCode)
				.add("pcmCode", pcmCode)
				.add("parent", PCM_CONTENT)
				.add("location", PCM.location(1))
				.build();

		kogitoUtils.triggerWorkflow(GADAQ, "processQuestions", payload);
	}

	/**
	 * Perform a named search from search bar
	 *
	 * @param code
	 * @param nameWildcard
	 */
	public void sendNameSearch(String code, String nameWildcard) {
		log.info("Sending Name Search :: " + code);
		// get sbe code from cache if code is empty
		if(code.isEmpty()) {
			String cachedCode = FilterConst.LAST_SBE_TABLE + ":" + userToken.getUserCode();
			code = cacheManager.getObject(userToken.getProductCode(),cachedCode,String.class);
		}
		// find in cache
		SearchEntity searchEntity = cacheManager.getObject(userToken.getProductCode(),
				code, SearchEntity.class);
		// TODO: remove this char from alyson
		nameWildcard = StringUtils.removeStart(nameWildcard, "!");
		// add filter on name and resend search
		searchEntity.add(new Filter(PRI_NAME, Operator.LIKE, "%"+nameWildcard+"%"));
		searchUtils.searchTable(searchEntity);
	}

	/**
	 * Handle a pagination event.
	 *
	 * @param code
	 * @param reverse
	 */
	public void handleSearchPagination(String code, Boolean reverse) {
		// fetch search from cache
		String sessionCode = searchUtils.sessionSearchCode(code);
		SearchEntity searchEntity = cacheManager.getObject(userToken.getProductCode(), "LAST-SEARCH:" + sessionCode, SearchEntity.class);
		if (searchEntity == null) {
			searchEntity = cacheManager.getObject(userToken.getProductCode(), code, SearchEntity.class);
			searchEntity.setCode(sessionCode);
		}
		// find direction
		Integer diff = searchEntity.getPageSize();
		if (reverse)
			diff = diff * -1;
		// calculate new pageStart
		Integer pageStart = searchEntity.getPageStart() + diff;
		searchEntity.setPageStart(pageStart);
		// send updated search
		searchUtils.searchTable(searchEntity);
	}

	/**
	 * Handle a sort click event.
	 *
	 * @param code
	 */
	public void handleSearchSort(String code) {
		// fetch from the cache
		String sessionCode = searchUtils.sessionSearchCode(code);
		SearchEntity searchEntity = cacheManager.getObject(userToken.getProductCode(), "LAST-SEARCH:" + sessionCode, SearchEntity.class);
		if (searchEntity == null) {
			searchEntity = cacheManager.getObject(userToken.getProductCode(), code, SearchEntity.class);
			searchEntity.setCode(sessionCode);
		}
		// find the sort
		Optional<Sort> sortOpt = searchEntity.getTrait(Sort.class, code);
		Sort sort;
		if(!sortOpt.isPresent()) {
			log.warn("Sort missing in SEntity: " + code + ". Adding..");
			sort = new Sort(code, Ord.ASC);
			searchEntity.add(sort);
		} else {
			sort = sortOpt.get();
			sort.flipOrd();
		}
		// send updated search
		searchUtils.searchTable(searchEntity);
	}

}
