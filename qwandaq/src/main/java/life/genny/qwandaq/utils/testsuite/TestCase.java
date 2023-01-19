package life.genny.qwandaq.utils.testsuite;

import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.callbacks.testing.FITestCallback;
import life.genny.qwandaq.utils.callbacks.testing.FITestVerificationCallback;

/**
 * 
 * @author Bryn Meachem
 */
public class TestCase<I, E> {
    public static final int EQUALS_LENGTH = 50;
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
        System.out.println(CommonUtils.equalsBreak(EQUALS_LENGTH) + "\n[!] Running test: " + this);
        E result = testCallback.test(input).expected;
        printData("Input", input.input);
        printData("Expected", expected.expected);
        printData("Result", result);
        return result;
    }
    
    private void printData(String tag, Object object) {
        if(object == null) {
            System.out.println("       "+ tag +": null");
        }
        else if(!object.getClass().isArray()) {
            System.out.println("       "+ tag +": " + object);
        } else {
            Object[] objects = (Object[])object;
            System.out.println("       "+ tag +": " + CommonUtils.getArrayString(objects));
        }
    }

    public void verify() {
        E result = test();
        try { 
            verificationCallback.assertFunction(result, expected.expected);
        } catch(AssertionError e) {
            assert(false);
        }
    }

    public E getExpected() {
        return expected.expected;
    }

    public String getName() {
        return name;
    }

    // TODO: Elaborate on this to string
    @Override
    public String toString() {
        return "Test [" + (name != null ? name : "Unnamed Test") + "]";
    }
}
