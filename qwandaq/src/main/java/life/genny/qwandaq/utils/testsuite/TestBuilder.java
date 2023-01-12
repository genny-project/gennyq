package life.genny.qwandaq.utils.testsuite;


import life.genny.qwandaq.utils.callbacks.testing.FITestCallback;
import life.genny.qwandaq.utils.callbacks.testing.FIAssertionCallback;

public class TestBuilder<I, E> {

    private Input<I> input;
    private Expected<E> expected;

    private JUnitTester<I, E> tester;

    private String name;
    private FITestCallback<Input<I>, Expected<E>> testCallback;
    private FIAssertionCallback<E> verificationCallback;
    
    public TestBuilder(JUnitTester<I, E> tester) {
        this.tester = tester;
        if(tester != null) {
            testCallback = tester.testCallback;
            verificationCallback = tester.verificationCallback;
        }
    }

    public TestBuilder<I, E> setTest(FITestCallback<Input<I>, Expected<E>> testCallback) {
        this.testCallback = testCallback;
        return this;
    }

    public TestBuilder<I, E> setVerification(FIAssertionCallback<E> verificationCallback) {
        this.verificationCallback = verificationCallback;
        return this;
    }

    public TestBuilder() {
        this(null);
    }

    public TestBuilder<I, E> setInput(I input) {
        this.input = new Input<I>(input);
        return this;
    }
    
    public TestBuilder<I, E> setExpected(E expected) {
        this.expected = new Expected<E>(expected);
        
        return this;
    }

    public TestBuilder<I, E> setName(String name) {
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

    public TestCase<I, E> buildTest() 
        throws IllegalArgumentException {
        if(testCallback == null) {
            throw new IllegalArgumentException("No test function set for test: " + name);
        }

        if(input == null) {
            throw new IllegalArgumentException("No input set for test: " + name);
        }

        if(expected == null) {
            throw new IllegalArgumentException("No Expected result set for test: " + name);
        }
        FIAssertionCallback<E> verifCallback = verificationCallback != null ? verificationCallback : tester.verificationCallback;
        return new TestCase<I, E>(name, input, expected, testCallback, verifCallback);
    }
}