package life.genny.qwandaq.capabilities;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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

import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.exception.runtime.entity.BaseEntityException;

import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.QwandaUtils;

/**
 * <p>This is a CDI enabled Controller for the Capabilities System within Genny.</p>
 * <p>The core methods of the controller are:
 * <ul>
 *  <li>{@link CapabilitiesController#getUserCapabilities getUserCapabilities} - used for getting all of the (Session) user capabilities</li>
 *  <li>{@link CapabilitiesController#getEntityCapabilities getEntityCapabilities} - used for getting either a PER or ROL 
 *                      {@link BaseEntity BaseEntity's} Capabilities</li>
 *  <li>{@link CapabilitiesController#createCapability createCapability} - used to create a Capability {@link Attribute}</li>
 *  <li>{@link CapabilitiesController#addCapability addCapability} - used to add a Capability {@link Attribute} to a Role or User BaseEntity</li>
 *  <li>{@link CapabilitiesController#getCapabilityAttributeMap getCapabilityAttributeMap} - used to make the 
 *                      Role building process more clean/organised and a bit easier on persistence</li>
 *  <li>{@link CapabilitiesController#doPersist doPersist} - a method to change whether or not addCapability persists on each call (helpful for Role Building)</li>
 * </ul>
 * </p>
 * 
 * @author Bryn
 */
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

    // Some configuration for the controller. Useful for keeping # of overloads small
    private boolean persistOnEachUpdate = true;

    /**
     * @deprecated (Marked as deprecated to show use cases in code. This is going to be moved to somewhere where it can be cached with User Session)
     * <p>Get a {@link CapabilitySet} of user capabilities. This will retrieve all Capabilities from a user 
     * {@link BaseEntity BaseEntity's} roles and then all capabilities saved directly against the user and put them in
     * one object containing a {@link HashSet HashSet}<{@link Capability}> and a direct link to the user {@link BaseEntity}
     * </p>
     * 
     * <p>
     * <b>IMPORTANT NOTE</b>: Try and use this call as little as possible. It may be moved to Bridge in the near future
     * </p>
     * @return a new {@link CapabilitySet} containing the user's capabilities and their user BaseEntity
     */
    @Deprecated(forRemoval = false)
    public CapabilitySet getUserCapabilities() {
        return getEntityCapabilities(beUtils.getUserBaseEntity());
    }

    /**
     * @deprecated (Marked as deprecated to show use cases in code. This is going to be moved to somewhere where it can be cached with User Session)
     * <p>Get a {@link CapabilitySet} of a {@link BaseEntity BaseEntity's} capabilities. This BaseEntity should
     *  be a PER BaseEntity or an ROL BaseEntity. This will retrieve all Capabilities from a user 
     * {@link BaseEntity BaseEntity's} roles and then all capabilities saved directly against the user and put them in
     * one object containing a {@link HashSet HashSet}<{@link Capability}> and a direct link to the user {@link BaseEntity}
     * </p>
     * 
     * <p>
     * <b>IMPORTANT NOTE</b>: Try and use this call as little as possible. It may be moved to Bridge in the near future
     * </p>
     * @return a new {@link CapabilitySet} containing the user's capabilities and their user BaseEntity
     */
    @Deprecated(forRemoval = false)
    public CapabilitySet getEntityCapabilities(BaseEntity baseEntity) {
        return engine.getEntityCapabilities(baseEntity);
    }

    /**
     * Create a capability attribute, and save it to the database of a given product
     * @param productCode - product to save it to
     * @param capabilityCode - the capability code to save
     * @param name - the name (brief description of the capability)
     * @return - the new saved Capability Attribute
     */
    public Attribute createCapability(final String productCode, String capabilityCode, final String name) {
        capabilityCode = cleanCapabilityCode(capabilityCode);
        return engine.createCapability(productCode, capabilityCode, name);
    }

    /**
     * Add a Capability attribute to a Person or Role {@link BaseEntity}
     * @param productCode - product that the {@link BaseEntity targetBe} is in
     * @param targetBe - the target {@link BaseEntity} (with a prefix in the 
     *                      {@link CapEngine#CAPABILITY_BEARING_ENTITY_PREFIXES Accepted Capability Bearing Prefixes})
     * @param capabilityAttribute - an {@link Attribute} with a prefix starting with {@link Prefix#CAP_ CAP_}
     * @param nodes - zero or more {@link CapabilityNode CapabilityNodes} to tie to this {@link BaseEntity targetBe} in
     *                      an {@link EntityAttribute}
     * 
     * @throws {@link NullParameterException} if the {@link BaseEntity targetBe} or {@link Attribute capabilityAttribute} is missing
     * @throws {@link BaseEntityException} if the {@link BaseEntity#getCode() targetBe's code} is not in the 
    *                                       {@link CapEngine#CAPABILITY_BEARING_ENTITY_PREFIXES Accepted Capability Bearing Prefixes}
     * @return the updated {@link BaseEntity targetBe}
     */
    public BaseEntity addCapability(String productCode, BaseEntity targetBe, Attribute capabilityAttribute,
            final CapabilityNode... nodes) {

        // exceptions thrown in here, don't need to handle/null check
        engine.addCapability(productCode, targetBe, capabilityAttribute, persistOnEachUpdate, nodes);
        return targetBe;
    }

    /**
     * Add a Capability attribute to a Person or Role {@link BaseEntity}
     * @param productCode - product that the {@link BaseEntity targetBe} is in
     * @param targetBe - the target {@link BaseEntity} (with a prefix in the 
     *                      {@link CapEngine#CAPABILITY_BEARING_ENTITY_PREFIXES Accepted Capability Bearing Prefixes})
     * @param capabilityAttribute - an {@link Attribute} with a prefix starting with {@link Prefix#CAP_ CAP_}
     * @param nodes - a {@link Collection} of zero or more {@link CapabilityNode CapabilityNodes} to tie to this {@link BaseEntity targetBe} in
     *                      an {@link EntityAttribute}
     * @return
     */
    public BaseEntity addCapability(String productCode, BaseEntity targetBe, Attribute capabilityAttribute,
            final Collection<CapabilityNode> nodes) {

        engine.addCapability(productCode, targetBe, capabilityAttribute, persistOnEachUpdate, nodes.toArray(new CapabilityNode[0]));
        return targetBe;
    }

    public BaseEntity addCapability(String productCode, BaseEntity targetBe, String capabilityCode,
            final CapabilityNode... modes) {
        // Ensure the capability is well defined
        capabilityCode = CapabilitiesController.cleanCapabilityCode(capabilityCode);
        Attribute attribute = qwandaUtils.getAttribute(productCode, capabilityCode);
        return addCapability(productCode, targetBe, attribute, modes);
    }

    public BaseEntity addCapability(String productCode, BaseEntity targetBe, String capabilityCode,
            final Collection<CapabilityNode> modes) {
        // Ensure the capability is well defined
        String cleanCapabilityCode = CapabilitiesController.cleanCapabilityCode(capabilityCode);
        Attribute attribute = qwandaUtils.getAttribute(productCode, cleanCapabilityCode);
        return addCapability(productCode, targetBe, attribute, modes);
    }

    public BaseEntity deleteCapability(String productCode, BaseEntity targetBe, String capabilityCode) {
        capabilityCode = cleanCapabilityCode(capabilityCode);
        return engine.deleteCapability(productCode, targetBe, capabilityCode);
    }

    public BaseEntity deleteCapability(String productCode, BaseEntity targetBe, Attribute capabilityAttribute) {
        return deleteCapability(productCode, targetBe, capabilityAttribute);
    }

    public BaseEntity resetCapability(String productCode, BaseEntity targetBe, String capabilityCode) {
        capabilityCode = cleanCapabilityCode(capabilityCode);
        return engine.resetCapability(productCode, targetBe, capabilityCode);
    }
    
    public BaseEntity resetCapability(String productCode, BaseEntity targetBe, Attribute capabilityAttribute) {
        return engine.resetCapability(productCode, targetBe, capabilityAttribute);
    }

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
	public Map<String, Attribute> getCapabilityAttributeMap(String productCode, String[][] attribData) {
		Map<String, Attribute> capabilityMap = new HashMap<String, Attribute>();

		Arrays.asList(attribData).stream()
				// Map data to capability. If capability name/tag is missing then use the code
				// with standard capitalisation
				.map((String[] item) -> createCapability(productCode, item[0],
						((item.length == 2 && item[1] != null) ? item[1] : "Manipulate ".concat(CommonUtils.normalizeString(item[0])))))
				// add each capability attribute to the capability map, stripping the CAP_
				// prefix to be used with the constants
				.forEach((Attribute attr) -> capabilityMap.put(attr.getCode(), attr));

		return capabilityMap;
	}
    
    // Getters and Setters
    /**
     * Set whether or not calls to {@link CapabilitiesController#addCapability addCapability} should persist each time it is called
     * Useful for Role Building. This is turned off when using {@link RoleBuilder} and reset to its previous state afterwards
     * @param shouldPersist whether or not to persist at the end of each call to {@link CapabilitiesController#addCapability addCapability}
     */
    public void doPersist(boolean shouldPersist) {
        this.persistOnEachUpdate = shouldPersist;
    }

    /**
     * <p>Check whether or not calls to {@link CapabilitiesController#addCapability addCapability} should persist each time it is called.</p>
     * <p>Default: <b>true</b></p>
     * @return whether or not the Controller will persist on each call
     */
    public boolean willPersist() {
        return this.persistOnEachUpdate;
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
     * @deprecated
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
