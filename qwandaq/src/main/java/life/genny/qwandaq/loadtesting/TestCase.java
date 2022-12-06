package life.genny.qwandaq.loadtesting;

public class TestCase<Input, Expected> {
    public final Input input;
    public final Expected expected;

    public TestCase(Input input, Expected expected) {
        this.input = input;
        this.expected = expected;
    }
}
