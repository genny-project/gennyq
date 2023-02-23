package life.genny.qwandaq.utils.ickle.predicate;

import life.genny.qwandaq.utils.ickle.IckleCriteriaBuilder;
import life.genny.qwandaq.utils.ickle.IckleRenderingContext;
import life.genny.qwandaq.utils.ickle.expression.IckleExpression;
import life.genny.qwandaq.utils.ickle.parameter.IckleParameterContainer;
import life.genny.qwandaq.utils.ickle.parameter.IckleParameterRegistry;

public class BetweenIcklePredicate<Y> extends AbstractSimpleIcklePredicate {
    private final IckleExpression<? extends Y> expression;
    private final IckleExpression<? extends Y> lowerBound;
    private final IckleExpression<? extends Y> upperBound;

    public BetweenIcklePredicate(IckleCriteriaBuilder criteriaBuilder, IckleExpression<? extends Y> expression, Y lowerBound, Y upperBound) {
        this(criteriaBuilder, expression, criteriaBuilder.literal(lowerBound), criteriaBuilder.literal(upperBound));
    }

    public BetweenIcklePredicate(IckleCriteriaBuilder criteriaBuilder, IckleExpression<? extends Y> expression, IckleExpression<? extends Y> lowerBound, IckleExpression<? extends Y> upperBound) {
        super(criteriaBuilder);
        this.expression = expression;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public IckleExpression<? extends Y> getExpression() {
        return expression;
    }

    public IckleExpression<? extends Y> getLowerBound() {
        return lowerBound;
    }

    public IckleExpression<? extends Y> getUpperBound() {
        return upperBound;
    }

    public void registerParameters(IckleParameterRegistry registry) {
        IckleParameterContainer.Helper.possibleParameter(getExpression(), registry);
        IckleParameterContainer.Helper.possibleParameter(getLowerBound(), registry);
        IckleParameterContainer.Helper.possibleParameter(getUpperBound(), registry);
    }

    @Override
    public String render(boolean isNegated, IckleRenderingContext renderingContext) {
        final String operator = isNegated ? " not between " : " between ";
        return getExpression().render(renderingContext) + operator + getLowerBound().render(renderingContext) + " and " + getUpperBound().render(renderingContext);
    }
}
