package life.genny.qwandaq.entity.search.clause;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.entity.search.trait.Filter;

/**
 * ClauseArgument
 */
@RegisterForReflection
public class ClauseContainer {

  private Filter filter;
  private And and;
  private Or or;

  public ClauseContainer() {
  }

  public ClauseContainer(Filter filter) {
    this.filter = filter;
  }

  public ClauseContainer(And and) {
    this.and = and;
  }

  public ClauseContainer(Or or) {
    this.or = or;
  }

  public Filter getFilter() {
    return filter;
  }

  public void setFilter(Filter filter) {
    this.filter = filter;
  }

  public And getAnd() {
    return and;
  }

  public void setAnd(And and) {
    this.and = and;
  }

  public Or getOr() {
    return or;
  }

  public void setOr(Or or) {
    this.or = or;
  }

}
