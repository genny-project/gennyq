package life.genny.qwandaq.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AttributeCodeValueString {
	
	@JsonProperty
	public String attributeCode;
	
	@JsonProperty
	public String value;
	
	public AttributeCodeValueString() { }
	
	public AttributeCodeValueString(final String attributeCode, final String value)
	{
		this.attributeCode = attributeCode;
		this.value = value;
	}

	/**
	 * @return the attributeCode
	 */
	public String getAttributeCode() {
		return attributeCode;
	}

	/**
	 * @param attributeCode the attributeCode to set
	 */
	public void setAttributeCode(String attributeCode) {
		this.attributeCode = attributeCode;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}
	
}
