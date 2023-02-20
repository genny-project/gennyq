package life.genny.qwandaq.serialization.datatype;

import life.genny.qwandaq.serialization.common.CoreEntityKey;

public class DataTypeKey implements CoreEntityKey {
	public static final String BE_KEY_DELIMITER = "|";

	private String realm;
	private String dttCode;

	// No-arg constructor
	public DataTypeKey() { }

	public DataTypeKey(String realm, String dttCode) {
		super(); // TODO: Why call java.lang.Object constructor?
		this.realm = realm;
		this.dttCode = dttCode;
	}

	public String getRealm() {
		return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public String getDttCode() {
		return dttCode;
	}

	public void setDttCode(String dttCode) {
		this.dttCode = dttCode;
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
	public DataTypeKey fromKey(String key) {
		String[] args = key.split(getDelimiter());
		return new DataTypeKey(args[0], args[1]);
	}
	@Override
	public String getKeyString() {
		return realm + getDelimiter() + dttCode;
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
		if(!(other instanceof DataTypeKey)) {
			return false;
		}
		DataTypeKey otherBE = (DataTypeKey) other;
		return getKeyString().equals(otherBE.getKeyString());
	}
}
