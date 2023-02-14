package life.genny.qwandaq.utils.ickle.parameter;

/**
 * A registry for parameters.  In criteria queries, parameters must be actively seeked out as expressions and predicates
 * are added to the {@link org.hibernate.criterion.CriteriaQuery}; this contract allows the various subcomponents to
 * register any parameters they contain.
 *
 * @author Steve Ebersole
 */
public interface IckleParameterRegistry {
    /**
     * Registers the given parameter with this regitry.
     *
     * @param parameter The parameter to register.
     */
    public void registerParameter(IckleParameterExpression<?> parameter);
}