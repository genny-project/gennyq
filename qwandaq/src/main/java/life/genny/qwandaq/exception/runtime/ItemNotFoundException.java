package life.genny.qwandaq.exception.runtime;

import life.genny.qwandaq.exception.GennyRuntimeException;

/**
 * This exception is used to notify that an item could not be found
 *
 * @author Jasper Robison
 */
public class ItemNotFoundException extends GennyRuntimeException {

	public static final String ERR_TEXT = "%s could not be found";
	public static final String PRD_TXT = ERR_TEXT + " in product %s";

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
		this(code, err, true);
	}

	public ItemNotFoundException(String code, Throwable err, boolean useFormat) {
		super(useFormat ? String.format(ERR_TEXT, code) : code, err);
	}

	public ItemNotFoundException(String productCode, String code, Throwable err) {
		super(String.format(PRD_TXT, code, productCode), err);
	}

	public ItemNotFoundException(String productCode, String baseEntityCode, String attributeCode) {
		super(String.format(PRD_TXT, "[" + baseEntityCode + ":" + attributeCode + "]", productCode));
	}

	/**
	 * Generate a plain ItemNotFoundException without using the predefined format.
	 * @param errorMsg - the details of this exception
	 * @param cause - the cause of this Exception
	 * @return a new unformatted ItemNotFoundException
	 * 
	 * @see {@link ItemNotFoundException#ERR_TEXT}
	 * @see {@link ItemNotFoundException#PRD_TXT}
	 */
	public static ItemNotFoundException general(String errorMsg, Throwable cause) {
		return new ItemNotFoundException(errorMsg, cause, false);
	}

	/**
	 * Generate a plain ItemNotFoundException without using the predefined format.
	 * @param errorMsg - the details of this exception
	 * @return a new unformatted ItemNotFoundException
	 * 
	 * @see {@link ItemNotFoundException#ERR_TEXT}
	 * @see {@link ItemNotFoundException#PRD_TXT}
	 */
	public static ItemNotFoundException general(String errorMsg) {
		return new ItemNotFoundException(errorMsg, false);
	}
}
