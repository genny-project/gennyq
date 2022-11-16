package life.genny.qwandaq.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GennyItem {
	List<AttributeCodeValueString> b2bdata = new ArrayList<>();
	
	public GennyItem() { }
	
	public GennyItem(List<AttributeCodeValueString> b2bdata) {
		this.b2bdata = b2bdata;
	}

	public void addB2B(AttributeCodeValueString item)
	{
		Optional<AttributeCodeValueString> existing = get(item.attributeCode);
		if (existing.isPresent()) {
			existing.get().value = item.value;
		} else {
			b2bdata.add(item);
		}
	}
	
	public void removeB2B(final String attributeCode) {

		if ((attributeCode == null)||(attributeCode.isBlank())) {
			return;
		}
		final String cleanAttributeCode = attributeCode.trim().toUpperCase();
		Optional<AttributeCodeValueString> existing = get(cleanAttributeCode);
		existing.ifPresent(attributeCodeValueString -> b2bdata.remove(attributeCodeValueString));
	}
	
	/**
	 * @return the b2bdata
	 */
	public List<AttributeCodeValueString> getB2bdata() {
		return b2bdata;
	}

	/**
	 * @param b2bdata the b2bdata to set
	 */
	public void setB2bdata(List<AttributeCodeValueString> b2bdata) {
		this.b2bdata = b2bdata;
	}

	public Optional<AttributeCodeValueString> get(final String attributeCode)
	{
		if ((attributeCode == null)||(attributeCode.isBlank())) {
			return null;
		}
		final String cleanAttributeCode = attributeCode.trim().toUpperCase();
		// TODo optimise
		Optional<AttributeCodeValueString> matchingObject = b2bdata.stream().
                filter(p -> p.getAttributeCode().equals(cleanAttributeCode)).
                findFirst();
		return matchingObject;
	}

}
