package life.genny.qwandaq.utils.ickle.predicate;

import life.genny.qwandaq.utils.ickle.IckleCriteriaBuilder;
import life.genny.qwandaq.utils.ickle.IckleRenderingContext;
import life.genny.qwandaq.utils.ickle.expression.IckleExpression;
import life.genny.qwandaq.utils.ickle.parameter.IckleParameterContainer;
import life.genny.qwandaq.utils.ickle.parameter.IckleParameterRegistry;

import java.io.Serializable;

public class BooleanExpressionIcklePredicate extends AbstractSimpleIcklePredicate implements Serializable {
    private final IckleExpression<Boolean> expression;

    public BooleanExpressionIcklePredicate(IckleCriteriaBuilder criteriaBuilder, IckleExpression<Boolean> expression) {
        super( criteriaBuilder );
        this.expression = expression;
    }

    /**
     * Get the boolean expression defining the predicate.
     *
     * @return The underlying boolean expression.
     */
    public IckleExpression<Boolean> getExpression() {
        return expression;
    }

    public void registerParameters(IckleParameterRegistry registry) {
        IckleParameterContainer.Helper.possibleParameter(expression, registry);
    }

    @Override
    public String render(boolean isNegated, IckleRenderingContext renderingContext) {
        return getExpression().render( renderingContext );
    }
}
