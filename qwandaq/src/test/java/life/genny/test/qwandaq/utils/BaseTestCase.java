package life.genny.test.qwandaq.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import life.genny.qwandaq.utils.testsuite.*;


@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class BaseTestCase {
    public static final String PRODUCT_CODE = "TEST_PRODUCT";

    public BaseTestCase() {
        log("Begin " + this.getClass().getSimpleName() + " tests");
    }

    private AutoCloseable closeable;
    
    @BeforeEach
    void initMocks() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void closeMocks() throws Exception {
        if(closeable != null) closeable.close();
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
