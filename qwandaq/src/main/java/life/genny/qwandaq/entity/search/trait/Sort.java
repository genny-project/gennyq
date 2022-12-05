package life.genny.qwandaq.entity.search.trait;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.constants.Prefix;

/**
 * Sort
 */
@RegisterForReflection
public class Sort extends Trait {

	public static final String PREFIX = Prefix.SRT;

	private Ord order;

	public Sort() {
		super();
	}

	public Sort(String code, Ord order) {
		super(code, code);
		this.order = order;
	}

	public Ord getOrder() {
		return order;
	}

	public void setOrder(Ord order) {
		this.order = order;
	}

	public void flipOrd() {
		setOrder(order.getOpposite());
	}

	public String toString() {
		return new StringBuilder(super.toString())
					.append(", ord=")
					.append(order.name())
					.toString();
	}

}
