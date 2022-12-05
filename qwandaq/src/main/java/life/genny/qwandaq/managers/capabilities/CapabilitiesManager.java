package life.genny.qwandaq.managers.capabilities;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import java.util.List;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.datatype.DataType;

import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.CapabilitySet;

import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.managers.Manager;
import life.genny.qwandaq.managers.capabilities.role.RoleManager;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.CommonUtils;

/*
 * A non-static utility class for managing roles and capabilities.
 * 
 * @author Adam Crow
 * @author Jasper Robison
 * @author Bryn Meachem
 */
@ApplicationScoped
public class CapabilitiesManager extends Manager {
	protected static final Logger log = Logger.getLogger(CapabilitiesManager.class);

	@Inject
	private RoleManager roleMan;

	public CapabilitiesManager() {
		super();
	}

	// == TODO LIST
	// 1. I want to get rid of the productCode chain here. When we have multitenancy
	// properly established this should be possible
	// but until then this is my best bet for getting this working reliably (don't
	// trust the tokens just yet, as service token has productCode improperly set)

	/**
	 * Return a Set of Capabilities based on a BaseEntity's LNK_ROLE and its own set
	 * of capabilities
	 * 
	 * @param target - the BaseEntity to fetch user capabilities for
	 * 
	 * @return a new {@link CapabilitySet} (a HashSet of Capabilities with knowledge of the target)
	 */
	@Deprecated(forRemoval = false)
	public CapabilitySet getUserCapabilities(BaseEntity target) {
// this is a necessary log, since we are trying to minimize how often this
		// function is called
		// it is good to see how often it comes up
		debug("[!][!] Generating new User Capabilities for " + userToken.getUserCode());

		List<BaseEntity> roles = roleMan.getRoles(target);
		CapabilitySet capabilities;
		
		if(!roles.isEmpty()) {
			debug("User Roles:");
			
			BaseEntity role = roles.get(0);
			capabilities = getEntityCapabilities(role);
			for (int i = 1; i < roles.size(); i++) {
				role = roles.get(i);
				CapabilitySet roleCaps = getEntityCapabilities(role);
				// Being careful about accidentally duplicating capabilities
				// (given the nature of the hashCode and equals methods in Capability.java)
				for (Capability cap : roleCaps) {
					// Find preexisting capability. If it exists, merge the Nodes in the way that
					// grants the most permission possible
					Capability preexistingCap = cap.hasCodeInSet(capabilities);
					if (preexistingCap != null) {
						capabilities.remove(preexistingCap);
						cap = preexistingCap.merge(cap, true);
					}
					capabilities.add(cap);
				}
			}
		} else {
			capabilities = new CapabilitySet(target);
		}

		// Now overwrite with user capabilities
		CapabilitySet userCapabilities = getEntityCapabilities(target);
		for (Capability capability : userCapabilities) {
			// Try and find a preexisting capability to overwrite.
			// If it exists, remove so we can override the role-based capability
			Capability otherCapability = capability.hasCodeInSet(capabilities);
			if (otherCapability != null) {
				capabilities.remove(otherCapability);
				capability = otherCapability.merge(capability, false);
			}
			capabilities.add(capability);
		}
		return capabilities;
	}

	/**
	 * Return a Set of Capabilities based on a BaseEntity's LNK_ROLE and its own set
	 * of capabilities
	 * @return a new {@link CapabilitySet} (a HashSet of Capabilities with knowledge of the target)
	 */
	@Deprecated(forRemoval = false)
	public CapabilitySet getUserCapabilities() {
		return getUserCapabilities(beUtils.getUserBaseEntity());
	}

	/**
	 * Get a single entity's capabilities (excluding roles)
	 * 
	 * @param productCode
	 * @param target
	 * @return
	 */
	public CapabilitySet getEntityCapabilities(final BaseEntity target) {
		Set<EntityAttribute> capabilities = new HashSet<>(target.findPrefixEntityAttributes(Prefix.CAP));
		debug("		- " + target.getCode() + "(" + capabilities.size() + " capabilities)");
		if(capabilities.isEmpty()) {
			return new CapabilitySet(target);
		}
		CapabilitySet cSet = new CapabilitySet(target);
		cSet.addAll(capabilities.stream()
			.map((EntityAttribute ea) -> {
				trace("	[!] " + ea.getAttributeCode() + " = " + ea.getValueString());
				return Capability.getFromEA(ea);
			})
			.collect(Collectors.toSet()));
		return cSet;
	}

	/**
	 * Add a capability to a BaseEntity.
	 * 
	 * @param productCode The product code
	 * @param target      The target entity
	 * @param code        The capability code
	 * @param nodes       The nodes to set
	 */
	private void updateCapability(String productCode, BaseEntity target, final Attribute capability,
			final CapabilityNode... nodes) {
		// Update base entity
		if (capability == null) {
			throw new NullParameterException("capability");
		}

		if (target == null) {
			throw new NullParameterException("target");
		}

		target.addAttribute(capability, 0.0, getModeString(nodes));
		CacheUtils.putObject(productCode, target.getCode() + ":" + capability.getCode(), getModeString(nodes));
		beUtils.updateBaseEntity(target);
	}

	private void updateCapability(String productCode, BaseEntity target, final Attribute capability,
			final List<CapabilityNode> modeList) {
		// Update base entity
		if (capability == null) {
			throw new NullParameterException("capability");
		}

		if (target == null) {
			throw new NullParameterException("target");
		}

		target.addAttribute(capability, 0.0, getModeString(modeList));
		CacheUtils.putObject(productCode, target.getCode() + ":" + capability.getCode(),
				modeList.toArray(new CapabilityNode[0]));
		beUtils.updateBaseEntity(target);
	}

	public Attribute createCapability(final String productCode, final String rawCapabilityCode, final String name) {
		return createCapability(productCode, rawCapabilityCode, name, false);
	}

	public Attribute createCapability(final String productCode, final String rawCapabilityCode, final String name,
			boolean cleanedCode) {
		String cleanCapabilityCode = cleanedCode ? rawCapabilityCode : cleanCapabilityCode(rawCapabilityCode);
		Attribute attribute = null;
		try {
			attribute = qwandaUtils.getAttribute(productCode, cleanCapabilityCode);
		} catch (ItemNotFoundException e) {
			log.debug("Could not find Attribute: " + cleanCapabilityCode + ". Creating new Capability");
		}

		if (attribute == null) {
			log.trace("Creating Capability : " + cleanCapabilityCode + " : " + name);
			attribute = new Attribute(cleanCapabilityCode, name, new DataType(String.class));
			qwandaUtils.saveAttribute(productCode, attribute);
		}

		return attribute;
	}

	public BaseEntity addCapabilityToBaseEntity(String productCode, BaseEntity targetBe, Attribute capabilityAttribute,
			final CapabilityNode... modes) {
		if (capabilityAttribute == null) {
			throw new ItemNotFoundException(productCode, "Capability Attribute");
		}

		// Check the user token has required capabilities
		if (!shouldOverride()) {
			log.error(userToken.getUserCode() + " is NOT ALLOWED TO ADD CAP: " + capabilityAttribute.getCode()
					+ " TO BASE ENTITITY: " + targetBe.getCode());
			return targetBe;
		}

		// ===== Old capability check ===
		// if (!hasCapability(cleanCapabilityCode, true, modes)) {
		// log.error(userToken.getUserCode() + " is NOT ALLOWED TO ADD CAP: " +
		// cleanCapabilityCode
		// + " TO BASE ENTITITY: " + targetBe.getCode());
		// return targetBe;
		// }

		updateCapability(productCode, targetBe, capabilityAttribute, modes);
		return targetBe;
	}

	public BaseEntity addCapabilityToBaseEntity(String productCode, BaseEntity targetBe, Attribute capabilityAttribute,
			final List<CapabilityNode> modes) {
		if (capabilityAttribute == null) {
			throw new ItemNotFoundException(productCode, "Capability Attribute");
		}

		// Check the user token has required capabilities
		if (!shouldOverride()) {
			log.error(userToken.getUserCode() + " is NOT ALLOWED TO ADD CAP: " + capabilityAttribute.getCode()
					+ " TO BASE ENTITITY: " + targetBe.getCode());
			return targetBe;
		}

		// ===== Old capability check ===
		// if (!hasCapability(cleanCapabilityCode, true, modes)) {
		// log.error(userToken.getUserCode() + " is NOT ALLOWED TO ADD CAP: " +
		// cleanCapabilityCode
		// + " TO BASE ENTITITY: " + targetBe.getCode());
		// return targetBe;
		// }

		updateCapability(productCode, targetBe, capabilityAttribute, modes);
		return targetBe;
	}

	public BaseEntity addCapabilityToBaseEntity(String productCode, BaseEntity targetBe, final String rawCapabilityCode,
			final CapabilityNode... modes) {
		// Ensure the capability is well defined
		String cleanCapabilityCode = cleanCapabilityCode(rawCapabilityCode);

		// Don't need to catch here since we don't want to create
		Attribute attribute = qwandaUtils.getAttribute(productCode, cleanCapabilityCode);

		return addCapabilityToBaseEntity(productCode, targetBe, attribute, modes);
	}

	public BaseEntity addCapabilityToBaseEntity(String productCode, BaseEntity target, final String rawCapCode,
			final List<CapabilityNode> capabilityList) {
		// Ensure the capability is well defined
		String cleanCapabilityCode = cleanCapabilityCode(rawCapCode);

		// Don't need to catch here since we don't want to create
		Attribute attribute = qwandaUtils.getAttribute(productCode, cleanCapabilityCode);
		return addCapabilityToBaseEntity(productCode, target, attribute, capabilityList);
	}

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
		if (!cleanCapabilityCode.startsWith(Prefix.CAP)) {
			cleanCapabilityCode = Prefix.CAP + cleanCapabilityCode;
		}

		return cleanCapabilityCode;
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
	 */
	public Map<String, Attribute> getCapabilityAttributeMap(String productCode, String[][] attribData) {
		Map<String, Attribute> capabilityMap = new HashMap<String, Attribute>();

		Arrays.asList(attribData).stream()
				// Map data to capability. If capability name/tag is missing then use the code
				// with standard capitalisation
				.map((String[] item) -> createCapability(productCode, item[0],
						(item[1] != null ? item[1] : CommonUtils.normalizeString(item[0]))))
				// add each capability attribute to the capability map, stripping the CAP_
				// prefix to be used with the constants
				.forEach((Attribute attr) -> capabilityMap.put(attr.getCode(), attr));

		return capabilityMap;
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

	private boolean shouldOverride() {
		// allow keycloak admin and devcs to do anything
		return (userToken.hasRole("admin", "dev") || ("service".equals(userToken.getUsername())));
	}

	// For use in builder patterns
	public RoleManager getRoleManager() {
		return roleMan;
	}
}
