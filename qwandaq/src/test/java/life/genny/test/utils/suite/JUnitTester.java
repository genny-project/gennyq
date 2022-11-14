package life.genny.test.utils.suite;

import java.util.ArrayList;
import java.util.List;

import life.genny.test.utils.callbacks.test.FITestCallback;
import life.genny.test.utils.callbacks.test.FITestVerificationCallback;

public class JUnitTester<I, E> {
    public FITestCallback<Input<I>, Expected<E>> testCallback;
    public FITestVerificationCallback<E> verificationCallback;

    private final List<TestCase<I,E>> tests = new ArrayList<>();

    public JUnitTester<I, E> setTest(FITestCallback<Input<I>, Expected<E>> testCallback) {
        this.testCallback = testCallback;
        return this;
    }

    public JUnitTester<I, E> setVerification(FITestVerificationCallback<E> verificationCallback) {
        this.verificationCallback = verificationCallback;
        return this;
    }

    public TestBuilder<I, E> createTest(String name) {
        TestBuilder<I, E> jUnitBuilder = new TestBuilder<I,E>(this).setName(name);
        return jUnitBuilder;
    }

    public JUnitTester<I, E> addTest(TestCase<I, E> test) {
        tests.add(test);
        return this;
    }

    public JUnitTester<I, E> assertAll() {
        for(TestCase<I, E> test : tests) {
            test.verify();
        }
        tests.clear();
        return this;
    }

}
