package life.genny.qwandaq.entity.search;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Filter
 */
public class Filter {

	private String code;
	private Operator operator;
	private Object value;
	private Class c;

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
		this.code = code;
		this.operator = operator;
		this.value = value;
		this.c = c;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
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
}
