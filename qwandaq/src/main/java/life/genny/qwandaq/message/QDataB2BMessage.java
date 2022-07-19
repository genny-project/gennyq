package life.genny.qwandaq.message;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.models.GennyItem;

@RegisterForReflection
public class QDataB2BMessage extends QDataMessage<GennyItem> {

	private static final long serialVersionUID = 1L;

	public QDataB2BMessage() {
		super();
	}

	public QDataB2BMessage(GennyItem item) {
		super(item);
	}

	public QDataB2BMessage(List<GennyItem> list) {
		super(list);
	}

}
