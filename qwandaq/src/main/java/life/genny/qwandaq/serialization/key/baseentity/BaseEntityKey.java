package life.genny.qwandaq.serialization.key.baseentity;

import life.genny.qwandaq.serialization.common.CoreEntityKey;

public class BaseEntityKey extends CoreEntityKey {

	private String baseEntityCode;

	// No-arg constructor
	@Deprecated
	public BaseEntityKey() { }

	public BaseEntityKey(String productCode, String baseEntityCode) {
		super(productCode);
		this.baseEntityCode = baseEntityCode;
	}

	// Core Entity Key Overrides
	@Override
	public BaseEntityKey fromKey(String key) {
		String[] args = key.split(getDelimiter());
		return new BaseEntityKey(args[0], args[1]);
	}
	@Override
	public String getKeyString() {
		return productCode + getDelimiter() + getEntityCode();
	}

	public String getEntityCode() {
		return baseEntityCode;
	}
}
