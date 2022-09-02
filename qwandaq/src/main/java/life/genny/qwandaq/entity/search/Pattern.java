package life.genny.qwandaq.entity.search;

import java.util.ArrayList;
import java.util.List;

/**
 * Pattern
 */
public class Pattern {

	List<Filter> filters = new ArrayList<>();

	public Pattern(String code, Operator operator, Object value) {
	}	

	public Pattern or(Pattern pattern) {

		return this;
	}

}
