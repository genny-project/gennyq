package life.genny.qwandaq.utils.ickle.predicate;

import life.genny.qwandaq.utils.ickle.IckleCriteriaBuilder;
import life.genny.qwandaq.utils.ickle.IckleRenderingContext;
import life.genny.qwandaq.utils.ickle.expression.IckleExpression;
import life.genny.qwandaq.utils.ickle.expression.LiteralIckleExpression;
import life.genny.qwandaq.utils.ickle.parameter.IckleParameterContainer;
import life.genny.qwandaq.utils.ickle.parameter.IckleParameterRegistry;
import org.hibernate.query.criteria.internal.ValueHandlerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class InIcklePredicate<T> extends AbstractSimpleIcklePredicate implements Serializable {
    private final IckleExpression<? extends T> expression;
    private final List<IckleExpression<? extends T>> values;

    /**
     * Constructs an IN predicate against a given expression with an empty list of values.
     *
     * @param criteriaBuilder The query builder from which this originates.
     * @param expression      The expression.
     */
    public InIcklePredicate(IckleCriteriaBuilder criteriaBuilder, IckleExpression<? extends T> expression) {
        this(criteriaBuilder, expression, new ArrayList<IckleExpression<? extends T>>());
    }

    /**
     * Constructs an IN predicate against a given expression with the given list of expression values.
     *
     * @param criteriaBuilder The query builder from which this originates.
     * @param expression      The expression.
     * @param values          The value list.
     */
    public InIcklePredicate(IckleCriteriaBuilder criteriaBuilder, IckleExpression<? extends T> expression, IckleExpression<? extends T>... values) {
        this(criteriaBuilder, expression, Arrays.asList(values));
    }

    /**
     * Constructs an IN predicate against a given expression with the given list of expression values.
     *
     * @param criteriaBuilder The query builder from which this originates.
     * @param expression      The expression.
     * @param values          The value list.
     */
    public InIcklePredicate(IckleCriteriaBuilder criteriaBuilder, IckleExpression<? extends T> expression, List<IckleExpression<? extends T>> values) {
        super(criteriaBuilder);
        this.expression = expression;
        this.values = values;
    }

    /**
     * Constructs an IN predicate against a given expression with the given given literal value list.
     *
     * @param criteriaBuilder The query builder from which this originates.
     * @param expression      The expression.
     * @param values          The value list.
     */
    public InIcklePredicate(IckleCriteriaBuilder criteriaBuilder, IckleExpression<? extends T> expression, T... values) {
        this(criteriaBuilder, expression, Arrays.asList(values));
    }

    /**
     * Constructs an IN predicate against a given expression with the given literal value list.
     *
     * @param criteriaBuilder The query builder from which this originates.
     * @param expression      The expression.
     * @param values          The value list.
     */
    @SuppressWarnings("unchecked")
    public InIcklePredicate(IckleCriteriaBuilder criteriaBuilder, IckleExpression<? extends T> expression, Collection<T> values) {
        super(criteriaBuilder);
        this.expression = expression;
        this.values = new ArrayList<IckleExpression<? extends T>>(values.size());
        final Class<? extends T> javaType = expression.getJavaType();
        ValueHandlerFactory.ValueHandler<? extends T> valueHandler = javaType != null && ValueHandlerFactory.isNumeric(javaType) ? ValueHandlerFactory.determineAppropriateHandler((Class<? extends T>) javaType) : new ValueHandlerFactory.NoOpValueHandler<T>();
        for (T value : values) {
            if (value instanceof IckleExpression) {
                this.values.add((IckleExpression<T>) value);
            } else {
                this.values.add(new LiteralIckleExpression<T>(criteriaBuilder, valueHandler.convert(value)));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public IckleExpression<T> getExpression() {
        return (IckleExpression<T>) expression;
    }

    public IckleExpression<? extends T> getExpressionInternal() {
        return expression;
    }

    public List<IckleExpression<? extends T>> getValues() {
        return values;
    }

    public InIcklePredicate<T> value(T value) {
        return value(new LiteralIckleExpression<T>(criteriaBuilder(), value));
    }

    public InIcklePredicate<T> value(IckleExpression<? extends T> value) {
        values.add(value);
        return this;
    }

    public void registerParameters(IckleParameterRegistry registry) {
        IckleParameterContainer.Helper.possibleParameter(getExpressionInternal(), registry);
        for (IckleExpression value : getValues()) {
            IckleParameterContainer.Helper.possibleParameter(value, registry);
        }
    }

    @Override
    public String render(boolean isNegated, IckleRenderingContext renderingContext) {
        final StringBuilder buffer = new StringBuilder();
        final IckleExpression exp = getExpression();
        buffer.append(getExpression().render(renderingContext));
        if (isNegated) {
            buffer.append(" not");
        }
        buffer.append(" in ");

        // subquery expressions are already wrapped in parenthesis, so we only need to
        // render the parenthesis here if the values represent an explicit value list
        List<IckleExpression<? extends T>> values = getValues();

        if (values.isEmpty()) {
            if (renderingContext.getDialect().supportsEmptyInList()) {
                buffer.append("()");
            } else {
                buffer.append("(null)");
            }
        } else {
            buffer.append('(');
            String sep = "";
            for (IckleExpression value : values) {
                buffer.append(sep).append(value.render(renderingContext));
                sep = ", ";
            }
            buffer.append(')');
        }
        return buffer.toString();
    }
}
