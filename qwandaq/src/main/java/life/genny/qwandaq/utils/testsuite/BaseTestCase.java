package life.genny.qwandaq.utils.testsuite;


public class BaseTestCase {

    public BaseTestCase() {
        log("Begin " + this.getClass().getSimpleName() + " tests");
    }

    public static void log(Object msg) {
        System.out.println(msg);
    }

    public static void error(Object msg) {
        System.err.println(msg);
    }
    

    public static <T> Expected<T> Expected(T value) {
        return new Expected<T>(value);
    }

    public static <T> Input<T> Input(T value) {
        return new Input<T>(value);
    }
}
