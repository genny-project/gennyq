package life.genny.qwandaq.utils.callbacks.testing;

public interface FIAssertionCallback<Expected> {
    public void assertFunction(Expected result, Expected expected);
}
