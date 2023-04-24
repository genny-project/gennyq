package life.genny.qwandaq.message;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.Ask;

import java.util.HashSet;
import java.util.Set;

@RegisterForReflection
public class QDataAskMessage extends QDataMessage {

    private static final long serialVersionUID = 1L;
    private Set<Ask> items;
    public static final String DATATYPE_ASK = Ask.class.getSimpleName();

    public QDataAskMessage() {
        super(DATATYPE_ASK);
	}

	public QDataAskMessage(Set<Ask> items) {

		super(DATATYPE_ASK);
		setItems(items);
	}

	public QDataAskMessage(Ask ask) {

		super(DATATYPE_ASK);
        Set<Ask> asks = new HashSet<>();
		asks.add(ask);
		setItems(asks);
	}

	/**
	 * @return List
	 */
	public Set<Ask> getItems() {
		return this.items;
	}

	/**
	 * @param asks the array of asks to set
	 */
	public void setItems(Set<Ask> asks) {
		this.items = asks;
	}

	public void add(Ask ask) {
		if (this.items == null) {
			this.items = new HashSet<>();
		}
		this.items.add(ask);
	}
}
