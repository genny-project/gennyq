package life.genny.qwandaq.entity.search.trait;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Sort
 */
@RegisterForReflection
public class Sort extends Trait {

	public static final String PREFIX = "SRT_";

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

}
