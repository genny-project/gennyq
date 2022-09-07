package life.genny.qwandaq.entity.search.trait;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * AssociatedColumn
 */
@RegisterForReflection
public class AssociatedColumn extends Trait {

	public static final String PREFIX = "COL__";
	public static final String DELIMITER = "__";

	public AssociatedColumn() {
		super();
	}

	public AssociatedColumn(String... fields) {

		super();
		String code = String.format("COL_%s", 
			Stream.of(fields).limit(fields.length-1).collect(Collectors.joining("__")));
		String name = fields[fields.length-1];

		setCode(code);
		setName(name);
	}

}
