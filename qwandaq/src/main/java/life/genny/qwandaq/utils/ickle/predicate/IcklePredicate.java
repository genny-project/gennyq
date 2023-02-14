package life.genny.qwandaq.utils.ickle.predicate;

import life.genny.qwandaq.utils.ickle.IckleCriteriaBuilder;
import life.genny.qwandaq.utils.ickle.IckleRenderingContext;
import life.genny.qwandaq.utils.ickle.expression.IckleExpression;

import java.util.List;

public interface IcklePredicate extends IckleExpression<Boolean> {
    public static enum BooleanOperator {
        AND, OR
    }

    /**
     * Return the boolean operator for the predicate.
     * If the predicate is simple, this is <code>AND</code>.
     *
     * @return boolean operator for the predicate
     */
    IcklePredicate.BooleanOperator getOperator();

    /**
     * Whether the predicate has been created from another
     * predicate by applying the <code>Predicate.not()</code> method
     * or the <code>CriteriaBuilder.not()</code> method.
     *
     * @return boolean indicating if the predicate is
     * a negated predicate
     */
    boolean isNegated();

    /**
     * Return the top-level conjuncts or disjuncts of the predicate.
     * Returns empty list if there are no top-level conjuncts or
     * disjuncts of the predicate.
     * Modifications to the list do not affect the query.
     *
     * @return list of boolean expressions forming the predicate
     */
    List<IckleExpression<Boolean>> getExpressions();

    /**
     * Create a negation of the predicate.
     *
     * @return negated predicate
     */
    IcklePredicate not();

    /**
     * Is this a conjunction or disjunction?
     *
     * @return {@code true} if this predicate is a junction (AND/OR); {@code false} otherwise
     */
    public boolean isJunction();

    /**
     * Access to the CriteriaBuilder
     *
     * @return The CriteriaBuilder
     */
    public IckleCriteriaBuilder criteriaBuilder();

    public String render(boolean isNegated, IckleRenderingContext renderingContext);
}
