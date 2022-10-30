package life.genny.qwandaq.utils.ickle.predicate;

import life.genny.qwandaq.utils.ickle.IckleCriteriaBuilder;
import life.genny.qwandaq.utils.ickle.IckleRenderingContext;
import life.genny.qwandaq.utils.ickle.expression.IckleExpression;
import life.genny.qwandaq.utils.ickle.parameter.IckleParameterContainer;
import life.genny.qwandaq.utils.ickle.parameter.IckleParameterRegistry;

/**
 * Defines a {@link life.genny.qwandaq.utils.ickle.predicate.IcklePredicate} for checking the
 * nullness state of an expression, aka an IS [NOT] NULL predicate.
 *
 * The NOT NULL form can be built by calling the constructor and then
 * calling {@link #not}.
 *
 * @author Steve Ebersole
 */
public class NullnessIcklePredicate extends AbstractSimpleIcklePredicate {
    private final IckleExpression<?> operand;

    /**
     * Constructs the affirmative form of nullness checking (<i>IS NULL</i>).  To
     * construct the negative form (<i>IS NOT NULL</i>) call {@link #not} on the
     * constructed instance.
     *
     * @param criteriaBuilder The query builder from whcih this originates.
     * @param operand         The expression to check.
     */
    public NullnessIcklePredicate(IckleCriteriaBuilder criteriaBuilder, IckleExpression<?> operand) {
        super(criteriaBuilder);
        this.operand = operand;
    }

    public IckleExpression<?> getOperand() {
        return operand;
    }

    public void registerParameters(IckleParameterRegistry registry) {
        IckleParameterContainer.Helper.possibleParameter(getOperand(), registry);
    }

    @Override
    public String render(boolean isNegated, IckleRenderingContext renderingContext) {
        return operand.render(renderingContext) + check(isNegated);
    }

    private String check(boolean negated) {
        return negated ? " is not null" : " is null";
    }
}