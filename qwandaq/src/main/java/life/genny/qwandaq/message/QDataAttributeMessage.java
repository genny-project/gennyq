package life.genny.qwandaq.message;

import life.genny.qwandaq.attribute.Attribute;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
		this.items = items;
	}


	public void add(Collection<Attribute> attributes) {
		Collection<Attribute> items = this.getItems() != null ? new CopyOnWriteArrayList<>(this.getItems())
				: new CopyOnWriteArrayList<>();
		items.addAll(attributes);
		this.setItems(items);
	}

}
