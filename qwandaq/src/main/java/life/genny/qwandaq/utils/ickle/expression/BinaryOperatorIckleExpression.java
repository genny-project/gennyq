package life.genny.qwandaq.utils.ickle.expression;

public interface BinaryOperatorIckleExpression<T> {
    /**
     * Get the right-hand operand.
     *
     * @return The right-hand operand.
     */
    public IckleExpression<?> getRightHandOperand();

    /**
     * Get the left-hand operand.
     *
     * @return The left-hand operand.
     */
    public IckleExpression<?> getLeftHandOperand();
}
