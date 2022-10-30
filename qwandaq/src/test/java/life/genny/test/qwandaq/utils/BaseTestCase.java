package life.genny.test.qwandaq.utils;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;

public class BaseTestCase {
    
    @BeforeEach
    void initMocks() {
        MockitoAnnotations.openMocks(this);
    }

}
