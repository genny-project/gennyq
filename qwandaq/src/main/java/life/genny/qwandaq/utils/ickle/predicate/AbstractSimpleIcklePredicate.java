package life.genny.qwandaq.utils.ickle.predicate;

import life.genny.qwandaq.utils.ickle.IckleCriteriaBuilder;
import life.genny.qwandaq.utils.ickle.IckleRenderingContext;
import life.genny.qwandaq.utils.ickle.expression.IckleExpression;
import org.hibernate.query.criteria.internal.compile.RenderingContext;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public abstract class AbstractSimpleIcklePredicate extends AbstractIcklePredicateImpl implements Serializable {
    private static final List<IckleExpression<Boolean>> NO_EXPRESSIONS = Collections.emptyList();

    public AbstractSimpleIcklePredicate(IckleCriteriaBuilder criteriaBuilder) {
        super( criteriaBuilder );
    }

    @Override
    public boolean isJunction() {
        return false;
    }

    @Override
    public IcklePredicate.BooleanOperator getOperator() {
        return IcklePredicate.BooleanOperator.AND;
    }

    @Override
    public final List<IckleExpression<Boolean>> getExpressions() {
        return NO_EXPRESSIONS;
    }

    public String render(IckleRenderingContext renderingContext) {
        return render( isNegated(), renderingContext );
    }
}
