package life.genny.qwandaq.serialization.userstore;

import life.genny.qwandaq.serialization.common.CoreEntityKey;

public class UserStoreKey implements CoreEntityKey {
	private String realm;
	private String usercode;

	// No-arg constructor
	public UserStoreKey() { }

	public UserStoreKey(String realm, String usercode) {
		super(); // TODO: Why call java.lang.Object constructor?
		this.realm = realm;
		this.usercode = usercode;
	}

	public String getRealm() {
		return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public String getUsercode() {
		return usercode;
	}

	public void setUsercode(String usercode) {
		this.usercode = usercode;
	}


	@Override
	public String toString() {
		return getKeyString();
	}

	// Core Entity Key Overrides

	@Override
	public UserStoreKey fromKey(String key) {
		String[] args = key.split(getDelimiter());
		return new UserStoreKey(args[0], args[1]);
	}
	@Override
	public String getKeyString() {
		return realm + getDelimiter() + usercode;
	}

	@Override
	public String getEntityCode() {
		return getComponents()[1];
	}
}
