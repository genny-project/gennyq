package life.genny.qwandaq.utils.ickle;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ValueHandlerFactory;

import java.util.Collections;
import java.util.List;

public class IckleSelectionImpl<X> implements IckleSelection<X> {

    private String alias;
    private ValueHandlerFactory.ValueHandler<X> valueHandler;

    private final Class originalJavaType;

    private Class<X> javaType;

    public IckleSelectionImpl(IckleCriteriaBuilder criteriaBuilder, Class<X> javaType) {
        this.originalJavaType = javaType;
        this.javaType = javaType;
    }

    public IckleSelection<X> alias(String alias) {
        setAlias(alias);
        return this;
    }

    public boolean isCompoundSelection() {
        return false;
    }

    @Override
    public Class<X> getJavaType() {
        return javaType;
    }

    @SuppressWarnings({ "unchecked" })
    protected void resetJavaType(Class targetType) {
        this.javaType = targetType;
//		this.valueHandler = javaType.equals( originalJavaType )
//				? null
//				: ValueHandlerFactory.determineAppropriateHandler( javaType );
        this.valueHandler = ValueHandlerFactory.determineAppropriateHandler( javaType );
    }

    public List<ValueHandlerFactory.ValueHandler> getValueHandlers() {
        return getValueHandler() == null
                ? null
                : Collections.singletonList((ValueHandlerFactory.ValueHandler) getValueHandler());
    }

    public List<IckleSelection<?>> getCompoundSelectionItems() {
        throw new IllegalStateException("Not a compound selection");
    }

    public ValueHandlerFactory.ValueHandler<X> getValueHandler() {
        return valueHandler;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    /**
     * Protected access to define the alias.
     *
     * @param alias The alias to use.
     */
    protected void setAlias(String alias) {
        this.alias = alias;
    }
}
