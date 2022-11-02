package life.genny.qwandaq.utils.ickle.predicate;

import life.genny.qwandaq.utils.ickle.IckleCriteriaBuilder;
import life.genny.qwandaq.utils.ickle.IckleRenderingContext;
import life.genny.qwandaq.utils.ickle.Renderable;
import life.genny.qwandaq.utils.ickle.expression.IckleExpression;
import life.genny.qwandaq.utils.ickle.parameter.IckleParameterContainer;
import life.genny.qwandaq.utils.ickle.parameter.IckleParameterRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompoundIcklePredicate extends AbstractIcklePredicateImpl {
    private IcklePredicate.BooleanOperator operator;
    private final List<IckleExpression<Boolean>> expressions = new ArrayList<IckleExpression<Boolean>>();

    /**
     * Constructs an empty conjunction or disjunction.
     *
     * @param criteriaBuilder The query builder from which this originates.
     * @param operator        Indicates whether this predicate will function
     *                        as a conjunction or disjunction.
     */
    public CompoundIcklePredicate(IckleCriteriaBuilder criteriaBuilder, IcklePredicate.BooleanOperator operator) {
        super(criteriaBuilder);
        this.operator = operator;
    }

    /**
     * Constructs a conjunction or disjunction over the given expressions.
     *
     * @param criteriaBuilder The query builder from which this originates.
     * @param operator        Indicates whether this predicate will function
     *                        as a conjunction or disjunction.
     * @param expressions     The expressions to be grouped.
     */
    public CompoundIcklePredicate(IckleCriteriaBuilder criteriaBuilder, IcklePredicate.BooleanOperator operator, IckleExpression<Boolean>... expressions) {
        this(criteriaBuilder, operator);
        applyExpressions(expressions);
    }

    /**
     * Constructs a conjunction or disjunction over the given expressions.
     *
     * @param criteriaBuilder The query builder from which this originates.
     * @param operator        Indicates whether this predicate will function
     *                        as a conjunction or disjunction.
     * @param expressions     The expressions to be grouped.
     */
    public CompoundIcklePredicate(IckleCriteriaBuilder criteriaBuilder, IcklePredicate.BooleanOperator operator, List<IckleExpression<Boolean>> expressions) {
        this(criteriaBuilder, operator);
        applyExpressions(expressions);
    }

    private void applyExpressions(IckleExpression<Boolean>... expressions) {
        applyExpressions(Arrays.asList(expressions));
    }

    private void applyExpressions(List<IckleExpression<Boolean>> expressions) {
        this.expressions.clear();
        final IckleCriteriaBuilder criteriaBuilder = criteriaBuilder();
        for (IckleExpression<Boolean> expression : expressions) {
            this.expressions.addAll(criteriaBuilder.wrap(expression).getExpressions());
        }
    }

    @Override
    public IcklePredicate.BooleanOperator getOperator() {
        return operator;
    }

    @Override
    public List<IckleExpression<Boolean>> getExpressions() {
        return expressions;
    }

    public void registerParameters(IckleParameterRegistry registry) {
        for (IckleExpression expression : getExpressions()) {
            IckleParameterContainer.Helper.possibleParameter(expression, registry);
        }
    }

    @Override
    public String render(IckleRenderingContext renderingContext) {
        return render(isNegated(), renderingContext);
    }

    @Override
    public boolean isJunction() {
        return true;
    }

    @Override
    public String render(boolean isNegated, IckleRenderingContext renderingContext) {
        return render(this, renderingContext);
    }

    @Override
    public IcklePredicate not() {
        return new NegatedIcklePredicateWrapper(this);
    }

    private void toggleOperator() {
        this.operator = reverseOperator(this.operator);
    }

    public static IcklePredicate.BooleanOperator reverseOperator(IcklePredicate.BooleanOperator operator) {
        return operator == IcklePredicate.BooleanOperator.AND ? IcklePredicate.BooleanOperator.OR : IcklePredicate.BooleanOperator.AND;
    }

    public static String render(IcklePredicate predicate, IckleRenderingContext renderingContext) {
        if (!predicate.isJunction()) {
            throw new IllegalStateException("CompoundPredicate.render should only be used to render junctions");
        }

        // for junctions, the negation is already cooked into the expressions and operator; we just need to render
        // them as is

        if (predicate.getExpressions().isEmpty()) {
            boolean implicitTrue = predicate.getOperator() == IcklePredicate.BooleanOperator.AND;
            // AND is always true for empty; OR is always false
            return implicitTrue ? "1=1" : "0=1";
        }

        // single valued junction ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        if (predicate.getExpressions().size() == 1) {
            return ((Renderable) predicate.getExpressions().get(0)).render(renderingContext);
        }

        final StringBuilder buffer = new StringBuilder();
        String sep = "";
        for (IckleExpression expression : predicate.getExpressions()) {
            buffer.append(sep).append("( ").append(((Renderable) expression).render(renderingContext)).append(" )");
            sep = operatorTextWithSeparator(predicate.getOperator());
        }
        return buffer.toString();
    }

    private static String operatorTextWithSeparator(IcklePredicate.BooleanOperator operator) {
        return operator == IcklePredicate.BooleanOperator.AND ? " and " : " or ";
    }
}
