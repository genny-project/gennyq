package life.genny.qwandaq.constants;

public final class GennyConstants {
    public static final String CACHE_NAME_BASEENTITY = "baseentity";
    public static final String CACHE_NAME_BASEENTITY_ATTRIBUTE = "baseentity_attribute";

    public static final String CACHE_NAME_ATTRIBUTE = "attribute";
    public static final String PACKAGE_PREFIX = "life.genny";

    public static final String PAGINATION_NEXT="QUE_TABLE_NEXT_BTN";
    public static final String PAGINATION_PREV="QUE_TABLE_PREVIOUS_BTN";
    public static final String PAGINATION_INDEX="PRI_INDEX";
    public static final String SEARCH_TEXT="PRI_SEARCH_TEXT";
    public static final String PRI_NAME="PRI_NAME";
    public static final String PRI_CODE="PRI_CODE";
    public static final String  PRI_PREFIX = "PRI_";
    public static final String PCM_TABLE = "PCM_TABLE";
    public static final String PCM_PROCESS = "PCM_PROCESS";
    public static final String BUCKET_CODES = "BUCKET_CODES";
    public static final String BUCKET_DISPLAY = "DISPLAY";
    public static final String NONE = "NONE";
    public static final String BUCKET_PROCESS = "PROCESS";


    // ================================== CAPABILITY CONSTANTS =============================================
	// Capability Attribute Prefix
	public static final String CAP_CODE_PREFIX = "CAP_";
	public static final String ROLE_BE_PREFIX = "ROL_";
    public static final String PER_BE_PREFIX = "PER_";

	public static final String PRI_IS_PREFIX = "PRI_IS_";

	// TODO: Confirm we want DEFs to have capabilities as well
	public static final String[] ACCEPTED_CAP_PREFIXES = { ROLE_BE_PREFIX, PER_BE_PREFIX, "DEF_" };

	public static final String ROLE_LINK_CODE = "LNK_ROLE";
    public static final String CHILDREN_LINK_CODE = "LNK_CHILDREN";
    public static final String DEF_ROLE_CODE = "DEF_ROLE";

    // =====================================================================================================

    public static final String LINK_CODE = "LNK_";
	public static final String LNK_DEF_CODE = "LNK_DEF";
	public static final String LNK_INCLUDE_CODE = "LNK_INCLUDE";

    public static final String PERSON_DEF_CODE = "DEF_PERSON";
    //Question
    public static final String QUE_QQQ_GROUP = "QQQ_QUESTION_GROUP";
    public static final String QUE_FILTER_GRP = "QUE_FILTER_GRP";
    public static final String QUE_ADD_FILTER_GRP = "QUE_ADD_FILTER_GRP";
    public static final String QUE_FILTER_COLUMN = "QUE_FILTER_COLUMN";
    public static final String QUE_FILTER_VALUE = "QUE_FILTER_VALUE";
    public static final String QUE_FILTER_OPTION = "QUE_FILTER_OPTION";
    public static final String QUE_FILTER_VALUE_TEXT = "QUE_FILTER_VALUE_TEXT";
    public static final String QUE_SUBMIT = "QUE_SUBMIT";
    public static final String QUE_FILTER_VALUE_PREF = "QUE_FILTER_VALUE_";
    public static final String QUE_TAG_PREF = "QUE_TAG_";
    public static final String QUE_FILTER_VALUE_COUNTRY = "QUE_FILTER_VALUE_COUNTRY";
    public static final String QUE_FILTER_VALUE_STATE = "QUE_FILTER_VALUE_STATE";
    public static final String QUE_FILTER_VALUE_INTERNSHIP_TYPE = "QUE_FILTER_VALUE_INTERNSHIP_TYPE";
    public static final String QUE_FILTER_VALUE_ACADEMY = "QUE_FILTER_VALUE_ACADEMY";
    public static final String QUE_FILTER_VALUE_DJP_HC = "QUE_FILTER_VALUE_DJP_HC";
    public static final String QUE_FILTER_VALUE_DATE = "QUE_FILTER_VALUE_DATE";
    public static final String QUE_FILTER_VALUE_DATETIME = "QUE_FILTER_VALUE_DATETIME";
    public static final String QUE_FILTER_VALUE_TIME = "QUE_FILTER_VALUE_TIME";

    //Link
    public static final String LNK_FILTER_COLUMN= "LNK_FILTER_COLUMN";
    public static final String LNK_FILTER_OPTION = "LNK_FILTER_OPTION";

    public static final String LNK_CORE= "LNK_CORE";
    public static final String LNK_PERSON= "LNK_PERSON";
    public static final String LNK_ITEMS= "LNK_ITEMS";
    public static final String ITEMS= "ITEMS";


    //Filter
    public static final String  FILTERS = "Filters";
    public static final String  FILTER_QUE_EXIST = "QUE_EXISTING_FILTERS_GRP";
    public static final String  FILTER_QUE_EXIST_NAME = "Existing Filters";
    public static final String  FILTER_SEL = "SEL_FILTER_COLUMN_";
    public static final String  FILTER_COL = "FLC_";
    public static final String  SEL_PREF = "SEL_";
    public static final String  SEL_EQUAL_TO = "SEL_EQUAL_TO";
    public static final String  SEL_NOT_EQUAL_TO = "SEL_NOT_EQUAL_TO";
    public static final String  SEL_LIKE = "SEL_LIKE";
    public static final String  SEL_NOT_LIKE = "SEL_NOT_LIKE";
    public static final String SEL_FILTER_COLUMN_FLC = "SEL_FILTER_COLUMN_FLC_";

    //Filter columns
    public static final String FILTER_DATE = "DATE";

    //Filter academy
    public static final String SEL_OA_WRP = "SEL_OA_WRP";
    public static final String SEL_OA_WIL = "SEL_OA_WIL";
    public static final String SEL_OA_CARRERBOX = "SEL_OA_CARRERBOX";
    public static final String SEL_PROFESSIONAL_YEAR = "SEL_PROFESSIONAL_YEAR";
    public static final String SEL_COURSE_CREDIT = "SEL_COURSE_CREDIT";
    public static final String SEL_DIGITAL_JOBS = "SEL_DIGITAL_JOBS";

    //Filter date
    public static final String SEL_GREATER_THAN = "SEL_GREATER_THAN";
    public static final String SEL_GREATER_THAN_OR_EQUAL_TO = "SEL_GREATER_THAN_OR_EQUAL_TO";
    public static final String SEL_LESS_THAN = "SEL_LESS_THAN";
    public static final String SEL_LESS_THAN_OR_EQUAL_TO = "SEL_LESS_THAN_OR_EQUAL_TO";

    //Dropdown
    public static final String SBE_DROPDOWN = "SBE_DROPDOWN";
    public static final String PRI_CODE_LABEL = "CODE";
    public static final String PRI_NAME_LABEL = "NAME";

    //event
    public static final String EVENT_WEBCMDS = "webcmds";

    //caching
    public static final String CACHING_SBE = "SBE_";
    public static final String SBE_HOST_COMPANIES_VIEW = "SBE_HOST_COMPANIES_VIEW";

    //bucket filter
    public static final String QUE_BUCKET_INTERNS_GRP = "QUE_BUCKET_INTERNS_GRP";
    public static final String QUE_SELECT_INTERN = "QUE_SELECT_INTERN";
    public static final String BKT_APPLICATIONS = "BKT_APPLICATIONS";

    public static final String QUE_TABLE_LAZY_LOAD = "QUE_TABLE_LAZY_LOAD";
    //Event
    public static final String PRI_EVENT = "PRI_EVENT";
    public static final String CODE = "code";
    public static final String TARGETCODE = "targetCode";
    public static final String TARGETCODES = "targetCodes";
    public static final String TOKEN = "token";
    public static final String ATTRIBUTECODE = "attributeCode";
    public static final String VALUE = "value";

}
