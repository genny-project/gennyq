package life.genny.qwandaq.utils.ickle.parameter;

import life.genny.qwandaq.utils.ickle.expression.IckleExpression;

/**
 * Contract for query components capable of either being a parameter or containing parameters.
 *
 * @author Steve Ebersole
 */
public interface IckleParameterContainer {
    /**
     * Register any parameters contained within this query component with the given registry.
     *
     * @param registry The parameter registry with which to register.
     */
    public void registerParameters(IckleParameterRegistry registry);

    /**
     * Helper to deal with potential parameter container nodes.
     */
    public static class Helper {
        public static void possibleParameter(IckleExpression expression, IckleParameterRegistry registry) {
            if (IckleParameterContainer.class.isInstance(expression)) {
                ((IckleParameterContainer) expression).registerParameters(registry);
            }
        }
    }
}