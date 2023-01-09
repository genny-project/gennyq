package life.genny.test.qwandaq.utils.capabilities.roles;

import java.util.Map;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import io.quarkus.test.junit.mockito.InjectMock;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.managers.capabilities.role.RoleManager;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.test.qwandaq.utils.BaseTestCase;

@RunWith(MockitoJUnitRunner.class)
public class RoleCapabilitiesTest extends BaseTestCase {

	private static Logger log = Logger.getLogger(RoleCapabilitiesTest.class);

	public final static DataType dtt = new DataType(String.class);

    @InjectMock
    UserToken userToken;

    @InjectMocks
    CapabilitiesManager capMan;

    @InjectMocks
    RoleManager roleMan;

    private static final Attribute LNK_ROLE = new Attribute(Attribute.LNK_ROLE, "Role Link", dtt);
    private static final Attribute LNK_CHILDREN = new Attribute(Attribute.LNK_CHILDREN, "Children Link", dtt);

    private static final String PRODUCT_CODE = "lojing";

    @Test
    public void testInject() {
        assert(capMan != null);
        assert(roleMan != null);
    }

    // @Test
    public void testRoleCreation() {
		
        CacheManager cm = Mockito.mock(CacheManager.class);
		Mockito.when(cm.getAttribute(PRODUCT_CODE, Attribute.LNK_ROLE)).thenReturn(LNK_ROLE);
		Mockito.when(cm.getAttribute(PRODUCT_CODE, Attribute.LNK_CHILDREN)).thenReturn(LNK_CHILDREN);
        
        String[][] capData = {
            {"CAP_TEST_1", "Capability Test 1"},
            {"CAP_TEST_2", "Capability Test 2"},
            {"CAP_TEST_3", "Capability Test 3"}
        };

        for(String[] capInfo : capData) {
            Mockito.when(capMan.createCapability(PRODUCT_CODE, capInfo[0], capInfo[1])).thenReturn(new Attribute(capInfo[0], capInfo[1], dtt));
        }

        log.info("Generating Capabilities Map!");
        Map<String, Attribute> capabilitiesMap = capMan.getCapabilityAttributeMap(PRODUCT_CODE, capData);
        log.info("Done");

        log.info("Creating new test role");
        // BaseEntity role = new RoleBuilder("ROL_TEST", "Test Role", PRODUCT_CODE)
        //     .setCapabilityMap(capabilitiesMap)
        //     .addCapability("CAP_TEST_1").add(ALL).view(ALL).delete(ALL)
        //     .addChildren("ROL_TEST_2")
        //     .build();
        log.info("Done");
    }
    
}
