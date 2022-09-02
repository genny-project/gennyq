package life.genny.qwandaq.entity.search;

/**
 * Sort
 */
public class Sort extends Trait {

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
