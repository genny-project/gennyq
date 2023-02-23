package life.genny.qwandaq.utils.ickle;

import life.genny.qwandaq.utils.ickle.expression.function.IckleFunctionExpression;
import life.genny.qwandaq.utils.ickle.parameter.IckleParameterExpression;
import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.criteria.LiteralHandlingMode;
import org.hibernate.query.criteria.internal.compile.ExplicitParameterInfo;
import org.hibernate.query.criteria.internal.expression.ParameterExpressionImpl;
import org.hibernate.type.Type;

import java.util.Stack;

public class IckleRenderingContextImpl implements IckleRenderingContext {
    private int aliasCount;
    private int explicitParameterCount;

    private final Stack<IckleClause> clauseStack = new Stack<>();
    private final Stack<IckleFunctionExpression> functionContextStack = new Stack<>();

    public String generateAlias() {
        return "generatedAlias" + aliasCount++;
    }

    public String generateParameterName() {
        return "param" + explicitParameterCount++;
    }

    @Override
    public Stack<IckleClause> getClauseStack() {
        return clauseStack;
    }

    @Override
    public Stack<IckleFunctionExpression> getFunctionStack() {
        return functionContextStack;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ExplicitParameterInfo registerExplicitParameter(IckleParameterExpression<?> criteriaQueryParameter) {
        /*ExplicitParameterInfo parameterInfo = explicitParameterInfoMap.get(criteriaQueryParameter);
        if (parameterInfo == null) {
            if (StringHelper.isNotEmpty(criteriaQueryParameter.getName()) && !((IckleParameterExpression) criteriaQueryParameter).isNameGenerated()) {
                parameterInfo = new ExplicitParameterInfo(
                        criteriaQueryParameter.getName(),
                        null,
                        criteriaQueryParameter.getJavaType()
                );
            } else if (criteriaQueryParameter.getPosition() != null) {
                parameterInfo = new ExplicitParameterInfo(
                        null,
                        criteriaQueryParameter.getPosition(),
                        criteriaQueryParameter.getJavaType()
                );
            } else {
                parameterInfo = new ExplicitParameterInfo(
                        generateParameterName(),
                        null,
                        criteriaQueryParameter.getJavaType()
                );
            }

            explicitParameterInfoMap.put(criteriaQueryParameter, parameterInfo);
        }

        return parameterInfo;*/
        throw new NotImplementedException();
    }

    public String registerLiteralParameterBinding(final Object literal, final Class javaType) {
        /*final String parameterName = generateParameterName();
        final ImplicitParameterBinding binding = new ImplicitParameterBinding() {
            public String getParameterName() {
                return parameterName;
            }

            public Class getJavaType() {
                return javaType;
            }

            public void bind(TypedQuery typedQuery) {
                if (literal instanceof Parameter) {
                    return;
                }
                typedQuery.setParameter(parameterName, literal);
            }
        };

        implicitParameterBindings.add(binding);
        return parameterName;*/
        throw new NotImplementedException();
    }

    public String getCastType(Class javaType) {
        /*SessionFactoryImplementor factory = entityManager.getFactory();
        Type hibernateType = factory.getTypeResolver().heuristicType(javaType.getName());
        if (hibernateType == null) {
            throw new IllegalArgumentException(
                    "Could not convert java type [" + javaType.getName() + "] to Hibernate type"
            );
        }
        return hibernateType.getName();*/
        throw new NotImplementedException();
    }

    @Override
    public Dialect getDialect() {
        throw new NotImplementedException();
    }

    @Override
    public LiteralHandlingMode getCriteriaLiteralHandlingMode() {
        return LiteralHandlingMode.AUTO;
    }
}
