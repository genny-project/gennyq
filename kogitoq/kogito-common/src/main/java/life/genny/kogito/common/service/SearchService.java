package life.genny.kogito.common.service;

import static life.genny.kogito.common.utils.KogitoUtils.UseService.GADAQ;
import static life.genny.qwandaq.attribute.Attribute.PRI_NAME;
import static life.genny.qwandaq.entity.PCM.PCM_CONTENT;
import static life.genny.qwandaq.entity.PCM.PCM_PROCESS;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.kogito.common.utils.KogitoUtils;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.PCM;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.entity.search.trait.Ord;
import life.genny.qwandaq.entity.search.trait.Sort;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.DefUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.utils.SearchUtils;

@ApplicationScoped
public class SearchService {

	private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	SearchUtils searchUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	DefUtils defUtils;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	KogitoUtils kogitoUtils;

	@Inject
	TaskService tasks;

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
		BaseEntity target = beUtils.getBaseEntity(targetCode);
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
		// find in cache
		SearchEntity searchEntity = CacheUtils.getObject(userToken.getProductCode(),
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
		SearchEntity searchEntity = CacheUtils.getObject(userToken.getProductCode(), "LAST-SEARCH:" + sessionCode, SearchEntity.class);
		if (searchEntity == null) {
			searchEntity = CacheUtils.getObject(userToken.getProductCode(), code, SearchEntity.class);
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
		SearchEntity searchEntity = CacheUtils.getObject(userToken.getProductCode(), "LAST-SEARCH:" + sessionCode, SearchEntity.class);
		if (searchEntity == null) {
			searchEntity = CacheUtils.getObject(userToken.getProductCode(), code, SearchEntity.class);
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
