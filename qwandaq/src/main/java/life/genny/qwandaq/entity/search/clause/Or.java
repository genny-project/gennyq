package life.genny.qwandaq.entity.search.clause;

/**
 * Or
 */
public class Or extends Clause implements ClauseArgument {

  public Or() {
    super();
  }

  public Or(ClauseArgument a, ClauseArgument b) {
    super(a, b);
  }

}
