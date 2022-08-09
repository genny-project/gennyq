package life.genny.test.utils.callbacks.test;

public interface FITestCallback<Input, Expected> {

    public Expected test(Input input);

}
