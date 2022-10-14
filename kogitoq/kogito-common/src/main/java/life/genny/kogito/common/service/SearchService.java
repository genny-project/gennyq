package life.genny.kogito.common.service;

import static life.genny.qwandaq.attribute.Attribute.PRI_NAME;
import static life.genny.qwandaq.entity.PCM.PCM_CONTENT;
import static life.genny.qwandaq.entity.PCM.PCM_PROCESS;

import java.lang.invoke.MethodHandles;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.PCM;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.exception.runtime.DebugException;
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
		tasks.dispatch(userCode, userCode, pcm, PCM_CONTENT, "PRI_LOC1");
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
		String template = Prefix.TPL + type + "_DETAIL_VIEW";
		String questionCode = Prefix.QUE + type;

		// send pcm with correct info
		String userCode = userToken.getUserCode();
		PCM pcm = beUtils.getPCM(PCM.PCM_DETAIL_VIEW);
		pcm.setTemplateCode(template);
		pcm.setQuestionCode(questionCode);

		tasks.dispatch(userCode, userCode, pcm, PCM_CONTENT, "PRI_LOC1");
	}

	/**
	 * Perform a named search from search bar
	 * 
	 * @param searchCode
	 * @param nameWildcard
	 */
	public void sendNameSearch(String searchCode, String nameWildcard) {

		log.info("Sending Name Search :: " + searchCode);

		SearchEntity searchEntity = CacheUtils.getObject(userToken.getProductCode(), 
				searchCode, SearchEntity.class);

		// TODO: remove this from alyson
		nameWildcard = StringUtils.removeStart(nameWildcard, "!");

		searchEntity.add(new Filter(PRI_NAME, Operator.LIKE, "%"+nameWildcard+"%"));
		searchUtils.searchTable(searchEntity);
	}

	/**
	 * @param code
	 * @param targetCode
	 */
	public void handleSearchPagination(String code, String targetCode) {

		if (!(GennyConstants.PAGINATION_NEXT.equals(code)) && !(GennyConstants.PAGINATION_PREV.equals(code)))
			throw new DebugException("Incorrect question code for pagination: " + code);

		// fetch search from cache
		SearchEntity searchEntity = CacheUtils.getObject(userToken.getProductCode(), targetCode, SearchEntity.class);

		// find direction
		Integer diff = searchEntity.getPageSize();
		if (GennyConstants.PAGINATION_PREV.equals(code))
			diff = diff * -1;

		// calculate new pageStart
		Integer pageStart = searchEntity.getPageStart() + diff;
		searchEntity.setPageStart(pageStart);

		// send updated search
		searchUtils.searchTable(searchEntity);
	}

}
