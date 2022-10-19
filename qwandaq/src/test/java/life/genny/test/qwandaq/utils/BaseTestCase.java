package life.genny.test.qwandaq.utils;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;

public class BaseTestCase {

	protected static final Logger log = Logger.getLogger(BaseTestCase.class);
    
    @BeforeEach
    void initMocks() {
        MockitoAnnotations.openMocks(this);
    }

}
