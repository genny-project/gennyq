package life.genny.qwandaq.utils.testsuite;

import java.util.ArrayList;
import java.util.List;

import life.genny.qwandaq.utils.callbacks.testing.FITestCallback;
import life.genny.qwandaq.utils.callbacks.testing.FIAssertionCallback;

public class JUnitTester<I, E> {
    public FITestCallback<Input<I>, Expected<E>> testCallback;
    public FIAssertionCallback<E> verificationCallback;

    private final List<TestCase<I,E>> tests = new ArrayList<>();

    public JUnitTester() {
        this(JUnitTester::defaultAssertion);
    }

    public JUnitTester(FIAssertionCallback<E> verificationCallback) {
        setAssertion(verificationCallback != null ? verificationCallback : JUnitTester::defaultAssertion);
    }

    public JUnitTester<I, E> setTest(FITestCallback<Input<I>, Expected<E>> testCallback) {
        this.testCallback = testCallback;
        return this;
    }

    public JUnitTester<I, E> setAssertion(FIAssertionCallback<E> verificationCallback) {
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
    
    public static <E> void defaultAssertion(E result, E expected) {
        if(!objectsAreEqual(result, expected))
            throw new AssertionError("Got: " + result + ". Expected: " + expected);
        
    }


    // pulled from junit api
    private static boolean objectsAreEqual(Object obj1, Object obj2) {
		if (obj1 == null) {
			return (obj2 == null);
		}
		return obj1.equals(obj2);
	}
}
