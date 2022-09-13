package life.genny.qwandaq.entity.search.clause;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * And
 */
@RegisterForReflection
public class And extends Clause {

  public And() {
    super();
  }

  public And(ClauseArgument a, ClauseArgument b) {
    super(a, b);
  }

}
