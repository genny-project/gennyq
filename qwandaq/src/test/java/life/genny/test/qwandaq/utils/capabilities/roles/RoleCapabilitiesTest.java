package life.genny.test.qwandaq.utils.capabilities.roles;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import io.quarkus.test.junit.mockito.InjectMock;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.AttributeText;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.managers.capabilities.role.RoleBuilder;
import life.genny.qwandaq.managers.capabilities.role.RoleManager;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.test.qwandaq.utils.BaseTestCase;

import static life.genny.qwandaq.constants.GennyConstants.ROLE_LINK_CODE;
import static life.genny.qwandaq.datatype.CapabilityMode.*;

import java.util.Map;

import static life.genny.qwandaq.constants.GennyConstants.CHILDREN_LINK_CODE;;

@RunWith(MockitoJUnitRunner.class)
public class RoleCapabilitiesTest extends BaseTestCase {
	private static Logger log = Logger.getLogger(RoleCapabilitiesTest.class);


    @InjectMock
    UserToken userToken;

    @InjectMocks
    CapabilitiesManager capMan;

    @InjectMocks
    RoleManager roleMan;

    private static final Attribute LNK_ROLE = new AttributeText(ROLE_LINK_CODE, "Role Link");
    private static final Attribute LNK_CHILDREN = new AttributeText(CHILDREN_LINK_CODE, "Children Link");

    private static final String PRODUCT_CODE = "lojing";

    @Test
    public void testInject() {
        assert(capMan != null);
        assert(roleMan != null);
    }

    // @Test
    public void testRoleCreation() {
		
        QwandaUtils qwandaUtils = Mockito.mock(QwandaUtils.class);
		Mockito.when(qwandaUtils.getAttribute(PRODUCT_CODE, ROLE_LINK_CODE)).thenReturn(LNK_ROLE);
		Mockito.when(qwandaUtils.getAttribute(PRODUCT_CODE, CHILDREN_LINK_CODE)).thenReturn(LNK_CHILDREN);
        
        String[][] capData = {
            {"CAP_TEST_1", "Capability Test 1"},
            {"CAP_TEST_2", "Capability Test 2"},
            {"CAP_TEST_3", "Capability Test 3"}
        };

        for(String[] capInfo : capData) {
            Mockito.when(capMan.createCapability(PRODUCT_CODE, capInfo[0], capInfo[1])).thenReturn(new AttributeText(capInfo[0], capInfo[1]));
        }

        log.info("Generating Capabilities Map!");
        Map<String, Attribute> capabilitiesMap = capMan.getCapabilityMap(PRODUCT_CODE, capData);
        log.info("Done");

        log.info("Creating new test role");
        BaseEntity role = new RoleBuilder("ROL_TEST", "Test Role", PRODUCT_CODE)
            .setCapabilityMap(capabilitiesMap)
            .addCapability("CAP_TEST_1", ADD, EDIT, DELETE)
            .addChildren("ROL_TEST_2")
            .build();
        log.info("Done");
    }
    
}
