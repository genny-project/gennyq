package life.genny.qwandaq.exception.runtime;

import life.genny.qwandaq.exception.GennyRuntimeException;

/**
 * This exception is used to notify that an item could not be found
 *
 * @author Jasper Robison
 */
public class ItemNotFoundException extends GennyRuntimeException {

	static String ERR_TEXT = " could not be found";

	public ItemNotFoundException() {
		super();
	}

	public ItemNotFoundException(String code) {
		super(code + ERR_TEXT);
	}

	public ItemNotFoundException(String productCode, String code) {
		super(code + ERR_TEXT + " in product " + productCode);
	}
	
	public ItemNotFoundException(String code, Throwable err) {
		super(code + ERR_TEXT, err);
	}

	public ItemNotFoundException(String productCode, String code, Throwable err) {
		super(code + ERR_TEXT + " in product " + productCode, err);
	}
}
