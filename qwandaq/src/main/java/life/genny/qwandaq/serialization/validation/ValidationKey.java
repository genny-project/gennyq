package life.genny.qwandaq.serialization.validation;

import life.genny.qwandaq.serialization.common.CoreEntityKey;

public class ValidationKey implements CoreEntityKey {
	private String realm;
	private String code;

	// No-arg constructor
	public ValidationKey() { }

	public ValidationKey(String realm, String code) {
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
	public ValidationKey fromKey(String key) {
		String[] args = key.split(getDelimiter());
		return new ValidationKey(args[0], args[1]);
	}
	@Override
	public String getKeyString() {
		return realm + getDelimiter() + code;
	}

	@Override
	public String getEntityCode() {
		return getComponents()[1];
	}

	@Override
	public int hashCode() {
		return getKeyString().hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if(!(other instanceof ValidationKey)) {
			return false;
		}
		ValidationKey otherValKey = (ValidationKey) other;
		return getKeyString().equals(otherValKey.getKeyString());
	}
}
