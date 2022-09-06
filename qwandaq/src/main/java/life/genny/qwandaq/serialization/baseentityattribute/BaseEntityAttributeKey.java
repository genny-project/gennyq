package life.genny.qwandaq.serialization.baseentityattribute;

import life.genny.qwandaq.serialization.common.CoreEntityKey;

public class BaseEntityAttributeKey implements CoreEntityKey {

	private static final long serialVersionUID = -5177661824086700199L;

	public static final String BEA_KEY_DELIMITER = ":";

	private String realm;
	private String baseEntityCode;
	private String attributeCode;

	// No-arg constructor
	public BaseEntityAttributeKey() { }

	public BaseEntityAttributeKey(String realm, String baseEntityCode, String attributeCode) {
		this.realm = realm;
		this.baseEntityCode = baseEntityCode;
		this.attributeCode = attributeCode;
	}

	public String getRealm() {
		return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public String getBaseEntityCode() {
		return baseEntityCode;
	}

	public void setBaseEntityCode(String baseEntityCode) {
		this.baseEntityCode = baseEntityCode;
	}
	
	public String getAttributeCode() {
		return attributeCode;
	}

	public void setAttributeCode(String attributeCode) {
		this.attributeCode = attributeCode;
	}


	@Override
	public String toString() {
		return getKeyString();
	}

	// Core Entity Key Overrides

	@Override
	public String getDelimiter() {
		return BEA_KEY_DELIMITER;
	}

	@Override
	public BaseEntityAttributeKey fromKey(String key) {
		String[] args = key.split(getDelimiter());
		return new BaseEntityAttributeKey(args[0], args[1], args[2]);
	}
	@Override
	public String getKeyString() {
		return realm + getDelimiter() + baseEntityCode + getDelimiter() + attributeCode;
	}

	@Override
	public String getEntityCode() {
		return baseEntityCode + getDelimiter() + attributeCode;
	}
}
