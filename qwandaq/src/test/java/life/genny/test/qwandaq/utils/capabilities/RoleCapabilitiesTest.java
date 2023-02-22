package life.genny.test.qwandaq.utils.capabilities;

import static life.genny.qwandaq.datatype.capability.core.node.PermissionMode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.stream.Collectors;

import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CommonUtils;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.quarkus.test.junit.mockito.InjectMock;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.capabilities.CapabilitiesController;
import life.genny.qwandaq.capabilities.RoleBuilder;
import life.genny.qwandaq.capabilities.RoleManager;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.models.UserToken;

public class RoleCapabilitiesTest extends BasicCapabilitiesTest {
	private static Logger log = Logger.getLogger(RoleCapabilitiesTest.class);

    @InjectMock
    UserToken userToken;

    @Test
    public void testRoleCreation() {
        testInject();


        log.info("Got Attributes: " + LNK_ROLE.getCode());

        String[][] capData = {
            {"CAP_TEST_1", "Capability Test 1"},
            {"CAP_TEST_2", "Capability Test 2"},
            {"CAP_TEST_3", "Capability Test 3"}
        };

        log.info("Generating Capabilities Map!");
        mockCapabilityMap(capData);
        Map<String, Attribute> capabilitiesMap = capabilities.getCapabilityAttributeMap(PRODUCT_CODE, capData);
        log.info("Done. Got: " + capabilitiesMap.size() + " capabilities");

        log.info("Creating new test role");


        BaseEntityUtils beUtils = Mockito.mock(BaseEntityUtils.class);
        MockedStatic<CommonUtils> commonUtils = Mockito.mockStatic(CommonUtils.class);
        commonUtils.when(() -> CommonUtils.getArcInstance(CapabilitiesController.class)).thenReturn(capabilities);        
        commonUtils.when(() -> CommonUtils.getArcInstance(RoleManager.class)).thenReturn(roleMan);       
        commonUtils.when(() -> CommonUtils.getArcInstance(BaseEntityUtils.class)).thenReturn(beUtils);       
        
        BaseEntity beUtilsCreatedBaseEntity = new BaseEntity("ROL_TEST", "Test Role");
        Mockito.when(roleMan.createRole(PRODUCT_CODE, "ROL_TEST", "Test Role")).thenReturn(beUtilsCreatedBaseEntity);
        
        // Mockito.when(beUtils.updateBaseEntity(beUtilsCreatedBaseEntity, true)).thenReturn(beUtilsCreatedBaseEntity);
        
        BaseEntity role = new RoleBuilder("ROL_TEST", "Test Role", PRODUCT_CODE)
            .setCapabilityMap(capabilitiesMap)
            .addCapability("CAP_TEST_1").add(ALL).view(ALL).delete(ALL).build()
            .addChildren("ROL_TEST_2")
            .build();

        log.info("Finished with: " + role.getBaseEntityAttributes().size() + " size");
        CommonUtils.printCollection(role.getBaseEntityAttributes(), log::info, (ea) -> ea.getAttributeCode() + " = " + ea.getValueString());
        log.info("Done");
        
        // assertEquals(1,
        //         role.getBaseEntityAttributes().stream().filter((ea -> ea.getAttributeCode().startsWith(Prefix.CAP_))).collect(Collectors.toList()).size()
        // );
    }
    
}
