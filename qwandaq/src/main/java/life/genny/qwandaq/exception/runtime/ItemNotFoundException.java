package life.genny.qwandaq.exception.runtime;

import life.genny.qwandaq.exception.GennyRuntimeException;

/**
 * This exception is used to notify that an item could not be found
 *
 * @author Jasper Robison
 */
public class ItemNotFoundException extends GennyRuntimeException {

	static String ERR_TEXT = "%s could not be found";
	static String PRD_TXT = ERR_TEXT + " in product %s";

	public ItemNotFoundException() {
		super();
	}

	public ItemNotFoundException(String code) {
		super(String.format(ERR_TEXT, code));
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
}
