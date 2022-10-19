package life.genny.test.utils.suite;

import life.genny.test.utils.callbacks.test.FITestCallback;
import life.genny.test.utils.callbacks.test.FITestVerificationCallback;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 
 * @author Bryn Meachem
 */
public class TestCase<I, E> {
    private final Input<I> input;
    private final Expected<E> expected;

    private final String name;

    private final FITestCallback<Input<I>, Expected<E>> testCallback;
    private final FITestVerificationCallback<E> verificationCallback;

    public TestCase(String name, Input<I> input, Expected<E> expected, 
            FITestCallback<Input<I>, Expected<E>> testCallback, FITestVerificationCallback<E> verificationCallback) {
        this.input = input;
        this.expected = expected;
        this.testCallback = testCallback;
        this.verificationCallback = verificationCallback;

        this.name = name;
    }

    public E test() {
        return testCallback.test(input).expected;
    }

    public void verify() {
        verificationCallback.assertFunction(test(), expected.expected);
    }

    public E getExpected() {
        return expected.expected;
    }

    // TODO: Elaborate on this to string
    @Override
    public String toString() {
        return "Test [" + (name != null ? name : "Unnamed Test") + "]";
    }

    private static <E> void defaultAssertion(E result, E expected) {
        assertEquals(expected, result);
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

        @Override
        public boolean equals(Object other) {
            if((other instanceof Expected<?>)) {
                return ((Expected<?>) other).expected.equals(this.expected);
            }

            return false;
        }
    }

    public static class Builder<I, E> {

        private Input<I> input;
        private Expected<E> expected;

        private JUnitTester<I, E> tester;

        private String name;
        private FITestCallback<Input<I>, Expected<E>> testCallback;
        private FITestVerificationCallback<E> verificationCallback;
        
        public Builder(JUnitTester<I, E> tester) {
            this.tester = tester;
            testCallback = tester.testCallback;
            verificationCallback = tester.verificationCallback;
        }

        public Builder<I, E> setTest(FITestCallback<Input<I>, Expected<E>> testCallback) {
            this.testCallback = testCallback;
            return this;
        }
    
        public Builder<I, E> setVerification(FITestVerificationCallback<E> verificationCallback) {
            this.verificationCallback = verificationCallback;
            return this;
        }

        public Builder() {
            this(null);
        }

        public Builder<I, E> setInput(I input) {
            this.input = new Input<I>(input);
            return this;
        }
        
        public Builder<I, E> setExpected(E expected) {
            this.expected = new Expected<E>(expected);
            
            return this;
        }

        public Builder<I, E> setName(String name) {
            this.name = name;
            return this;
        }

        public JUnitTester<I,E> build() {
            if(tester == null) {
                throw new UnsupportedOperationException("Cannot call .build() on a Builder that was instantiated with new Builder(). Use .buildTest() instead");
            }

            tester.addTest(buildTest());
            return tester;
        }

        public TestCase<I, E> buildTest() {
            return new TestCase<I, E>(name, input, expected, testCallback, verificationCallback != null ? verificationCallback : TestCase::defaultAssertion);
        }
    }
}
