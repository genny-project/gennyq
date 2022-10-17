package life.genny.qwandaq.utils.ickle.expression.function;

import life.genny.qwandaq.utils.ickle.IckleCriteriaBuilder;
import life.genny.qwandaq.utils.ickle.IckleRenderingContext;
import life.genny.qwandaq.utils.ickle.Renderable;
import life.genny.qwandaq.utils.ickle.expression.IckleExpression;
import life.genny.qwandaq.utils.ickle.expression.LiteralIckleExpression;

import javax.persistence.criteria.Root;
import java.util.Arrays;
import java.util.List;

public class AggregationFunction<T> extends IckleFunctionExpression {

    private String functionName;

    private final List<IckleExpression<?>> argumentExpressions;

    private Class<T> javaType;

    /**
     * Constructs an aggregation function with a single literal argument.
     *
     * @param criteriaBuilder The query builder instance.
     * @param returnType The function return type.
     * @param functionName The name of the function.
     * @param argument The literal argument
     */
    @SuppressWarnings({ "unchecked" })
    public AggregationFunction(
            IckleCriteriaBuilder criteriaBuilder,
            Class<T> returnType,
            String functionName,
            Object argument) {
        this( criteriaBuilder, returnType, functionName, new LiteralIckleExpression( criteriaBuilder, argument ) );
    }

    /**
     * Constructs an aggregation function with a single literal argument.
     *
     * @param criteriaBuilder The query builder instance.
     * @param returnType The function return type.
     * @param functionName The name of the function.
     * @param argument The argument
     */
    public AggregationFunction(
            IckleCriteriaBuilder criteriaBuilder,
            Class<T> returnType,
            String functionName,
            IckleExpression<?> argument) {
        super(criteriaBuilder, returnType);
        this.functionName = functionName;
        argumentExpressions = Arrays.asList( argument );
    }

    public String render(IckleRenderingContext renderingContext) {
        renderingContext.getFunctionStack().push( this );

        try {
            final StringBuilder buffer = new StringBuilder();
            buffer.append( functionName ).append( "(" );
            renderArguments( buffer, renderingContext );
            return buffer.append( ')' ).toString();
        }
        finally {
            renderingContext.getFunctionStack().pop();
        }
    }

    protected void renderArguments(StringBuilder buffer, IckleRenderingContext renderingContext) {
        String sep = "";
        for ( IckleExpression argument : argumentExpressions ) {
            buffer.append( sep ).append( ( (Renderable) argument ).render( renderingContext ) );
            sep = ", ";
        }
    }

    protected List<IckleExpression<?>> getArgumentExpressions() {
        return argumentExpressions;
    }

    @SuppressWarnings({ "unchecked" })
    protected void resetJavaType(Class targetType) {
        this.javaType = targetType;
//		this.valueHandler = javaType.equals( originalJavaType )
//				? null
//				: ValueHandlerFactory.determineAppropriateHandler( javaType );
        // this.valueHandler = ValueHandlerFactory.determineAppropriateHandler( javaType );
    }

    public String getFunctionName() {
        return functionName;
    }

    @Override
    public boolean isAggregation() {
        return true;
    }

    /**
     * Implementation of a COUNT function providing convenience in construction.
     * 
     * Parameterized as {@link Long} because thats what JPA states
     * that the return from COUNT should be.
     */
    public static class COUNT extends AggregationFunction<Long> {
        public static final String NAME = "count";

        private final boolean distinct;

        public COUNT(IckleCriteriaBuilder criteriaBuilder, IckleExpression<?> expression, boolean distinct) {
            super( criteriaBuilder, Long.class, NAME , expression );
            this.distinct = distinct;
        }

        @Override
        protected void renderArguments(StringBuilder buffer, IckleRenderingContext renderingContext) {
            if ( isDistinct() ) {
                buffer.append("distinct ");
            }
            else {
                // If function specifies a single non-distinct entity with ID, its alias would normally be rendered, which ends up
                // converting to the column(s) associated with the entity's ID in the rendered SQL.  However, some DBs don't support
                // the multiple columns that would end up here for entities with composite IDs.  So, since we modify the query to
                // instead specify star since that's functionally equivalent and supported by all DBs.
                List<IckleExpression<?>> argExprs = getArgumentExpressions();
                if (argExprs.size() == 1) {
                    IckleExpression argExpr = argExprs.get(0);
                    if (argExpr instanceof Root<?>) {
                        Root<?> root = (Root<?>)argExpr;
                        if (!root.getModel().hasSingleIdAttribute()) {
                            buffer.append('*');
                            return;
                        }
                    }
                }
            }
            super.renderArguments(buffer, renderingContext);
        }

        public boolean isDistinct() {
            return distinct;
        }
    }

    /**
     * Implementation of a AVG function providing convenience in construction.
     * 
     * Parameterized as {@link Double} because thats what JPA states that the return from AVG should be.
     */
    public static class AVG extends AggregationFunction<Double> {
        public static final String NAME = "avg";

        public AVG(IckleCriteriaBuilder criteriaBuilder, IckleExpression<? extends Number> expression) {
            super( criteriaBuilder, Double.class, NAME, expression );
        }
    }

    /**
     * Implementation of a SUM function providing convenience in construction.
     * 
     * Parameterized as {@link Number N extends Number} because thats what JPA states
     * that the return from SUM should be.
     */
    public static class SUM<N extends Number> extends AggregationFunction<N> {
        public static final String NAME = "sum";

        @SuppressWarnings({ "unchecked" })
        public SUM(IckleCriteriaBuilder criteriaBuilder, IckleExpression<N> expression) {
            super( criteriaBuilder, (Class<N>)expression.getJavaType(), NAME , expression);
            // force the use of a ValueHandler
            resetJavaType( expression.getJavaType() );
        }

        public SUM(IckleCriteriaBuilder criteriaBuilder, IckleExpression<? extends Number> expression, Class<N> returnType) {
            super( criteriaBuilder, returnType, NAME , expression);
            // force the use of a ValueHandler
            resetJavaType( returnType );
        }
    }

    /**
     * Implementation of a MIN function providing convenience in construction.
     * 
     * Parameterized as {@link Number N extends Number} because thats what JPA states
     * that the return from MIN should be.
     */
    public static class MIN<N extends Number> extends AggregationFunction<N> {
        public static final String NAME = "min";

        @SuppressWarnings({ "unchecked" })
        public MIN(IckleCriteriaBuilder criteriaBuilder, IckleExpression<N> expression) {
            super( criteriaBuilder, ( Class<N> ) expression.getJavaType(), NAME , expression);
        }
    }

    /**
     * Implementation of a MAX function providing convenience in construction.
     * 
     * Parameterized as {@link Number N extends Number} because thats what JPA states
     * that the return from MAX should be.
     */
    public static class MAX<N extends Number> extends AggregationFunction<N> {
        public static final String NAME = "max";

        @SuppressWarnings({ "unchecked" })
        public MAX(IckleCriteriaBuilder criteriaBuilder, IckleExpression<N> expression) {
            super( criteriaBuilder, ( Class<N> ) expression.getJavaType(), NAME , expression);
        }
    }

    /**
     * Models  the MIN function in terms of non-numeric expressions.
     *
     * @see AggregationFunction.MIN
     */
    public static class LEAST<X extends Comparable<X>> extends AggregationFunction<X> {
        public static final String NAME = "min";

        @SuppressWarnings({ "unchecked" })
        public LEAST(IckleCriteriaBuilder criteriaBuilder, IckleExpression<X> expression) {
            super( criteriaBuilder, ( Class<X> ) expression.getJavaType(), NAME , expression);
        }
    }

    /**
     * Models  the MAX function in terms of non-numeric expressions.
     *
     * @see AggregationFunction.MAX
     */
    public static class GREATEST<X extends Comparable<X>> extends AggregationFunction<X> {
        public static final String NAME = "max";

        @SuppressWarnings({ "unchecked" })
        public GREATEST(IckleCriteriaBuilder criteriaBuilder, IckleExpression<X> expression) {
            super( criteriaBuilder, ( Class<X> ) expression.getJavaType(), NAME , expression);
        }
    }
}
