package life.genny.qwandaq.utils.ickle.expression;

import life.genny.qwandaq.utils.ickle.IckleSelection;
import life.genny.qwandaq.utils.ickle.Renderable;
import life.genny.qwandaq.utils.ickle.predicate.IcklePredicate;

import java.util.Collection;

/**
 * Type for query expressions.
 *
 * @param <T> the type of the expression
 *
 * @since 2.0
 */
public interface IckleExpression<T> extends IckleSelection<T>, Renderable {

    /**
     *  Create a predicate to test whether the expression is null.
     *  @return predicate testing whether the expression is null
     */
    IcklePredicate isNull();

    /**
     *  Create a predicate to test whether the expression is
     *  not null.
     *  @return predicate testing whether the expression is not null
     */
    IcklePredicate isNotNull();

    /**
     * Create a predicate to test whether the expression is a member
     * of the argument list.
     * @param values  values to be tested against
     * @return predicate testing for membership
     */
    IcklePredicate in(Object... values);

    /**
     * Create a predicate to test whether the expression is a member
     * of the argument list.
     * @param values  expressions to be tested against
     * @return predicate testing for membership
     */
    IcklePredicate in(IckleExpression<?>... values);

    /**
     * Create a predicate to test whether the expression is a member
     * of the collection.
     * @param values  collection of values to be tested against
     * @return predicate testing for membership
     */
    IcklePredicate in(Collection<?> values);

    /**
     * Create a predicate to test whether the expression is a member
     * of the collection.
     * @param values expression corresponding to collection to be
     *        tested against
     * @return predicate testing for membership
     */
    IcklePredicate in(IckleExpression<Collection<?>> values);

    /**
     * Perform a typecast upon the expression, returning a new
     * expression object.
     * This method does not cause type conversion:
     * the runtime type is not changed.
     * Warning: may result in a runtime failure.
     * @param type  intended type of the expression
     * @return new expression of the given type
     */
    <X> IckleExpression<X> as(Class<X> type);
}
