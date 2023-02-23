package life.genny.qwandaq.utils.ickle.predicate;

import life.genny.qwandaq.utils.ickle.IckleCriteriaBuilder;
import life.genny.qwandaq.utils.ickle.IckleSelection;
import life.genny.qwandaq.utils.ickle.Renderable;
import life.genny.qwandaq.utils.ickle.expression.IckleExpressionImpl;

import java.util.List;

public abstract class AbstractIcklePredicateImpl extends IckleExpressionImpl<Boolean> implements IcklePredicate, Renderable {
    protected AbstractIcklePredicateImpl(IckleCriteriaBuilder criteriaBuilder) {
        super(criteriaBuilder, Boolean.class);
    }

    public boolean isNegated() {
        return false;
    }

    public IcklePredicate not() {
        return new NegatedIcklePredicateWrapper(this);
    }


    // Selection ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public final boolean isCompoundSelection() {
        // Should always be false for predicates
        return false;
    }

    public final List<IckleSelection<?>> getCompoundSelectionItems() {
        throw new IllegalStateException( "Not a compound selection" );
    }
}
