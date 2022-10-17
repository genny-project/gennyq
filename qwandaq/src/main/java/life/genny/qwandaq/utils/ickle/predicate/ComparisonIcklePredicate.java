package life.genny.qwandaq.utils.ickle.predicate;

import life.genny.qwandaq.utils.ickle.IckleCriteriaBuilder;
import life.genny.qwandaq.utils.ickle.IckleRenderingContext;
import life.genny.qwandaq.utils.ickle.expression.BinaryOperatorIckleExpression;
import life.genny.qwandaq.utils.ickle.expression.IckleExpression;
import life.genny.qwandaq.utils.ickle.expression.LiteralIckleExpression;
import life.genny.qwandaq.utils.ickle.parameter.IckleParameterContainer;
import life.genny.qwandaq.utils.ickle.parameter.IckleParameterRegistry;
import org.hibernate.query.criteria.internal.ValueHandlerFactory;

import java.io.Serializable;

public class ComparisonIcklePredicate extends AbstractSimpleIcklePredicate implements BinaryOperatorIckleExpression<Boolean>, Serializable {
    private final ComparisonIcklePredicate.ComparisonOperator comparisonOperator;
    private final IckleExpression<?> leftHandSide;
    private final IckleExpression<?> rightHandSide;

    public ComparisonIcklePredicate(IckleCriteriaBuilder criteriaBuilder, ComparisonIcklePredicate.ComparisonOperator comparisonOperator, IckleExpression<?> leftHandSide, IckleExpression<?> rightHandSide) {
        super(criteriaBuilder);
        this.comparisonOperator = comparisonOperator;
        this.leftHandSide = leftHandSide;
        this.rightHandSide = rightHandSide;
    }

    @SuppressWarnings({"unchecked"})
    public ComparisonIcklePredicate(IckleCriteriaBuilder criteriaBuilder, ComparisonIcklePredicate.ComparisonOperator comparisonOperator, IckleExpression<?> leftHandSide, Object rightHandSide) {
        super(criteriaBuilder);
        this.comparisonOperator = comparisonOperator;
        this.leftHandSide = leftHandSide;
        if (ValueHandlerFactory.isNumeric(leftHandSide.getJavaType())) {
            this.rightHandSide = new LiteralIckleExpression(criteriaBuilder, ValueHandlerFactory.convert(rightHandSide, (Class<Number>) leftHandSide.getJavaType()));
        } else {
            this.rightHandSide = new LiteralIckleExpression(criteriaBuilder, rightHandSide);
        }
    }

    @SuppressWarnings({"unchecked"})
    public <N extends Number> ComparisonIcklePredicate(IckleCriteriaBuilder criteriaBuilder, ComparisonIcklePredicate.ComparisonOperator comparisonOperator, IckleExpression<N> leftHandSide, Number rightHandSide) {
        super(criteriaBuilder);
        this.comparisonOperator = comparisonOperator;
        this.leftHandSide = leftHandSide;
        Class type = leftHandSide.getJavaType();
        if (Number.class.equals(type)) {
            this.rightHandSide = new LiteralIckleExpression(criteriaBuilder, rightHandSide);
        } else {
            N converted = (N) ValueHandlerFactory.convert(rightHandSide, type);
            this.rightHandSide = new LiteralIckleExpression<N>(criteriaBuilder, converted);
        }
    }

    public ComparisonIcklePredicate.ComparisonOperator getComparisonOperator() {
        return getComparisonOperator(isNegated());
    }

    public ComparisonIcklePredicate.ComparisonOperator getComparisonOperator(boolean isNegated) {
        return isNegated ? comparisonOperator.negated() : comparisonOperator;
    }

    @Override
    public IckleExpression getLeftHandOperand() {
        return leftHandSide;
    }

    @Override
    public IckleExpression getRightHandOperand() {
        return rightHandSide;
    }

    public void registerParameters(IckleParameterRegistry registry) {
        IckleParameterContainer.Helper.possibleParameter(getLeftHandOperand(), registry);
        IckleParameterContainer.Helper.possibleParameter(getRightHandOperand(), registry);
    }

    /**
     * Defines the comparison operators.  We could also get away with
     * only 3 and use negation...
     */
    public static enum ComparisonOperator {
        EQUAL {
            public ComparisonIcklePredicate.ComparisonOperator negated() {
                return NOT_EQUAL;
            }

            public String rendered() {
                return "=";
            }
        }, NOT_EQUAL {
            public ComparisonIcklePredicate.ComparisonOperator negated() {
                return EQUAL;
            }

            public String rendered() {
                return "<>";
            }
        }, LESS_THAN {
            public ComparisonIcklePredicate.ComparisonOperator negated() {
                return GREATER_THAN_OR_EQUAL;
            }

            public String rendered() {
                return "<";
            }
        }, LESS_THAN_OR_EQUAL {
            public ComparisonIcklePredicate.ComparisonOperator negated() {
                return GREATER_THAN;
            }

            public String rendered() {
                return "<=";
            }
        }, GREATER_THAN {
            public ComparisonIcklePredicate.ComparisonOperator negated() {
                return LESS_THAN_OR_EQUAL;
            }

            public String rendered() {
                return ">";
            }
        }, GREATER_THAN_OR_EQUAL {
            public ComparisonIcklePredicate.ComparisonOperator negated() {
                return LESS_THAN;
            }

            public String rendered() {
                return ">=";
            }
        };

        public abstract ComparisonIcklePredicate.ComparisonOperator negated();

        public abstract String rendered();
    }


    @Override
    public String render(boolean isNegated, IckleRenderingContext renderingContext) {
        return getLeftHandOperand().render(renderingContext) + getComparisonOperator(isNegated).rendered() + getRightHandOperand().render(renderingContext);
    }
}
