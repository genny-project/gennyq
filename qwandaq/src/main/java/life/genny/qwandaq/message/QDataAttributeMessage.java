package life.genny.qwandaq.message;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import life.genny.qwandaq.attribute.Attribute;

public class QDataAttributeMessage extends QDataMessage{

	private static final long serialVersionUID = 1L;
	private Attribute[] items;
	private static final String DATATYPE_ATTRIBUTE = Attribute.class.getSimpleName();

	public QDataAttributeMessage() {
		super(DATATYPE_ATTRIBUTE);
	}

	public QDataAttributeMessage(Attribute[] items) {

		super(DATATYPE_ATTRIBUTE);
		setItems(items);
	}

	/** 
	 * @return Attribute[]
	 */
	public Attribute[] getItems() {
		return items;
	}

	/** 
	 * @param items the array of attributes to set
	 */
	public void setItems(Attribute[] items) {
		this.items = items;
	}


	public void add(List<Attribute> attributes) {
		List<Attribute> items = this.getItems() != null ? new CopyOnWriteArrayList<>(this.getItems())
				: new CopyOnWriteArrayList<>();
		items.addAll(attributes);
		this.setItems(items.toArray(new Attribute[items.size()]));
	}

}
