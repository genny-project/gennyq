package life.genny.qwandaq.utils.ickle.predicate;

import life.genny.qwandaq.utils.ickle.IckleCriteriaBuilder;
import life.genny.qwandaq.utils.ickle.IckleRenderingContext;
import life.genny.qwandaq.utils.ickle.expression.IckleExpression;
import life.genny.qwandaq.utils.ickle.expression.IckleExpressionImpl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NegatedIcklePredicateWrapper extends IckleExpressionImpl<Boolean> implements IcklePredicate, Serializable {
    private final IcklePredicate predicate;
    private final IcklePredicate.BooleanOperator negatedOperator;
    private final List<IckleExpression<Boolean>> negatedExpressions;

    @SuppressWarnings("unchecked")
    public NegatedIcklePredicateWrapper(IcklePredicate predicate) {
        super(predicate.criteriaBuilder(), Boolean.class);
        this.predicate = predicate;
        this.negatedOperator = predicate.isJunction()
                ? CompoundIcklePredicate.reverseOperator(predicate.getOperator())
                : predicate.getOperator();
        this.negatedExpressions = negateCompoundExpressions(predicate.getExpressions(), predicate.criteriaBuilder());
    }

    private static List<IckleExpression<Boolean>> negateCompoundExpressions(
            List<IckleExpression<Boolean>> expressions,
            IckleCriteriaBuilder criteriaBuilder) {
        if (expressions == null || expressions.isEmpty()) {
            return Collections.emptyList();
        }

        final List<IckleExpression<Boolean>> negatedExpressions = new ArrayList<>();
        for (IckleExpression<Boolean> expression : expressions) {
            if (IcklePredicate.class.isInstance(expression)) {
                negatedExpressions.addAll(((IcklePredicate) expression).not().getExpressions());
            } else {
                negatedExpressions.addAll(criteriaBuilder.not(expression).getExpressions());
            }
        }
        return negatedExpressions;
    }

    @Override
    public IcklePredicate.BooleanOperator getOperator() {
        return negatedOperator;
    }

    public boolean isJunction() {
        return false;
    }

    @Override
    public IckleCriteriaBuilder criteriaBuilder() {
        return predicate.criteriaBuilder();
    }

    @Override
    public boolean isNegated() {
        return !predicate.isNegated();
    }

    @Override
    public List<IckleExpression<Boolean>> getExpressions() {
        return negatedExpressions;
    }

    @Override
    public IcklePredicate not() {
        return new NegatedIcklePredicateWrapper(this);
    }

    @Override
    public String render(boolean isNegated, IckleRenderingContext renderingContext) {
        if (isJunction()) {
            return CompoundIcklePredicate.render(this, renderingContext);
        } else {
            return predicate.render(isNegated, renderingContext);
        }
    }

    @Override
    public String render(IckleRenderingContext renderingContext) {
        return render(isNegated(), renderingContext);
    }
}
