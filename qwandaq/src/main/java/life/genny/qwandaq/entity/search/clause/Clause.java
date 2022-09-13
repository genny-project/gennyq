package life.genny.qwandaq.entity.search.clause;

import life.genny.qwandaq.entity.search.trait.Filter;

/**
 * Clause
 */
public class Clause {

  private ClauseContainer a, b;

  public Clause() {
  }

  public Clause(ClauseArgument a, ClauseArgument b) {
    this.a = createContainer(a);
    this.b = createContainer(b);
  }

  public ClauseContainer getA() {
    return a;
  }

  public void setA(ClauseContainer a) {
    this.a = a;
  }

  public ClauseContainer getB() {
    return b;
  }

  public void setB(ClauseContainer b) {
    this.b = b;
  }

  private ClauseContainer createContainer(ClauseArgument clauseArgument) {

    ClauseContainer container = new ClauseContainer();
    if (clauseArgument instanceof And)
      container.setAnd((And) clauseArgument);
    if (clauseArgument instanceof And)
      container.setOr((Or) clauseArgument);
    if (clauseArgument instanceof And)
      container.setFilter((Filter) clauseArgument);

    return container;
  }

}
