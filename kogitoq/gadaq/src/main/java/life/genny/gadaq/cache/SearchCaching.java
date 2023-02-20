package life.genny.gadaq.cache;

import static life.genny.qwandaq.attribute.Attribute.LNK_PARENT;
import static life.genny.qwandaq.attribute.Attribute.PRI_BCC;
import static life.genny.qwandaq.attribute.Attribute.PRI_BODY;
import static life.genny.qwandaq.attribute.Attribute.PRI_CC;
import static life.genny.qwandaq.attribute.Attribute.PRI_CODE;
import static life.genny.qwandaq.attribute.Attribute.PRI_CONTEXT_ASSOCIATIONS;
import static life.genny.qwandaq.attribute.Attribute.PRI_CONTEXT_LIST;
import static life.genny.qwandaq.attribute.Attribute.PRI_DEFAULT_MSG_TYPE;
import static life.genny.qwandaq.attribute.Attribute.PRI_DESCRIPTION;
import static life.genny.qwandaq.attribute.Attribute.PRI_NAME;

import java.lang.invoke.MethodHandles;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.entity.search.trait.Action;
import life.genny.qwandaq.entity.search.trait.Column;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
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
		cacheDropdown(DEF_ADDRESS,
				new SearchEntity("SBE_SER_LNK_SELECT_COUNTRY", "Country Dropdown")
						.add(new Filter(LNK_PARENT, Operator.CONTAINS, GRP_COUNTRIES)));

		// DEF_PERSON
		cacheDropdown(DEF_PERSON,
				new SearchEntity("SBE_SER_LNK_GENDER_SELECT", "Select Gender Dropdown")
						.add(new Filter(LNK_PARENT, Operator.CONTAINS, GRP_GENDERS)));

		// DEF_COMPANY
		cacheDropdown(DEF_COMPANY,
				new SearchEntity("SBE_SER_LNK_NUMBER_STAFF", "Number Staff Dropdown")
						.add(new Filter(LNK_PARENT, Operator.CONTAINS, GRP_NUMBER_STAFF)));
		cacheDropdown(DEF_COMPANY,
				new SearchEntity("SBE_SER_LNK_COMPANY_INDUSTRY", "Company Industry Dropdown")
						.add(new Filter(LNK_PARENT, Operator.CONTAINS, GRP_COMPANY_INDUSTRY)));

		// DEF_TOOL
		cacheDropdown(DEF_TOOL,
				new SearchEntity("SBE_SER_LNK_TOOL_TYPE", "Tool Type Dropdown")
						.add(new Filter(PRI_CODE, Operator.STARTS_WITH, Prefix.TTY_)));

		// DEF_APPOINTMENT
		cacheDropdown(DEF_APPOINTMENT,
				new SearchEntity("SBE_SER_LNK_SELECT_TOOL", "Select Toole Dropdown")
						.add(new Filter("LNK_TOOL_TYPE", Operator.EQUALS, "TTY_CONFERENCING")));

		// DEF_BUILDING
		cacheDropdown(DEF_BUILDING,
				new SearchEntity("SBE_SER_LNK_AREA_UNIT", "Area Unit Dropdown")
						.add(new Filter(LNK_PARENT, Operator.CONTAINS, GRP_AREA_UNIT)));

		// DEF_MESSAGE
		cacheSearch(
				new SearchEntity(SearchEntity.SBE_MESSAGE, "Messages")
						.add(new Filter(PRI_CODE, Operator.STARTS_WITH, Prefix.MSG_))
						.add(new Column(PRI_NAME, "Code"))
						.add(new Column(PRI_DESCRIPTION, "Description"))
						.add(new Column(PRI_DEFAULT_MSG_TYPE, "Default Message Type"))
						.add(new Column(PRI_CONTEXT_LIST, "Context List"))
						.add(new Column(PRI_CONTEXT_ASSOCIATIONS, "Context Associations"))
						.add(new Column(PRI_CC, "CC"))
						.add(new Column(PRI_BCC, "BCC"))
						.add(new Column(PRI_BODY, "Body"))
						.add(new Action(EDIT, "Edit"))
						.setPageSize(4)
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
