package life.genny.test.utils.suite;

import life.genny.test.exception.TestException;
import life.genny.test.utils.JUnitTester;
import life.genny.test.utils.callbacks.test.FITestCallback;

/**
 * 
 * @author Bryn Meachem
 */
public class TestBuilder<Input, Expected> {

    private JUnitTester<Input, Expected> tester;

    public Input input;
    public Expected expected;

    public String name = "";

    public FITestCallback<Input, Expected> testCallback;

    public TestBuilder() {
    }

    public JUnitTester<Input, Expected> getTester() {
        return tester != null ? tester : getTester(null);
    }
    
    public JUnitTester<Input, Expected> getTester(Class<?> clazz) {
        tester = new JUnitTester<Input, Expected>(clazz);
        return tester;
    }

    public TestBuilder<Input, Expected> setName(String name) {
        this.name = name;
        return this;
    }

    public TestBuilder<Input, Expected> setInput(Input input) {
        this.input = input;
        return this;
    }

    public TestBuilder<Input, Expected> setExpected(Expected expected) {
        this.expected = expected;
        return this;
    }

    public TestBuilder<Input, Expected> setTestFunction(FITestCallback<Input, Expected> callback) {
        this.testCallback = callback;
        return this;
    }

    public TestCase<Input, Expected> build() {
        if(testCallback == null) {
            throw new TestException("Invalid Test: No Test Function has been defined!", name);
        }

        return new TestCase<Input, Expected>(name, input, expected, testCallback);
    }
}