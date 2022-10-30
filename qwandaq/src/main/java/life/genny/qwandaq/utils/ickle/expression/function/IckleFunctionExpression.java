package life.genny.qwandaq.utils.ickle.expression.function;

import life.genny.qwandaq.utils.ickle.IckleCriteriaBuilder;
import life.genny.qwandaq.utils.ickle.IckleRenderingContext;
import life.genny.qwandaq.utils.ickle.expression.IckleExpressionImpl;
import life.genny.qwandaq.utils.ickle.parameter.IckleParameterRegistry;

public abstract class IckleFunctionExpression<T> extends IckleExpressionImpl<T> {
    public IckleFunctionExpression(IckleCriteriaBuilder ickleCriteriaBuilder, Class<T> javaType) {
        super(ickleCriteriaBuilder, javaType);
    }

    /**
     * Retrieve the name of the function.
     *
     * @return The function name.
     */
    public abstract String getFunctionName();

    /**
     * Is this function a value aggregator (like a COUNT or MAX function e.g.)?
     *
     * @return True if this functions does aggregation.
     */
    public abstract boolean isAggregation();

    public void registerParameters(IckleParameterRegistry registry) {
        // nothing to do here...
    }

    public String render(IckleRenderingContext renderingContext) {
        return getFunctionName() + "()";
    }
}
