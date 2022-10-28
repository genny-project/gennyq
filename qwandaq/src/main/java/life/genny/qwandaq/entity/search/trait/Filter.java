package life.genny.qwandaq.entity.search.trait;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import javax.json.bind.annotation.JsonbTransient;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.entity.search.clause.ClauseArgument;
import life.genny.qwandaq.exception.runtime.DebugException;

/**
 * Filter
 */
@RegisterForReflection
public class Filter extends Trait implements ClauseArgument {

	private Operator operator;
	private Object value;

	@JsonbTransient
	private Class<?> c;
	private String className;

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

	private Filter(String code, Operator operator, Object value, Class<?> c) {
		super(code, code);
		this.operator = operator;
		this.value = value;
		this.c = c;
		this.className = c.getName();
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

	public Class<?> getC() {

		if (c == null) {
			try {
				this.c = Class.forName(className);
			} catch (ClassNotFoundException e) {
				throw new DebugException(e.getMessage());
			} catch (NullPointerException e) {
				throw new DebugException("className is null for filter with code " + getCode());
			}
		}

		return c;
	}

	public void setC(Class<?> c) {
		this.c = c;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public enum FILTER_TYPE {
		PRIMARY,
		EXTRA
	}

	private FILTER_TYPE type;

	public FILTER_TYPE getType() {
		return type;
	}

	public void setType(FILTER_TYPE type) {
		this.type = type;
	}

	public Filter(String code, Operator operator, Object value, FILTER_TYPE type) {
		super(code, code);
		this.operator = operator;
		this.value = value;
		this.c = value.getClass();
		this.className = value.getClass().getName();
		this.type = type;
	}
}
