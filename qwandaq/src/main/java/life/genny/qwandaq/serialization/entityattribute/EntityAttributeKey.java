package life.genny.qwandaq.serialization.entityattribute;

import life.genny.qwandaq.serialization.common.CoreEntityKey;

public class EntityAttributeKey implements CoreEntityKey {

	private static final long serialVersionUID = -5177661824086700199L;

	public static final String BEA_KEY_DELIMITER = ":";

	private String realm;
	private String baseEntityCode;
	private String attributeCode;

	// No-arg constructor
	public EntityAttributeKey() { }

	public EntityAttributeKey(String realm, String baseEntityCode, String attributeCode) {
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
	public EntityAttributeKey fromKey(String key) {
		String[] args = key.split(getDelimiter());
		return new EntityAttributeKey(args[0], args[1], args[2]);
	}
	@Override
	public String getKeyString() {
		return realm + getDelimiter() + baseEntityCode + getDelimiter() + attributeCode;
	}

	@Override
	public String getEntityCode() {
		return baseEntityCode + getDelimiter() + attributeCode;
	}

	@Override
	public int hashCode() {
		return getKeyString().hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if(!(other instanceof EntityAttributeKey)) {
			return false;
		}
		EntityAttributeKey otherEA = (EntityAttributeKey) other;
		return getKeyString().equals(otherEA.getKeyString());
	}
}
