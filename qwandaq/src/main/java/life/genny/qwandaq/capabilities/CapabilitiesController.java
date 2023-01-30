package life.genny.qwandaq.capabilities;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.CapabilitySet;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.QwandaUtils;

@ApplicationScoped
public class CapabilitiesController {

    @Inject
    CapEngine engine;

    @Inject
    BaseEntityUtils beUtils;

	@Inject
	RoleManager roleMan;

    @Inject
    QwandaUtils qwandaUtils;

    // get user capabilities
    @Deprecated(forRemoval = false)
    public CapabilitySet getUserCapabilities() {
        return getEntityCapabilities(beUtils.getUserBaseEntity());
    }

    // get entity capabilities
    @Deprecated(forRemoval = false)
    public CapabilitySet getEntityCapabilities(BaseEntity baseEntity) {
        return engine.getEntityCapabilities(baseEntity);
    }

    public Attribute createCapability(final String productCode, final String rawCapabilityCode, final String name) {
        return createCapability(productCode, rawCapabilityCode, name, false);
    }

    // create capability
    public Attribute createCapability(final String productCode, final String rawCapabilityCode, final String name,
            boolean cleanedCode) {
        return engine.createCapability(productCode, rawCapabilityCode, name, cleanedCode);
    }

    // add capability

    public BaseEntity addCapability(String productCode, BaseEntity targetBe, Attribute capabilityAttribute,
            final CapabilityNode... modes) {
        if (capabilityAttribute == null) {
            throw new ItemNotFoundException(productCode, "Capability Attribute");
        }
        engine.addCapability(productCode, targetBe, capabilityAttribute, modes);
        return targetBe;
    }

    public BaseEntity addCapability(String productCode, BaseEntity targetBe, Attribute capabilityAttribute,
            final List<CapabilityNode> modes) {

        engine.addCapability(productCode, targetBe, capabilityAttribute, modes.toArray(new CapabilityNode[0]));
        return targetBe;
    }

    public BaseEntity addCapability(String productCode, BaseEntity targetBe, String rawCapabilityCode,
            final CapabilityNode... modes) {
        // Ensure the capability is well defined
        String cleanCapabilityCode = CapabilitiesController.cleanCapabilityCode(rawCapabilityCode);
        Attribute attribute = qwandaUtils.getAttribute(productCode, cleanCapabilityCode);
        return addCapability(productCode, targetBe, attribute, modes);
    }

    public BaseEntity addCapability(String productCode, BaseEntity targetBe, String rawCapabilityCode,
            final List<CapabilityNode> modes) {
        // Ensure the capability is well defined
        String cleanCapabilityCode = CapabilitiesController.cleanCapabilityCode(rawCapabilityCode);
        Attribute attribute = qwandaUtils.getAttribute(productCode, cleanCapabilityCode);
        return addCapability(productCode, targetBe, attribute, modes);
    }

    // remove capability
    // update capability

    // getters

	/**
	 * Generate a capability map from a 2D string of attributes
	 * 
	 * @param productCode - product code to create capability attributes from
	 * @param attribData  - 2D array of Strings (each entry in the first array is an
	 *                    array of 2 strings, one for name and one for code)
	 *                    - e.g [ ["CAP_ADMIN", "Manipulate Admin"], ["CAP_STAFF",
	 *                    "Manipulate Staff"]]
	 * @return a map going from attribute code (capability code) to attribute
	 *         (capability)
     * 
     * <pre>
     * CapabilitiesController controller; // get an instance, probably from CDI
     * 
     * String[][] capabilityData = {
     *  {"CAP_TEST_1", "Test capability"},
     *  {"CAP_TEST_2"},
     *  {"TEST_3", "Test Capability 3"}
     * };
     * 
     * // this will create these 3 capability attributes and persist them in the database
     * Map<String, Attribute> capabilityMap = controller.getCapabilityAttributeMap("gada", capabilityData);
     * 
     * new RoleBuilder("TEST_ROLE", "Test Role", "gada")
     * .setCapabilityMap(capabilityMap)
     * .addCapability("CAP_TEST_1").view(ALL).build()
     * 
     * .build()
     * 
     * // now we have a test role with code:TEST_ROLE and a single capability for CAP_TEST_1. This capability is available
     * // throughout the system as a standard Attribute, this implementation is just a bit of syntactic sugar and less database calls
     * // on startup
     * </pre>
	 */
	Map<String, Attribute> getCapabilityAttributeMap(String productCode, String[][] attribData) {
		Map<String, Attribute> capabilityMap = new HashMap<String, Attribute>();

		Arrays.asList(attribData).stream()
				// Map data to capability. If capability name/tag is missing then use the code
				// with standard capitalisation
				.map((String[] item) -> createCapability(productCode, item[0],
						((item.length == 2 && item[1] != null) ? item[1] : "Manipulate ".concat(CommonUtils.normalizeString(item[0]))), false))
				// add each capability attribute to the capability map, stripping the CAP_
				// prefix to be used with the constants
				.forEach((Attribute attr) -> capabilityMap.put(attr.getCode(), attr));

		return capabilityMap;
	}
    
    CapEngine getEngine() {
        return engine;
    }

	// For use in builder patterns
	RoleManager getRoleManager() {
		return roleMan;
	}

    // statics
    public static Capability deserializeCapability(String capabilityCode, String modeString) {
        List<CapabilityNode> caps = deserializeCapArray(modeString);
        return new Capability(capabilityCode, caps);
    }

    /**
     * Deserialise a stringified array of modes to a set of {@link CapabilityNode}
     * 
     * @param modeString
     * @return
     */
    @Deprecated
    public static Set<CapabilityNode> deserializeCapSet(String modeString) {
        return CommonUtils.getSetFromString(modeString, CapabilityNode::parseNode);
    }

    /**
     * Deserialise a stringified array of modes to an array of
     * {@link CapabilityNode}
     * 
     * @param modeString
     * @return
     */
    @Deprecated
    public static List<CapabilityNode> deserializeCapArray(String modeString) {
        return CommonUtils.getListFromString(modeString, CapabilityNode::parseNode);
    }

    /**
     * Clean a raw capability code.
     * Prepends the Capability Code Prefix if missing and forces uppercase
     * 
     * @param rawCapabilityCode
     * @return
     */
    public static String cleanCapabilityCode(final String rawCapabilityCode) {
        String cleanCapabilityCode = rawCapabilityCode.toUpperCase();
        if (!cleanCapabilityCode.startsWith(Prefix.CAP_)) {
            cleanCapabilityCode = Prefix.CAP_.concat(cleanCapabilityCode);
        }

        return cleanCapabilityCode;
    }

    /**
     * Serialize an array of {@link CapabilityNode}s to a string
     * 
     * @param modes
     * @return
     */
    public static String getModeString(CapabilityNode... capabilities) {
        return CommonUtils.getArrayString(capabilities, (capability) -> capability.toString());
    }

    public static String getModeString(Collection<CapabilityNode> capabilities) {
        return CommonUtils.getArrayString(capabilities, (capability) -> capability.toString());
    }

}
