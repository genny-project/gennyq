package life.genny.kogito.common.service;

import static life.genny.qwandaq.attribute.Attribute.PRI_NAME;
import static life.genny.qwandaq.entity.PCM.PCM_CONTENT;
import static life.genny.qwandaq.entity.PCM.PCM_PROCESS;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.PCM;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.entity.search.trait.Sort;
import life.genny.qwandaq.exception.runtime.search.trait.MissingTraitException;
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
	TaskService tasks;

	/**
	 * Perform a Bucket search.
	 */
	public void sendBuckets() {
		String userCode = userToken.getUserCode();
		tasks.dispatch(userCode, userCode, PCM_PROCESS, PCM_CONTENT, "PRI_LOC1");
	}

	/**
	 * Perform a Detail View search.
	 *
	 * @param code The code of the target to display
	 */
	public void sendTable(String code) {

		// trim TREE_ITEM_ from code if present
		code = StringUtils.replaceOnce(code, "_TREE_ITEM_", "_");
		String searchCode = StringUtils.replaceOnce(code, Prefix.QUE, Prefix.SBE);
		log.info("Sending Table :: " + searchCode);

		// send pcm with correct template code
		String userCode = userToken.getUserCode();
		PCM pcm = beUtils.getPCM(PCM.PCM_TABLE);
		pcm.setLocation(1, searchCode);
		tasks.dispatch(userCode, userCode, pcm, PCM_CONTENT, PCM.location(1));
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
		String type = StringUtils.removeStart(definition.getCode(), Prefix.DEF);

		// construct template and question codes from type
		String pcmCode = Prefix.PCM + type + "_DETAIL_VIEW";

		// send pcm with correct info
		String userCode = userToken.getUserCode();
		tasks.dispatch(userCode, targetCode, pcmCode, PCM_CONTENT, PCM.location(1));
	}

	/**
	 * Perform a named search from search bar
	 * 
	 * @param code
	 * @param nameWildcard
	 */
	public void sendNameSearch(String code, String nameWildcard) {

		log.info("Sending Name Search :: " + code);

		SearchEntity searchEntity = CacheUtils.getObject(userToken.getProductCode(),
				code, SearchEntity.class);

		// TODO: remove this from alyson
		nameWildcard = StringUtils.removeStart(nameWildcard, "!");

		searchEntity.add(new Filter(PRI_NAME, Operator.LIKE, "%"+nameWildcard+"%"));
		searchUtils.searchTable(searchEntity);
	}

	/**
	 * @param code
	 * @param code
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

	public void handleSearchSort(String entityCode, Sort sort) {
		String sessionCode = searchUtils.sessionSearchCode(entityCode);
		SearchEntity searchEntity = CacheUtils.getObject(userToken.getProductCode(), "LAST-SEARCH:" + sessionCode, SearchEntity.class);
		if (searchEntity == null) {
			searchEntity = CacheUtils.getObject(userToken.getProductCode(), entityCode, SearchEntity.class);
			searchEntity.setCode(sessionCode);
		}

		// Change the sort
		Optional<Sort> sortOpt = searchEntity.getTrait(Sort.class, sort.getCode());
		Sort searchSort;
		if(!sortOpt.isPresent()) {
			log.warn("Sort missing in SEntity: " + entityCode + ". Adding..");
			searchEntity.add(sort);
			searchSort = sort;
		} else {
			searchSort = sortOpt.get();
		}

		searchSort.flipOrd();

		// send updated search
		searchUtils.searchTable(searchEntity);
	}

}
