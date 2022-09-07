package life.genny.gadaq.cache;

import java.lang.invoke.MethodHandles;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.entity.search.clause.And;
import life.genny.qwandaq.entity.search.clause.Or;
import life.genny.qwandaq.entity.search.trait.Action;
import life.genny.qwandaq.entity.search.trait.AssociatedColumn;
import life.genny.qwandaq.entity.search.trait.Column;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.serviceq.Service;

/**
 * SearchCaching
 */
@ApplicationScoped
public class SearchCaching {

	static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	@Inject Service service;

	public void saveToCache() {

		cacheSearch(
			new SearchEntity("SBE_SER_PRI_TIMEZONE_ID", "Timezone ID Dropdown")
				.add(new Filter("PRI_TIMEZONE_ID", Operator.LIKE, "%"))
		);
		cacheSearch(
			new SearchEntity("SBE_SER_LNK_SELECT_COUNTRY", "Country Dropdown")
				.add(new Filter("LNK_CORE", Operator.EQUALS, "COUNTRY"))
		);
		cacheSearch(
			new SearchEntity("SBE_SER_LNK_ALL_EMAILS", "All Emails Dropdown")
				.add(new Filter("LNK_CORE", Operator.EQUALS, "YES_NO"))
		);
		cacheSearch(
			new SearchEntity("SBE_SER_LNK_SEND_EMAIL", "Send Emails Dropdown")
				.add(new Filter("LNK_CORE", Operator.EQUALS, "YES_NO"))
		);
		cacheSearch(
			new SearchEntity("SBE_SER_LNK_GENDER_SELECT", "Select Gender Dropdown")
				.add(new Filter("LNK_CORE", Operator.EQUALS, "GENDER"))
		);
		cacheSearch(
			new SearchEntity("SBE_SER_LNK_SPECIFY_ABN", "Specify ABN Dropdown")
				.add(new Filter("LNK_CORE", Operator.EQUALS, "YES_NO"))
		);
		cacheSearch(
			new SearchEntity("SBE_SER_LNK_NUMBER_STAFF", "Number Staff Dropdown")
				.add(new Filter("LNK_CORE", Operator.EQUALS, "NO_OF_STAFF"))
		);
		cacheSearch(
			new SearchEntity("SBE_SER_LNK_COMPANY_INDUSTRY", "Company Industry Dropdown")
				.add(new Filter("LNK_CORE", Operator.EQUALS, "COMPANY_INDUSTRY"))
		);
		cacheSearch(
			new SearchEntity("SBE_SER_LNK_TOOL_TYPE", "Tool Type Dropdown")
				.add(new Filter("PRI_CODE", Operator.LIKE, "TTY_%"))
		);
		cacheSearch(
			new SearchEntity("SBE_SER_LNK_SELECT_TOOL", "Select Toole Dropdown")
				.add(new Filter("LNK_TOOL_TYPE", Operator.EQUALS, "TTY_CONFERENCING"))
		);
		cacheSearch(
			new SearchEntity("SBE_SER_LNK_INTERN", "Intern Dropdown")
				.add(new Filter("PRI_IS_INTERN", true))
		);
		cacheSearch(
			new SearchEntity("SBE_SER_LNK_AUTHOR", "Author Dropdown")
				.add(new Filter("PRI_IS_DEV", true))
		);
		cacheSearch(
			new SearchEntity("SBE_SER_LNK_PREFERRED_CONTACT", "Preferred Contact Dropdown")
				.add(new Filter("LNK_CORE", Operator.EQUALS, "PREF_CONTACT"))
		);
		cacheSearch(
			new SearchEntity("SBE_SER_LNK_SELECT_COUNTRY", "Select Country Dropdown")
				.add(new Filter("LNK_CORE", Operator.EQUALS, "COUNTRY"))
		);
		cacheSearch(
			new SearchEntity("SBE_SER_LNK_STATE", "Select State Dropdown")
				.add(new Filter("LNK_CORE", Operator.EQUALS, "AUS_STATE"))
		);
		cacheSearch(
			new SearchEntity("SBE_SER_LNK_AREA_UNIT", "Area Unit Dropdown")
				.add(new Filter("LNK_CORE", Operator.EQUALS, "AREA_UNIT"))
		);
	}

	private void cacheSearch(SearchEntity entity) {
		for (String productCode : service.getProductCodes()) {
			entity.setRealm(productCode);
			CacheUtils.putObject(productCode, entity.getCode(), entity);
		}
	}

}
