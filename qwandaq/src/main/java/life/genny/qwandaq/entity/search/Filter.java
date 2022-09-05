package life.genny.qwandaq.entity.search;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.entity.search.Successor.Operation;

/**
 * Filter
 */
@RegisterForReflection
public class Filter extends Trait {

	private Operator operator;
	private Object value;
	private Class c;
	private Successor successor;
	private Capability capability;

	public Filter() {
		super();
	}

	public Filter(String code, Operator operator, String value) {
		this(code, operator, value, String.class);
	}

	public Filter(String code, Boolean value) {
		this(code, Operator.EQUALS, value, Boolean.class);
	}

	public Filter(String code, Operator operator, Integer value) {
		this(code, operator, value, Integer.class);
	}

	public Filter(String code, Operator operator, Long value) {
		this(code, operator, value, Long.class);
	}

	public Filter(String code, Operator operator, Double value) {
		this(code, operator, value, Double.class);
	}

	public Filter(String code, Operator operator, LocalDateTime value) {
		this(code, operator, value, LocalDateTime.class);
	}

	public Filter(String code, Operator operator, LocalDate value) {
		this(code, operator, value, LocalDate.class);
	}

	public Filter(String code, Operator operator, LocalTime value) {
		this(code, operator, value, LocalTime.class);
	}

	private Filter(String code, Operator operator, Object value, Class c) {
		super(code, code);
		this.operator = operator;
		this.value = value;
		this.c = c;
	}

	public Operator getOperator() {
		return operator;
	}

	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public Class getC() {
		return c;
	}

	public void setC(Class c) {
		this.c = c;
	}

	public Successor getSuccessor() {
		return successor;
	}

	public void setSuccessor(Successor successor) {
		this.successor = successor;
	}

	public Boolean hasSuccessor() {
		return (this.successor == null ? false : true);
	}

	public Boolean hasCapability() {
		return (this.capability == null ? false : true);
	}

	public Filter and(Filter filter) {
		this.successor = new Successor(filter, Operation.AND);
		return this;
	}

	public Filter or(Filter filter) {
		this.successor = new Successor(filter, Operation.OR);
		return this;
	}

	public Filter add(Capability capability) {
		this.capability = capability;
		return this;
	}

}
