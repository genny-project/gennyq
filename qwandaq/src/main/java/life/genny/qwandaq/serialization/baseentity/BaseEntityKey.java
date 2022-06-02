package life.genny.qwandaq.serialization.baseentity;

import life.genny.qwandaq.serialization.common.CoreEntityKey;

public class BaseEntityKey implements CoreEntityKey {
	public static final String BE_KEY_DELIMITER = ":";

	private String realm;
	private String code;

	// No-arg constructor
	public BaseEntityKey() { }

	public BaseEntityKey(String realm, String code) {
		super(); // TODO: Why call java.lang.Object constructor?
		this.realm = realm;
		this.code = code;
	}

	public String getRealm() {
		return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}


	@Override
	public String toString() {
		return getKeyString();
	}

	// Core Entity Key Overrides

	@Override
	public String getDelimiter() {
		return BE_KEY_DELIMITER;
	}

	@Override
	public BaseEntityKey fromKey(String key) {
		String[] args = key.split(getDelimiter());
		return new BaseEntityKey(args[0], args[1]);
	}
	@Override
	public String getKeyString() {
		return realm + getDelimiter() + code;
	}

	@Override
	public String getEntityCode() {
		return getComponents()[1];
	}
}
