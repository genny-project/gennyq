package life.genny.qwandaq.utils.ickle.expression;

import life.genny.qwandaq.utils.ickle.IckleCriteriaBuilder;
import life.genny.qwandaq.utils.ickle.IckleRenderingContext;
import life.genny.qwandaq.utils.ickle.IckleSelectionImpl;
import life.genny.qwandaq.utils.ickle.predicate.IcklePredicate;
import org.hibernate.query.criteria.internal.ExpressionImplementor;
import org.hibernate.query.criteria.internal.ValueHandlerFactory;
import org.hibernate.query.criteria.internal.expression.function.CastFunction;

import javax.persistence.criteria.Expression;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;

public abstract class IckleExpressionImpl<T> extends IckleSelectionImpl<T> implements IckleExpression<T> {
    private IckleCriteriaBuilder ickleCriteriaBuilder;

    private Class<T> javaType;

    private ValueHandlerFactory.ValueHandler<T> valueHandler;

    public IckleExpressionImpl(IckleCriteriaBuilder ickleCriteriaBuilder, Class<T> javaType) {
        super(ickleCriteriaBuilder, javaType);
        this.ickleCriteriaBuilder = ickleCriteriaBuilder;
        this.javaType = javaType;
    }

    public <X> IckleExpression<X> as(Class<X> type) {
        /*return type.equals( getJavaType() )
                ? (IckleExpression<X>) this
                : new CastFunction<X, T>( criteriaBuilder(), type, this );*/
        return (IckleExpression<X>) this;

    }

    public IcklePredicate isNull() {
        return ickleCriteriaBuilder.isNull( this );
    }

    /**
     *  Create a ickle-predicate to test whether the expression is 
     *  not null.
     *  @return ickle-predicate testing whether the expression is not null
     */
    public IcklePredicate isNotNull() {
        return ickleCriteriaBuilder.isNotNull( this );
    }

    /**
     * Create a ickle-predicate to test whether the expression is a member
     * of the argument list.
     * @param values  values to be tested against
     * @return ickle-predicate testing for membership
     */
    public IcklePredicate in(Object... values) {
        return ickleCriteriaBuilder.in( this, values );
    }

    /**
     * Create a ickle-predicate to test whether the expression is a member
     * of the argument list.
     * @param values  expressions to be tested against
     * @return ickle-predicate testing for membership
     */
    public IcklePredicate in(IckleExpression<?>... values) {
        return ickleCriteriaBuilder.in( this, values );
    }

    /**
     * Create a ickle-predicate to test whether the expression is a member
     * of the collection.
     * @param values  collection of values to be tested against
     * @return ickle-predicate testing for membership
     */
    public IcklePredicate in(Collection<?> values) {
        return ickleCriteriaBuilder.in( this, values.toArray() );
    }

    /**
     * Create a ickle-predicate to test whether the expression is a member
     * of the collection.
     * @param values expression corresponding to collection to be
     *        tested against
     * @return ickle-predicate testing for membership
     */
    public IcklePredicate in(IckleExpression<Collection<?>> values) {
        return ickleCriteriaBuilder.in( this, values );
    }

    public Class<T> getJavaType() {
        return javaType;
    }

    public IckleCriteriaBuilder criteriaBuilder() {
        return ickleCriteriaBuilder;
    }

    public abstract String render(IckleRenderingContext renderingContext);

    @SuppressWarnings({ "unchecked" })
    public ExpressionImplementor<Long> asLong() {
        resetJavaType( Long.class );
        return (ExpressionImplementor<Long>) this;
    }

    @SuppressWarnings({ "unchecked" })
    public ExpressionImplementor<Integer> asInteger() {
        resetJavaType( Integer.class );
        return (ExpressionImplementor<Integer>) this;
    }

    @SuppressWarnings({ "unchecked" })
    public ExpressionImplementor<Float> asFloat() {
        resetJavaType( Float.class );
        return (ExpressionImplementor<Float>) this;
    }

    @SuppressWarnings({ "unchecked" })
    public ExpressionImplementor<Double> asDouble() {
        resetJavaType( Double.class );
        return (ExpressionImplementor<Double>) this;
    }

    @SuppressWarnings({ "unchecked" })
    public ExpressionImplementor<BigDecimal> asBigDecimal() {
        resetJavaType( BigDecimal.class );
        return (ExpressionImplementor<BigDecimal>) this;
    }

    @SuppressWarnings({ "unchecked" })
    public ExpressionImplementor<BigInteger> asBigInteger() {
        resetJavaType( BigInteger.class );
        return (ExpressionImplementor<BigInteger>) this;
    }

    @SuppressWarnings({ "unchecked" })
    public ExpressionImplementor<String> asString() {
        resetJavaType( String.class );
        return (ExpressionImplementor<String>) this;
    }

    @SuppressWarnings({ "unchecked" })
    protected void resetJavaType(Class targetType) {
        this.javaType = targetType;
//		this.valueHandler = javaType.equals( originalJavaType )
//				? null
//				: ValueHandlerFactory.determineAppropriateHandler( javaType );
        this.valueHandler = ValueHandlerFactory.determineAppropriateHandler( javaType );
    }
}
