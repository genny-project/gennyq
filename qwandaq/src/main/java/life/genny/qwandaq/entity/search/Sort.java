package life.genny.qwandaq.entity.search;

/**
 * Sort
 */
public class Sort {

	private String code;
	private Ord order;

	public Sort() {
	}

	public Sort(String code, Ord order) {
		this.code = code;
		this.order = order;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Ord getOrder() {
		return order;
	}

	public void setOrder(Ord order) {
		this.order = order;
	}

}
