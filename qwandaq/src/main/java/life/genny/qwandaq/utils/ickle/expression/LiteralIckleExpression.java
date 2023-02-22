package life.genny.qwandaq.utils.ickle.expression;

import life.genny.qwandaq.utils.ickle.IckleCriteriaBuilder;
import life.genny.qwandaq.utils.ickle.IckleRenderingContext;
import org.hibernate.query.criteria.LiteralHandlingMode;
import org.hibernate.query.criteria.internal.ValueHandlerFactory;

import java.io.Serializable;

import static life.genny.qwandaq.utils.ickle.IckleClause.GROUP;
import static life.genny.qwandaq.utils.ickle.IckleClause.SELECT;

public class LiteralIckleExpression<T> extends IckleExpressionImpl<T> implements Serializable {
    private Object literal;

    private Class<T> javaType;

    @SuppressWarnings({"unchecked"})
    public LiteralIckleExpression(IckleCriteriaBuilder criteriaBuilder, T literal) {
        this(criteriaBuilder, (Class<T>) determineClass(literal), literal);
    }

    private static Class determineClass(Object literal) {
        return literal == null ? null : literal.getClass();
    }

    public LiteralIckleExpression(IckleCriteriaBuilder criteriaBuilder, Class<T> type, T literal) {
        super(criteriaBuilder, type);
        this.literal = literal;
        this.javaType = type;
    }

    @SuppressWarnings({"unchecked"})
    public T getLiteral() {
        return (T) literal;
    }

    public String render(IckleRenderingContext renderingContext) {
        // In the case of literals, we currently do not have an easy way to get the value.
        // That would require some significant infrastructure changes.
        // For now, we force the normalRender() code path for enums which means we will
        // always use parameter binding for enum literals.
        if (literal instanceof Enum) {
            return normalRender(renderingContext, LiteralHandlingMode.BIND);
        }

        switch (renderingContext.getClauseStack().peek()) {
            case SELECT: {
                return renderProjection(renderingContext);
            }
            case GROUP: {
                // technically a literal in the group-by clause
                // would be a reference to the position of a selection
                //
                // but this is what the code used to do...
                return renderProjection(renderingContext);
            }
            default: {
                return normalRender(renderingContext, renderingContext.getCriteriaLiteralHandlingMode());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String normalRender(IckleRenderingContext renderingContext, LiteralHandlingMode literalHandlingMode) {
        switch (literalHandlingMode) {
            case AUTO: {
                if (ValueHandlerFactory.isNumeric(literal)) {
                    return ValueHandlerFactory.determineAppropriateHandler((Class) literal.getClass()).render(literal);
                } else {
                    return bindLiteral(renderingContext);
                }
            }
            case BIND: {
                return bindLiteral(renderingContext);
            }
            case INLINE: {
                Object literalValue = literal;
                if (String.class.equals(literal.getClass())) {
                    literalValue = renderingContext.getDialect().inlineLiteral((String) literal);
                }

                ValueHandlerFactory.ValueHandler valueHandler = ValueHandlerFactory.determineAppropriateHandler((Class) literal.getClass());
                if (valueHandler == null) {
                    return bindLiteral(renderingContext);
                }

                return ValueHandlerFactory.determineAppropriateHandler((Class) literal.getClass()).render(literalValue);
            }
            default: {
                throw new IllegalArgumentException("Unexpected LiteralHandlingMode: " + literalHandlingMode);
            }
        }
    }

    private String renderProjection(IckleRenderingContext renderingContext) {
        if (ValueHandlerFactory.isCharacter(literal)) {
            // In case literal is a Character, pass literal.toString() as the argument.
            return renderingContext.getDialect().inlineLiteral(literal.toString());
        }

        // some drivers/servers do not like parameters in the select clause
        final ValueHandlerFactory.ValueHandler handler =
                ValueHandlerFactory.determineAppropriateHandler(literal.getClass());

        if (handler == null) {
            return normalRender(renderingContext, LiteralHandlingMode.BIND);
        } else {
            return handler.render(literal);
        }
    }

    private String bindLiteral(IckleRenderingContext renderingContext) {
        final String parameterName = renderingContext.registerLiteralParameterBinding(getLiteral(), javaType);
        return ':' + parameterName;
    }
}
