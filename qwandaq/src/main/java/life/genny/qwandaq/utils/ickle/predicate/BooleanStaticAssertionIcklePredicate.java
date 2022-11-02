package life.genny.qwandaq.utils.ickle.predicate;

import life.genny.qwandaq.utils.ickle.IckleCriteriaBuilder;
import life.genny.qwandaq.utils.ickle.IckleRenderingContext;
import life.genny.qwandaq.utils.ickle.parameter.IckleParameterRegistry;

/**
 * Predicate used to assert a static boolean condition.
 */
public class BooleanStaticAssertionIcklePredicate
        extends AbstractSimpleIcklePredicate {
    private final Boolean assertedValue;

    public BooleanStaticAssertionIcklePredicate(
            IckleCriteriaBuilder criteriaBuilder,
            Boolean assertedValue) {
        super(criteriaBuilder);
        this.assertedValue = assertedValue;
    }

    public Boolean getAssertedValue() {
        return assertedValue;
    }

    public void registerParameters(IckleParameterRegistry registry) {
        // nada
    }

    public String render(boolean isNegated, IckleRenderingContext renderingContext) {
        boolean isTrue = getAssertedValue();
        if (isNegated) {
            isTrue = !isTrue;
        }
        return isTrue ? "1=1" : "0=1";
    }

}