package life.genny.qwandaq.message;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.Ask;

@RegisterForReflection
public class QDataAskMessage extends QDataMessage {

	private static final long serialVersionUID = 1L;
	private Ask[] items;
	private static final String DATATYPE_ASK = Ask.class.getSimpleName();

	public QDataAskMessage() { }

	public QDataAskMessage(Ask[] items) {

		super(DATATYPE_ASK);
		setItems(items);
	}

	public QDataAskMessage(List<Ask> items) {

		this(items.toArray(new Ask[items.size()]));
	}

	public QDataAskMessage(Ask ask) {

		this(new Ask[]{ ask });
	}

	/**
	 * Get The ask items.
	 *
	 * @return Ask[]
	 */
	public Ask[] getItems() {
		return this.items;
	}

	/**
	 * Set the ask items.
	 *
	 * @param asks the array of asks to set
	 */
	public void setItems(Ask[] asks) {
		this.items = asks;
	}

}
