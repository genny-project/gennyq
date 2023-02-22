package life.genny.qwandaq.utils.ickle.predicate;

import life.genny.qwandaq.utils.ickle.IckleCriteriaBuilder;
import life.genny.qwandaq.utils.ickle.IckleRenderingContext;
import life.genny.qwandaq.utils.ickle.Renderable;
import life.genny.qwandaq.utils.ickle.expression.IckleExpression;
import life.genny.qwandaq.utils.ickle.parameter.IckleParameterContainer;
import life.genny.qwandaq.utils.ickle.parameter.IckleParameterRegistry;

import java.io.Serializable;

public class BooleanAssertionIcklePredicate extends AbstractSimpleIcklePredicate implements Serializable {
    private final IckleExpression<Boolean> expression;
    private final Boolean assertedValue;

    public BooleanAssertionIcklePredicate(IckleCriteriaBuilder criteriaBuilder, IckleExpression<Boolean> expression, Boolean assertedValue) {
        super(criteriaBuilder);
        this.expression = expression;
        this.assertedValue = assertedValue;
    }

    public IckleExpression<Boolean> getExpression() {
        return expression;
    }

    public Boolean getAssertedValue() {
        return assertedValue;
    }

    public void registerParameters(IckleParameterRegistry registry) {
        IckleParameterContainer.Helper.possibleParameter(expression, registry);
    }

    @Override
    public String render(boolean isNegated, IckleRenderingContext renderingContext) {
        final String operator = isNegated ? " <> " : " = ";
        final String assertionLiteral = assertedValue ? "true" : "false";

        return ((Renderable) expression).render(renderingContext) + operator + assertionLiteral;
    }
}
