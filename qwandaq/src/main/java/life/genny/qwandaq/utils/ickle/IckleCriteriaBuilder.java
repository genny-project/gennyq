package life.genny.qwandaq.utils.ickle;

import life.genny.qwandaq.utils.ickle.expression.IckleExpression;
import life.genny.qwandaq.utils.ickle.expression.LiteralIckleExpression;
import life.genny.qwandaq.utils.ickle.expression.function.AggregationFunction;
import life.genny.qwandaq.utils.ickle.predicate.*;
import org.hibernate.query.criteria.internal.PathImplementor;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collection;

@ApplicationScoped
public class IckleCriteriaBuilder {
    public IcklePredicate not(IckleExpression<Boolean> expression) {
        return wrap(expression).not();
    }

    @SuppressWarnings("unchecked")
    public IcklePredicate and(IckleExpression<Boolean> x, IckleExpression<Boolean> y) {
        return new CompoundIcklePredicate(this, IcklePredicate.BooleanOperator.AND, x, y);
    }

    @SuppressWarnings("unchecked")
    public IcklePredicate or(IckleExpression<Boolean> x, IckleExpression<Boolean> y) {
        return new CompoundIcklePredicate(this, IcklePredicate.BooleanOperator.OR, x, y);
    }

    public IcklePredicate and(IcklePredicate... restrictions) {
        return new CompoundIcklePredicate(this, IcklePredicate.BooleanOperator.AND, restrictions);
    }

    public IcklePredicate or(IcklePredicate... restrictions) {
        return new CompoundIcklePredicate(this, IcklePredicate.BooleanOperator.OR, restrictions);
    }

    public IcklePredicate conjunction() {
        return new CompoundIcklePredicate(this, IcklePredicate.BooleanOperator.AND);
    }

    public IcklePredicate disjunction() {
        return new CompoundIcklePredicate(this, IcklePredicate.BooleanOperator.OR);
    }

    public IcklePredicate isTrue(IckleExpression<Boolean> expression) {
        if (CompoundIcklePredicate.class.isInstance(expression)) {
            final CompoundIcklePredicate predicate = (CompoundIcklePredicate) expression;
            if (predicate.getExpressions().size() == 0) {
                return new BooleanStaticAssertionIcklePredicate(this, predicate.getOperator() == IcklePredicate.BooleanOperator.AND);
            }
            return predicate;
        } else if (IcklePredicate.class.isInstance(expression)) {
            return (IcklePredicate) expression;
        }
        return new BooleanAssertionIcklePredicate(this, expression, Boolean.TRUE);
    }

    public IcklePredicate isFalse(IckleExpression<Boolean> expression) {
        if (CompoundIcklePredicate.class.isInstance(expression)) {
            final CompoundIcklePredicate predicate = (CompoundIcklePredicate) expression;
            if (predicate.getExpressions().size() == 0) {
                return new BooleanStaticAssertionIcklePredicate(this, predicate.getOperator() == IcklePredicate.BooleanOperator.OR);
            }
            predicate.not();
            return predicate;
        } else if (IcklePredicate.class.isInstance(expression)) {
            final IcklePredicate predicate = (IcklePredicate) expression;
            predicate.not();
            return predicate;
        }
        return new BooleanAssertionIcklePredicate(this, expression, Boolean.FALSE);
    }

    public IcklePredicate isNull(IckleExpression<?> x) {
        return new NullnessIcklePredicate(this, x);
    }

    public IcklePredicate isNotNull(IckleExpression<?> x) {
        return isNull(x).not();
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public IcklePredicate equal(IckleExpression<?> x, IckleExpression<?> y) {
        return new ComparisonIcklePredicate(this, ComparisonIcklePredicate.ComparisonOperator.EQUAL, x, y);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public IcklePredicate notEqual(IckleExpression<?> x, IckleExpression<?> y) {
        return new ComparisonIcklePredicate(this, ComparisonIcklePredicate.ComparisonOperator.NOT_EQUAL, x, y);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public IcklePredicate equal(IckleExpression<?> x, Object y) {
        return new ComparisonIcklePredicate(this, ComparisonIcklePredicate.ComparisonOperator.EQUAL, x, y);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public IcklePredicate notEqual(IckleExpression<?> x, Object y) {
        return new ComparisonIcklePredicate(this, ComparisonIcklePredicate.ComparisonOperator.NOT_EQUAL, x, y);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public <Y extends Comparable<? super Y>> IcklePredicate greaterThan(IckleExpression<? extends Y> x, IckleExpression<? extends Y> y) {
        return new ComparisonIcklePredicate(this, ComparisonIcklePredicate.ComparisonOperator.GREATER_THAN, x, y);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public <Y extends Comparable<? super Y>> IcklePredicate lessThan(IckleExpression<? extends Y> x, IckleExpression<? extends Y> y) {
        return new ComparisonIcklePredicate(this, ComparisonIcklePredicate.ComparisonOperator.LESS_THAN, x, y);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public <Y extends Comparable<? super Y>> IcklePredicate greaterThanOrEqualTo(IckleExpression<? extends Y> x, IckleExpression<? extends Y> y) {
        return new ComparisonIcklePredicate(this, ComparisonIcklePredicate.ComparisonOperator.GREATER_THAN_OR_EQUAL, x, y);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public <Y extends Comparable<? super Y>> IcklePredicate lessThanOrEqualTo(IckleExpression<? extends Y> x, IckleExpression<? extends Y> y) {
        return new ComparisonIcklePredicate(this, ComparisonIcklePredicate.ComparisonOperator.LESS_THAN_OR_EQUAL, x, y);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public <Y extends Comparable<? super Y>> IcklePredicate greaterThan(IckleExpression<? extends Y> x, Y y) {
        return new ComparisonIcklePredicate(this, ComparisonIcklePredicate.ComparisonOperator.GREATER_THAN, x, y);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public <Y extends Comparable<? super Y>> IcklePredicate lessThan(IckleExpression<? extends Y> x, Y y) {
        return new ComparisonIcklePredicate(this, ComparisonIcklePredicate.ComparisonOperator.LESS_THAN, x, y);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public <Y extends Comparable<? super Y>> IcklePredicate greaterThanOrEqualTo(IckleExpression<? extends Y> x, Y y) {
        return new ComparisonIcklePredicate(this, ComparisonIcklePredicate.ComparisonOperator.GREATER_THAN_OR_EQUAL, x, y);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public <Y extends Comparable<? super Y>> IcklePredicate lessThanOrEqualTo(IckleExpression<? extends Y> x, Y y) {
        return new ComparisonIcklePredicate(this, ComparisonIcklePredicate.ComparisonOperator.LESS_THAN_OR_EQUAL, x, y);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public IcklePredicate gt(IckleExpression<? extends Number> x, IckleExpression<? extends Number> y) {
        return new ComparisonIcklePredicate(this, ComparisonIcklePredicate.ComparisonOperator.GREATER_THAN, x, y);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public IcklePredicate lt(IckleExpression<? extends Number> x, IckleExpression<? extends Number> y) {
        return new ComparisonIcklePredicate(this, ComparisonIcklePredicate.ComparisonOperator.LESS_THAN, x, y);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public IcklePredicate ge(IckleExpression<? extends Number> x, IckleExpression<? extends Number> y) {
        return new ComparisonIcklePredicate(this, ComparisonIcklePredicate.ComparisonOperator.GREATER_THAN_OR_EQUAL, x, y);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public IcklePredicate le(IckleExpression<? extends Number> x, IckleExpression<? extends Number> y) {
        return new ComparisonIcklePredicate(this, ComparisonIcklePredicate.ComparisonOperator.LESS_THAN_OR_EQUAL, x, y);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public IcklePredicate gt(IckleExpression<? extends Number> x, Number y) {
        return new ComparisonIcklePredicate(this, ComparisonIcklePredicate.ComparisonOperator.GREATER_THAN, x, y);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public IcklePredicate lt(IckleExpression<? extends Number> x, Number y) {
        return new ComparisonIcklePredicate(this, ComparisonIcklePredicate.ComparisonOperator.LESS_THAN, x, y);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public IcklePredicate ge(IckleExpression<? extends Number> x, Number y) {
        return new ComparisonIcklePredicate(this, ComparisonIcklePredicate.ComparisonOperator.GREATER_THAN_OR_EQUAL, x, y);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public IcklePredicate le(IckleExpression<? extends Number> x, Number y) {
        return new ComparisonIcklePredicate(this, ComparisonIcklePredicate.ComparisonOperator.LESS_THAN_OR_EQUAL, x, y);
    }

    public <Y extends Comparable<? super Y>> IcklePredicate between(IckleExpression<? extends Y> expression, Y lowerBound, Y upperBound) {
        return new BetweenIcklePredicate<Y>(this, expression, lowerBound, upperBound);
    }

    public <Y extends Comparable<? super Y>> IcklePredicate between(IckleExpression<? extends Y> expression, IckleExpression<? extends Y> lowerBound, IckleExpression<? extends Y> upperBound) {
        return new BetweenIcklePredicate<Y>(this, expression, lowerBound, upperBound);
    }

    public <T> InIcklePredicate<T> in(IckleExpression<? extends T> expression) {
        return new InIcklePredicate<T>(this, expression);
    }

    public <T> InIcklePredicate<T> in(IckleExpression<? extends T> expression, IckleExpression<? extends T>... values) {
        return new InIcklePredicate<T>(this, expression, values);
    }

    public <T> InIcklePredicate<T> in(IckleExpression<? extends T> expression, T... values) {
        return new InIcklePredicate<T>(this, expression, values);
    }

    public <T> InIcklePredicate<T> in(IckleExpression<? extends T> expression, Collection<T> values) {
        return new InIcklePredicate<T>(this, expression, values);
    }

    public IcklePredicate like(IckleExpression<String> matchExpression, IckleExpression<String> pattern) {
        return new LikeIcklePredicate(this, matchExpression, pattern);
    }

    public IcklePredicate like(IckleExpression<String> matchExpression, IckleExpression<String> pattern, IckleExpression<Character> escapeCharacter) {
        return new LikeIcklePredicate(this, matchExpression, pattern, escapeCharacter);
    }

    public IcklePredicate like(IckleExpression<String> matchExpression, IckleExpression<String> pattern, char escapeCharacter) {
        return new LikeIcklePredicate(this, matchExpression, pattern, escapeCharacter);
    }

    public IcklePredicate like(IckleExpression<String> matchExpression, String pattern) {
        return new LikeIcklePredicate(this, matchExpression, pattern);
    }

    public IcklePredicate like(IckleExpression<String> matchExpression, String pattern, IckleExpression<Character> escapeCharacter) {
        return new LikeIcklePredicate(this, matchExpression, pattern, escapeCharacter);
    }

    public IcklePredicate like(IckleExpression<String> matchExpression, String pattern, char escapeCharacter) {
        return new LikeIcklePredicate(this, matchExpression, pattern, escapeCharacter);
    }

    public IcklePredicate notLike(IckleExpression<String> matchExpression, IckleExpression<String> pattern) {
        return like(matchExpression, pattern).not();
    }

    public IcklePredicate notLike(IckleExpression<String> matchExpression, IckleExpression<String> pattern, IckleExpression<Character> escapeCharacter) {
        return like(matchExpression, pattern, escapeCharacter).not();
    }

    public IcklePredicate notLike(IckleExpression<String> matchExpression, IckleExpression<String> pattern, char escapeCharacter) {
        return like(matchExpression, pattern, escapeCharacter).not();
    }

    public IcklePredicate notLike(IckleExpression<String> matchExpression, String pattern) {
        return like(matchExpression, pattern).not();
    }

    public IcklePredicate notLike(IckleExpression<String> matchExpression, String pattern, IckleExpression<Character> escapeCharacter) {
        return like(matchExpression, pattern, escapeCharacter).not();
    }

    public IcklePredicate notLike(IckleExpression<String> matchExpression, String pattern, char escapeCharacter) {
        return like(matchExpression, pattern, escapeCharacter).not();
    }

    // aggregate functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public <N extends Number> IckleExpression<Double> avg(IckleExpression<N> x) {
        return new AggregationFunction.AVG(this, x);
    }

    public <N extends Number> IckleExpression<N> sum(IckleExpression<N> x) {
        return new AggregationFunction.SUM<N>(this, x);
    }

    public IckleExpression<Long> sumAsLong(IckleExpression<Integer> x) {
        return new AggregationFunction.SUM<Long>(this, x, Long.class);
    }

    public IckleExpression<Double> sumAsDouble(IckleExpression<Float> x) {
        return new AggregationFunction.SUM<Double>(this, x, Double.class);
    }

    public <N extends Number> IckleExpression<N> max(IckleExpression<N> x) {
        return new AggregationFunction.MAX<N>(this, x);
    }

    public <N extends Number> IckleExpression<N> min(IckleExpression<N> x) {
        return new AggregationFunction.MIN<N>(this, x);
    }

    @SuppressWarnings({"unchecked"})
    public <X extends Comparable<? super X>> IckleExpression<X> greatest(IckleExpression<X> x) {
        return new AggregationFunction.GREATEST(this, x);
    }

    @SuppressWarnings({"unchecked"})
    public <X extends Comparable<? super X>> IckleExpression<X> least(IckleExpression<X> x) {
        return new AggregationFunction.LEAST(this, x);
    }

    public IckleExpression<Long> count(IckleExpression<?> x) {
        return new AggregationFunction.COUNT(this, x, false);
    }

    public IckleExpression<Long> countDistinct(IckleExpression<?> x) {
        return new AggregationFunction.COUNT(this, x, true);
    }

    public IcklePredicate wrap(IckleExpression<Boolean> expression) {
        if (IcklePredicate.class.isInstance(expression)) {
            return ((IcklePredicate) expression);
        } else if (PathImplementor.class.isInstance(expression)) {
            return new BooleanAssertionIcklePredicate(this, expression, Boolean.TRUE);
        } else {
            return new BooleanAssertionIcklePredicate(this, expression, Boolean.FALSE);
        }
    }

    public <T> IckleExpression<T> literal(T value) {
        if (value == null) {
            throw new IllegalArgumentException("literal value cannot be null");
        }
        return new LiteralIckleExpression<T>(this, value);
    }
}
