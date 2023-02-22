package life.genny.test.qwandaq.utils.capabilities;

import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.capabilities.CapabilitiesController;
import life.genny.qwandaq.capabilities.RoleManager;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.utils.AttributeUtils;
import life.genny.test.qwandaq.utils.BaseTestCase;


public class BasicCapabilitiesTest extends BaseTestCase {
	private static final DataType dtt = new DataType(String.class);

    protected static final Attribute LNK_ROLE = new Attribute(Attribute.LNK_ROLE, "Role Link", dtt);
    protected static final Attribute LNK_CHILDREN = new Attribute(Attribute.LNK_CHILDREN, "Children Link", dtt);
    
    @InjectMocks
    CapabilitiesController capabilities;

    @InjectMocks
    RoleManager roleMan;

    @InjectMocks
    AttributeUtils attributeUtils;
    
    @Test
    public void testInject() {
        // Might use some cheeky reflection to make this maintainable
        assertNotNull(capabilities);
        assertNotNull(roleMan);
        assertNotNull(attributeUtils);
    }

    @BeforeEach
    void initMocks() {
        attributeUtils = Mockito.mock(AttributeUtils.class);
        capabilities = Mockito.mock(CapabilitiesController.class);
        roleMan = Mockito.mock(RoleManager.class);

		Mockito.when(attributeUtils.getAttribute(PRODUCT_CODE, Attribute.LNK_ROLE)).thenReturn(LNK_ROLE);
		Mockito.when(attributeUtils.getAttribute(PRODUCT_CODE, Attribute.LNK_CHILDREN)).thenReturn(LNK_CHILDREN);
    }
    
    public void mockCapabilityMap(String[][] capData) {
        Map<String, Attribute> attribMap = new HashMap<>();
        for(String[] capInfo : capData) {
            Attribute attrib = new Attribute(capInfo[0], capInfo[1], dtt);
            // Mockito.when(attributeUtils.getAttribute(PRODUCT_CODE, capInfo[0])).thenReturn(attrib);
            // Mockito.when(qwandaUtils.getAttribute(PRODUCT_CODE, capInfo[0])).thenReturn(attrib);
            Mockito.doReturn(attrib).when(capabilities).createCapability(PRODUCT_CODE, capInfo[0], capInfo[1]);
            attribMap.put(capInfo[0], attrib);
            // Mockito.when(capabilities.createCapability(PRODUCT_CODE, capInfo[0], capInfo[1])).thenReturn(attrib);
        }


        // Mockito.when(capabilities.createCapability(PRODUCT_CODE, capData))
        Mockito.when(capabilities.getCapabilityAttributeMap(PRODUCT_CODE, capData)).thenReturn(attribMap);
    }
    
}
