package life.genny.qwandaq.message;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.Ask;

@RegisterForReflection
public class QDataAskMessage extends QDataMessage {

	private static final long serialVersionUID = 1L;
	private List<Ask> items;
	private static final String DATATYPE_ASK = Ask.class.getSimpleName();

	public QDataAskMessage() { }

	public QDataAskMessage(List<Ask> items) {

		super(DATATYPE_ASK);
		setItems(items);
	}

	public QDataAskMessage(Ask ask) {

		super(DATATYPE_ASK);
		List<Ask> asks = new ArrayList<Ask>();
		asks.add(ask);
		setItems(asks);
	}

	/**
	 * @return List
	 */
	public List<Ask> getItems() {
		return this.items;
	}

	/**
	 * @param asks the array of asks to set
	 */
	public void setItems(List<Ask> asks) {
		this.items = asks;
	}

	public void add(Ask ask) {
		if (this.items == null) {
			this.items = new ArrayList<Ask>();
		}
		this.items.add(ask);
	}
}
