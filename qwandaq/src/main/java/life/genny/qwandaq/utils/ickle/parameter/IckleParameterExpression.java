package life.genny.qwandaq.utils.ickle.parameter;

import life.genny.qwandaq.utils.ickle.IckleCriteriaBuilder;
import life.genny.qwandaq.utils.ickle.expression.IckleExpressionImpl;

/**
 * Type of criteria query parameter expressions.
 *
 * @param <T> the type of the parameter expression
 */
public abstract class IckleParameterExpression<T> extends IckleExpressionImpl<T> implements IckleParameter<T> {
    public IckleParameterExpression(IckleCriteriaBuilder ickleCriteriaBuilder, Class<T> javaType) {
        super(ickleCriteriaBuilder, javaType);
    }
}