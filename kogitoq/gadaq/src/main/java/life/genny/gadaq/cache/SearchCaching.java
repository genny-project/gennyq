package life.genny.gadaq.cache;

import static life.genny.qwandaq.attribute.Attribute.LNK_MESSAGE_TYPE;
import static life.genny.qwandaq.attribute.Attribute.PRI_BODY;
import static life.genny.qwandaq.attribute.Attribute.PRI_CODE;
import static life.genny.qwandaq.attribute.Attribute.PRI_NAME;

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

	public static final String DEF_ADDRESS = "DEF_ADDRESS";
	public static final String DEF_PERSON = "DEF_PERSON";
	public static final String DEF_COMPANY = "DEF_COMPANY";
	public static final String DEF_APPOINTMENT = "DEF_APPOINTMENT";
	public static final String DEF_TOOL = "DEF_TOOL";
	public static final String DEF_BUILDING = "DEF_BUILDING";

	public static final String EDIT = "EDIT";

	public static final String GRP_GENDERS = "GRP_GENDERS";
	public static final String GRP_COUNTRIES = "GRP_COUNTRIES";
	public static final String GRP_NUMBER_STAFF = "GRP_NUMBER_STAFF";
	public static final String GRP_COMPANY_INDUSTRY = "GRP_COMPANY_INDUSTRY";
	public static final String GRP_AREA_UNIT = "GRP_AREA_UNIT";

	@Inject
	Service service;

	@Inject
	CacheManager cm;

	public void saveToCache() {

		// DEF_ADDRESS
		cacheDropdown("DEF_ADDRESS",
				new SearchEntity("SBE_SER_PRI_TIMEZONE_ID", "Timezone ID Dropdown")
						.add(new Filter("PRI_TIMEZONE_ID", Operator.LIKE, "%")));
		cacheDropdown("DEF_ADDRESS",
				new SearchEntity("SBE_SER_LNK_SELECT_COUNTRY", "Country Dropdown")
						.setLinkValue("COUNTRY"));

		// DEF_PERSON
		cacheDropdown("DEF_PERSON",
				new SearchEntity("SBE_SER_LNK_ALL_EMAILS", "All Emails Dropdown")
						.setLinkValue("YES_NO"));
		cacheDropdown("DEF_PERSON",
				new SearchEntity("SBE_SER_LNK_SEND_EMAIL", "Send Emails Dropdown")
						.setLinkValue("YES_NO"));
		cacheDropdown("DEF_PERSON",
				new SearchEntity("SBE_SER_LNK_GENDER_SELECT", "Select Gender Dropdown")
						.setLinkValue("GENDER"));

		// DEF_COMPANY
		cacheDropdown("DEF_COMPANY",
				new SearchEntity("SBE_SER_LNK_SPECIFY_ABN", "Specify ABN Dropdown")
						.setLinkValue("YES_NO"));
		cacheDropdown("DEF_COMPANY",
				new SearchEntity("SBE_SER_LNK_NUMBER_STAFF", "Number Staff Dropdown")
						.setLinkValue("NO_OF_STAFF"));
		cacheDropdown("DEF_COMPANY",
				new SearchEntity("SBE_SER_LNK_COMPANY_INDUSTRY", "Company Industry Dropdown")
						.setLinkValue("COMPANY_INDUSTRY"));

		// DEF_TOOL
		cacheDropdown("DEF_TOOL",
				new SearchEntity("SBE_SER_LNK_TOOL_TYPE", "Tool Type Dropdown")
						.add(new Filter("PRI_CODE", Operator.LIKE, "TTY_%")));

		// DEF_APPOINTMENT
		cacheDropdown("DEF_APPOINTMENT",
				new SearchEntity("SBE_SER_LNK_SELECT_TOOL", "Select Toole Dropdown")
						.add(new Filter("LNK_TOOL_TYPE", Operator.EQUALS, "TTY_CONFERENCING")));

		// DEF_BUCKET_PAGE
		cacheDropdown("DEF_BUCKET_PAGE",
				new SearchEntity("SBE_SER_LNK_INTERN", "Intern Dropdown")
						.add(new Filter("PRI_IS_INTERN", true)));
		cacheDropdown("DEF_BUCKET_PAGE",
				new SearchEntity("SBE_SER_LNK_AUTHOR", "Author Dropdown")
						.add(new Filter("PRI_IS_DEV", true)));

		// DEF_REMOTE_SERVICE
		cacheDropdown("DEF_REMOTE_SERVICE",
				new SearchEntity("SBE_SER_LNK_PREFERRED_CONTACT", "Preferred Contact Dropdown")
						.setLinkValue("PREF_CONTACT"));

		// DEF_SEARCH_ENTITY
		cacheDropdown("DEF_SEARCH_ENTITY",
				new SearchEntity("SBE_SER_LNK_SELECT_COUNTRY", "Select Country Dropdown")
						.setLinkValue("COUNTRY"));
		cacheDropdown("DEF_SEARCH_ENTITY",
				new SearchEntity("SBE_SER_LNK_STATE", "Select State Dropdown")
						.setLinkValue("AUS_STATE"));

		// DEF_BUILDING
		cacheDropdown("DEF_BUILDING",
				new SearchEntity("SBE_SER_LNK_AREA_UNIT", "Area Unit Dropdown")
						.setLinkValue("AREA_UNIT"));

		// DEF_MESSAGE
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
