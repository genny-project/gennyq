package life.genny.qwandaq.constants;

public final class GennyConstants {

	private GennyConstants() {}

	public static final String PATH_TO_PROTOS = "/life/genny/qwandaq/serialization/protos/";
    public static final String PACKAGE_PREFIX = "life.genny";

    public static final String PAGINATION_NEXT="QUE_TABLE_NEXT_BTN";
    public static final String PAGINATION_PREV="QUE_TABLE_PREVIOUS_BTN";

	public static final String PRI_IS_PREFIX = "PRI_IS_";

    // ================================== CAPABILITY CONSTANTS =============================================
	// Capability Attribute Prefix
	public static final String[] ACCEPTED_CAP_PREFIXES = { Prefix.ROL_, Prefix.PER_ };
	public static final String DEF_ROLE_CODE = "DEF_ROLE";
	
	// =====================================================================================================

	// service user
	public static final String PER_SERVICE = "PER_SERVICE";

	public static final String QUE_TABLE_LAZY_LOAD = "QUE_TABLE_LAZY_LOAD";

	// event
	public static final String CODE = "code";
	public static final String TARGETCODE = "targetCode";
	public static final String TARGETCODES = "targetCodes";
	public static final String TOKEN = "token";
	public static final String ATTRIBUTECODE = "attributeCode";
	public static final String VALUE = "value";

	//Message
	public static final String SBE_TABLE_MESSAGE = "SBE_TABLE_MESSAGE";

    public static final String ERROR_FALLBACK_MSG = "Error Occurred!";
	public static final char COMMA = ',';

	// ================================== DATA TYPES =============================================
	public static final String JAVA_LANG_INTEGER = "java.lang.Integer";
	public static final String INTEGER = "Integer";
	public static final String JAVA_TIME_LOCAL_DATE_TIME = "java.time.LocalDateTime";
	public static final String LOCAL_DATE_TIME = "LocalDateTime";
	public static final String JAVA_TIME_LOCAL_TIME = "java.time.LocalTime";
	public static final String LOCAL_TIME = "LocalTime";
	public static final String JAVA_LANG_LONG = "java.lang.Long";
	public static final String LONG = "Long";
	public static final String JAVA_LANG_DOUBLE = "java.lang.Double";
	public static final String DOUBLE = "Double";
	public static final String JAVA_LANG_BOOLEAN = "java.lang.Boolean";
	public static final String BOOLEAN = "Boolean";
	public static final String JAVA_TIME_LOCAL_DATE = "java.time.LocalDate";
	public static final String LOCAL_DATE = "LocalDate";
	public static final String ORG_JAVAMONEY_MONETA_MONEY = "org.javamoney.moneta.Money";
	public static final String MONEY = "Money";
	public static final String JAVA_LANG_STRING = "java.lang.String";
	// =====================================================================================================
}

