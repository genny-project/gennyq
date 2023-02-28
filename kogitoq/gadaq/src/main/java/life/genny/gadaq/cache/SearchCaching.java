package life.genny.gadaq.cache;

import static life.genny.qwandaq.attribute.Attribute.LNK_MESSAGE_TYPE;
import static life.genny.qwandaq.attribute.Attribute.PRI_BODY;
import static life.genny.qwandaq.attribute.Attribute.PRI_CODE;
import static life.genny.qwandaq.attribute.Attribute.PRI_NAME;
import static life.genny.qwandaq.attribute.Attribute.LNK_PARENT;
import java.lang.invoke.MethodHandles;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.entity.search.trait.*;
import org.jboss.logging.Logger;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.serviceq.Service;


/**
 * SearchCaching
 */
@ApplicationScoped
public class SearchCaching {

    static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	public static final String DEF_MESSAGE= "DEF_MESSAGE";

	public static final String EDIT = "EDIT";

	@Inject
	Service service;

	@Inject
	CacheManager cm;

	public void saveToCache() {
		
		// DEF_MESSAGE
		SearchEntity messageTypeSBE = new SearchEntity("SBE_SER_LNK_MESSAGE_TYPE", "Message Types Dropdown")
				.add(new Filter(LNK_PARENT, Operator.CONTAINS, "GRP_MESSAGE_TYPES"));

		cacheDropdown(DEF_MESSAGE, messageTypeSBE);			

		cacheSearch(
				new SearchEntity(GennyConstants.SBE_TABLE_MESSAGE, "Messages")
						.add(new Filter(PRI_CODE, Operator.STARTS_WITH, Prefix.MSG_))
						.add(new Column(PRI_CODE, "Message Id"))
						.add(new Column(PRI_NAME, "Name"))
						.add(new Column(PRI_BODY, "Body"))
						.add(new AssociatedColumn(LNK_MESSAGE_TYPE, PRI_NAME, "Message Type"))
						.add(new Action(EDIT, "Edit"))
						.setPageSize(8)
						.setPageStart(0));
	}

	private void cacheSearch(SearchEntity entity) {
		for (String productCode : service.getProductCodes()) {
			entity.setRealm(productCode);
			cm.putObject(productCode, entity.getCode(), entity);
		}
	}

	private void cacheDropdown(String definitionCode, SearchEntity entity) {
		for (String productCode : service.getProductCodes()) {
			entity.setRealm(productCode);
			String key = new StringBuilder(definitionCode).append(":").append(entity.getCode()).toString();
			cm.putObject(productCode, key, entity);
		}
	}
}
