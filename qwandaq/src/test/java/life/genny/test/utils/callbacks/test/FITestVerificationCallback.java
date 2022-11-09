package life.genny.test.utils.callbacks.test;

public interface FITestVerificationCallback<Expected> {
    public void assertFunction(Expected result, Expected expected);
}
