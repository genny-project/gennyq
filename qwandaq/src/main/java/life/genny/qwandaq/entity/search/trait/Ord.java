package life.genny.qwandaq.entity.search.trait;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public enum Ord {
	ASC,
	DESC;

	public Ord getOpposite() {
		return values()[Math.abs(this.ordinal() - 1)];
	}
}
