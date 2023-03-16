package life.genny.qwandaq.serialization.attribute;

import life.genny.qwandaq.serialization.baseentity.BaseEntityKey;
import life.genny.qwandaq.serialization.common.CoreEntityKey;

public class AttributeKey implements CoreEntityKey {

	private String realm;
	private String attributeCode;

	public AttributeKey() {}

	public AttributeKey(String realm, String attributeCode) {
		this.realm = realm;
		this.attributeCode = attributeCode;
	}

	public String getRealm() {
		return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
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
	public String getKeyString() {
		return realm + getDelimiter() + attributeCode;
	}

	@Override
	public CoreEntityKey fromKey(String key) {
		String[] args = key.split(getDelimiter());
		return new AttributeKey(args[0], args[1]);
	}

	@Override
	public String getEntityCode() {
		return getComponents()[1];
	}

	@Override
	public boolean equals(Object otherAttributeKey) {
		if (otherAttributeKey == null) {
			return false;
		}
		return this.getKeyString().equals(((AttributeKey) otherAttributeKey).getKeyString());
	}

	@Override
	public int hashCode() {
		return this.getKeyString().hashCode();
	}
}
