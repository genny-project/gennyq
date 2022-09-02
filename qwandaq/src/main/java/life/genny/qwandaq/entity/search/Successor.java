package life.genny.qwandaq.entity.search;

/**
 * Successor
 */
public class Successor {

	public enum Operation {
		AND,
		OR
	}

	private Filter filter;
	private Operation operation;

	public Successor() {
	}

	public Successor(Filter filter, Operation operation) {
		this.filter = filter;
		this.operation = operation;
	}

	public Filter getFilter() {
		return filter;
	}

	public void setFilter(Filter filter) {
		this.filter = filter;
	}

	public Operation getOperation() {
		return operation;
	}

	public void setOperation(Operation operation) {
		this.operation = operation;
	}

}
