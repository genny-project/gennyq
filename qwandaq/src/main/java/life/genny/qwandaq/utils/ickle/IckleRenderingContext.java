package life.genny.qwandaq.utils.ickle;

import life.genny.qwandaq.utils.ickle.expression.function.IckleFunctionExpression;
import life.genny.qwandaq.utils.ickle.parameter.IckleParameterExpression;
import org.hibernate.dialect.Dialect;
import org.hibernate.query.criteria.LiteralHandlingMode;
import org.hibernate.query.criteria.internal.compile.ExplicitParameterInfo;

import java.util.Stack;

/**
 * Used to provide a context and services to the rendering.
 *
 * @author Steve Ebersole
 */
public interface IckleRenderingContext {
    /**
     * Generate a correlation name.
     *
     * @return The generated correlation name
     */
    String generateAlias();

    /**
     * Register parameters explicitly encountered in the criteria query.
     *
     * @param criteriaQueryParameter The parameter expression
     *
     * @return The JPA-QL parameter name
     */
    ExplicitParameterInfo registerExplicitParameter(IckleParameterExpression<?> criteriaQueryParameter);

    /**
     * Register a parameter that was not part of the criteria query (at least not as a parameter).
     *
     * @param literal The literal value
     * @param javaType The java type as which to handle the literal value.
     *
     * @return The JPA-QL parameter name
     */
    String registerLiteralParameterBinding(Object literal, Class javaType);

    /**
     * Given a java type, determine the proper cast type name.
     *
     * @param javaType The java type.
     *
     * @return The cast type name.
     */
    String getCastType(Class javaType);

    /**
     * Current Dialect.
     *
     * @return Dialect
     */
    Dialect getDialect();

    Stack<IckleClause> getClauseStack();

    Stack<IckleFunctionExpression> getFunctionStack();

    /**
     * How literals are going to be handled.
     *
     * @return literal handling strategy
     */
    default LiteralHandlingMode getCriteriaLiteralHandlingMode() {
        return LiteralHandlingMode.AUTO;
    }
}