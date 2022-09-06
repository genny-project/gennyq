package life.genny.test.utils.suite;

import life.genny.test.utils.callbacks.test.FITestCallback;

/**
 * 
 * @author Bryn Meachem
 */
public class TestCase<I, E> {
    private final Input<I> input;
    private final Expected<E> expected;

    public final String name;

    public final FITestCallback<Input<I>, Expected<E>> testCallback;

    public TestCase(String name, Input<I> input, Expected<E> expected, FITestCallback<Input<I>, Expected<E>> testCallback) {
        this.input = input;
        this.expected = expected;
        this.testCallback = testCallback;

        this.name = name;
    }

    public E test() {
        return testCallback.test(input).expected;
    }

    public E getExpected() {
        return expected.expected;
    }

    // TODO: Elaborate on this to string
    @Override
    public String toString() {
        return "Test [" + (name != null ? name : "Unnamed Test") + "]";
    }



    // Helper classes
    public static class Input<T> {
        public T input;

        public Input(T input) {
            this.input = input;
        }
    }

    public static class Expected<T> {
        public T expected;

        public Expected(T expected) {
            this.expected = expected;
        }
    }

    public static class Builder<I, E> {
        private Input<I> input;
        private Expected<E> expected;

        private String name;
        private FITestCallback<Input<I>, Expected<E>> testCallback;
        
        public Builder<I, E> setInput(I input) {
            this.input = new Input<I>(input);
            return this;
        }
        
        public Builder<I, E> setExpected(E expected) {
            this.expected = new Expected<E>(expected);
            
            return this;
        }

        public Builder<I, E> setTest(FITestCallback<Input<I>, Expected<E>> testCallback) {
            this.testCallback = testCallback;
            return this;
        }

        public Builder<I, E> setName(String name) {
            this.name = name;
            return this;
        }

        public TestCase<I, E> build() {
            return new TestCase<I, E>(name, input, expected, testCallback);
        }
    }
}
