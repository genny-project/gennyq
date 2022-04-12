package life.genny.qwandaq.message;

import java.io.Serializable;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.Ask;

@RegisterForReflection
public class QDataAskMessage extends QDataMessage {

	private static final long serialVersionUID = 1L;
	private Ask[] items;
	private static final String DATATYPE_ASK = Ask.class.getSimpleName();

	public QDataAskMessage() {
		// super(); // removed for native execution
	}

	public QDataAskMessage(Ask[] items) {

		super(DATATYPE_ASK);
		setItems(items);
	}

	public QDataAskMessage(Ask ask) {

		super(DATATYPE_ASK);
		Ask[] asks = new Ask[1];
		asks[0] = ask;
		setItems(asks);
	}

	/**
	 * @return Ask[]
	 */
	public Ask[] getItems() {
		return this.items;
	}

	/**
	 * @param asks the array of asks to set
	 */
	public void setItems(Ask[] asks) {
		this.items = asks;
	}

}
