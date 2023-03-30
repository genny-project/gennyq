package life.genny.qwandaq.message;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.Ask;

import java.util.HashSet;
import java.util.Set;

@RegisterForReflection
public class QDataAskMessage extends QDataMessage {

    private static final long serialVersionUID = 1L;
    private Set<Ask> items = new HashSet<>();
    private static final String DATATYPE_ASK = Ask.class.getSimpleName();

    public QDataAskMessage() {
        super(DATATYPE_ASK);
	}

	public QDataAskMessage(Set<Ask> items) {
		super(DATATYPE_ASK);
		setItems(items);
	}

	public QDataAskMessage(Ask ask) {
		super(DATATYPE_ASK);
		add(ask);
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
		asks.stream().forEach(ask -> {
			if(ask.getQuestion() != null && ask.getQuestion().getCapabilityRequirements() != null) {
				ask.getQuestion().getCapabilityRequirements().clear();
			}
		});
		this.items = asks;
	}

	public void add(Ask ask) {
		if(ask.getQuestion() != null && ask.getQuestion().getCapabilityRequirements() != null) {
			ask.getQuestion().getCapabilityRequirements().clear();
		}
		this.items.add(ask);
	}
}
