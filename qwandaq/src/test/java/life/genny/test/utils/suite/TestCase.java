package life.genny.test.utils.suite;

import life.genny.test.utils.callbacks.test.FITestCallback;

/**
 * 
 * @author Bryn Meachem
 */
public class TestCase<Input, Expected> {
    public final Input input;
    public final Expected expected;

    public final String name;

    public final FITestCallback<Input, Expected> testCallback;

    public TestCase(String name, Input input, Expected expected, FITestCallback<Input, Expected> testCallback) {
        this.input = input;
        this.expected = expected;
        this.testCallback = testCallback;

        this.name = name;
    }

    public Expected test() {
        return testCallback.test(input);
    }

    // TODO: Elaborate on this to string
    @Override
    public String toString() {
        return "Test [" + name + "]";
    }
}
