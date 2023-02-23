package life.genny.qwandaq.utils.ickle.predicate;

import life.genny.qwandaq.utils.ickle.IckleCriteriaBuilder;
import life.genny.qwandaq.utils.ickle.IckleRenderingContext;

import java.io.Serializable;

public class IsEmptyIcklePredicate<C> extends AbstractSimpleIcklePredicate implements Serializable {

    // private final PluralAttributePath<C> collectionPath;

    private String operand;

    public IsEmptyIcklePredicate(
            IckleCriteriaBuilder criteriaBuilder,
            String operand) {
        super(criteriaBuilder);
        this.operand = operand;
    }

    public String getOperand() {
        return operand;
    }

    public String render(boolean isNegated, IckleRenderingContext renderingContext) {
        final String operator = isNegated ? " is not empty" : " is empty";
        // return getOperand().render( renderingContext ) + operator;
        return operand + operator;
    }
}
