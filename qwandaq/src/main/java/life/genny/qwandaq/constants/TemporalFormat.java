package life.genny.qwandaq.constants;

/**
 * TemporalFormat
 */
public class TemporalFormat {

	public static final String[] DATETIME = { 
		"yyyy-MM-dd'T'HH:mm:ss",
		"yyyy-MM-dd HH:mm",
		"yyyy-MM-dd HH:mm:ss",
		"yyyy-MM-dd",
		"yyyy-MM-dd'T'HH:mm:ss.SSSZ",
		"yyyy-MM-dd HH:mm:ss.SSSZ"
	};

	public static final String[] DATE = { 
		"yyyy-MM-dd",
		"M/y",
		"yyyy/MM/dd",
		"yyyy-MM-dd'T'HH:mm:ss",
		"yyyy-MM-dd HH:mm:ss",
		"yyyy-MM-dd'T'HH:mm:ss.SSSZ",
		"yyyy-MM-dd HH:mm:ss.SSSZ"
	};

	public static final String[] TIME = { 
		"HH:mm",
		"HH:mm:ss",
		"HH:mm:ss.SSSZ"
	};
}
