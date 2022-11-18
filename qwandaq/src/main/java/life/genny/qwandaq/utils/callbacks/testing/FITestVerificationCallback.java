package life.genny.qwandaq.utils.callbacks.testing;

public interface FITestVerificationCallback<Expected> {
    public void assertFunction(Expected result, Expected expected);
}
