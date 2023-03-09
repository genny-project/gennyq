package life.genny.qwandaq.exception.runtime;

import life.genny.qwandaq.exception.GennyRuntimeException;

/**
 * This exception is used to notify that an item could not be found
 *
 * @author Jasper Robison
 */
public class ItemNotFoundException extends GennyRuntimeException {

	private static final String ERR_TEXT = "%s could not be found";
	private static final String PRD_TXT = ERR_TEXT + " in product %s";

	public ItemNotFoundException() {
		super();
	}

	public ItemNotFoundException(String code) {
		this(code, true);
	}

	public ItemNotFoundException(String code, boolean useFormat) {
		super(useFormat ? String.format(ERR_TEXT, code) : code);
	}

	public ItemNotFoundException(String productCode, String code) {
		super(String.format(PRD_TXT, code, productCode));
	}
	
	public ItemNotFoundException(String code, Throwable err) {
		super(String.format(ERR_TEXT, code), err);
	}

	public ItemNotFoundException(String productCode, String code, Throwable err) {
		super(String.format(PRD_TXT, code, productCode), err);
	}

	public ItemNotFoundException(String productCode, String baseEntityCode, String attributeCode) {
		super(String.format(PRD_TXT, "[" + baseEntityCode + ":" + attributeCode + "]", productCode));
	}
	
	/**
	 * Create a general ItemNotFoundException that does not refer to a product
	 * @param missingItem - reference to item that is missing
	 * @param expectedLocation - reference to location it was expected to be in
	 * @return a newly formatted ItemNotFoundException
	 */
	public static ItemNotFoundException general(String missingItem, String expectedLocation) {
		return new ItemNotFoundException(String.format(ERR_TEXT, missingItem) + " in " + expectedLocation, false);
	}
}
