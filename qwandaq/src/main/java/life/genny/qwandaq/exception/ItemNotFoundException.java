package life.genny.qwandaq.exception;

/**
 * This exception is used to notify that an item could not be found
 *
 * @author Jasper Robison
 */
public class ItemNotFoundException extends GennyRuntimeException {

	static String ERR_TEXT = "%s could not be found";
	static String PRODUCT_TEXT = " in product %s";

	public ItemNotFoundException() {
		super();
	}

	public ItemNotFoundException(String code) {
		super(String.format(ERR_TEXT, code));
	}

	public ItemNotFoundException(String productCode, String code) {
		super(String.format(ERR_TEXT + PRODUCT_TEXT, code, productCode));
	}
	
	public ItemNotFoundException(String code, Throwable err) {
		super(String.format(ERR_TEXT, code), err);
	}

	public ItemNotFoundException(String productCode, String code, Throwable err) {
		super(String.format(ERR_TEXT + PRODUCT_TEXT, code, productCode), err);
	}
}
