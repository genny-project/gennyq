package life.genny.qwandaq.utils.callbacks.testing;

public interface FITestCallback<Input, Expected> {

    public Expected test(Input input);

}
