package life.genny.qwandaq.exception;

/**
 * This exception is used to notify that an item could not be found
 */
public class ItemNotFoundException extends GennyException {

	static String ERR_SUFFIX = " could not be found";

	public ItemNotFoundException() {
		super();
	}

	public ItemNotFoundException(String code) {
		super(code + ERR_SUFFIX);
	}
	
	public ItemNotFoundException(String code, Throwable err) {
		super(code + ERR_SUFFIX, err);
	}
}
