package life.genny.qwandaq.entity.search.trait;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Column
 */
@RegisterForReflection
public class Column extends Trait {

	public static final String PREFIX = "COL_";

	public Column() {
		super();
	}

	public Column(String code, String name) {
		super(code, name);
	}

}
