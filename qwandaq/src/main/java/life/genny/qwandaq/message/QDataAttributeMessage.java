package life.genny.qwandaq.message;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.constants.Prefix;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class QDataAttributeMessage extends QDataMessage{

	private static final long serialVersionUID = 1L;
	private Collection<Attribute> items;
	private static final String DATATYPE_ATTRIBUTE = Attribute.class.getSimpleName();

	public QDataAttributeMessage() {
		super(DATATYPE_ATTRIBUTE);
	}

	public QDataAttributeMessage(Collection<Attribute> items) {

		super(DATATYPE_ATTRIBUTE);
		setItems(items);
	}

	/** 
	 * @return Attribute[]
	 */
	public Collection<Attribute> getItems() {
		return items;
	}

	/** 
	 * @param items the array of attributes to set
	 */
	public void setItems(Collection<Attribute> items) {
		items = items.stream()
			.filter(this::shouldSend)
			.collect(Collectors.toList());
		this.items = items;
	}


	public void add(Collection<Attribute> attributes) {
		Collection<Attribute> items = this.getItems() != null ? new CopyOnWriteArrayList<>(this.getItems())
				: new CopyOnWriteArrayList<>();
		attributes.forEach(attribute -> {
			if(shouldSend(attribute)) {
				items.add(attribute);
			}
		});
		this.setItems(items);
	}

	private boolean shouldSend(Attribute attribute) {
		return !StringUtils.isBlank(attribute.getCode()) && !attribute.getCode().startsWith(Prefix.CAP_);
	}

}
