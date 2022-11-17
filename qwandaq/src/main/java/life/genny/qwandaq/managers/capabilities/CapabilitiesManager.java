package life.genny.qwandaq.managers.capabilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import life.genny.qwandaq.models.UserToken;

import java.util.List;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import life.genny.qwandaq.utils.DebugTimer;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.datatype.DataType;

import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.CapabilitySet;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityMode;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.datatype.capability.core.node.PermissionMode;
import life.genny.qwandaq.datatype.capability.requirement.ReqConfig;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.checked.RoleException;
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
	// 1. I want to get rid of the productCode chain here. When we have multitenancy properly established this should be possible
	// but until then this is my best bet for getting this working reliably (don't trust the tokens just yet, as service token has productCode improperly set)
	
	/**
	 * Return a Set of Capabilities based on a BaseEntity's LNK_ROLE and its own set of capabilities
	 * <p>If a {@link UserToken} instance is accessible, this will have already been called and cached,
	 * so {@link UserToken#getUserCapabilities()} will be faster</p>
	 * @return
	 */
	@Deprecated(forRemoval = false)
	public ReqConfig getUserCapabilities(boolean requiresAllCaps, boolean requiresAllModes) {
		// this is a necessary log, since we are trying to minimize how often this function is called
		// it is good to see how often it comes up
		info("[!][!] Generating new User Capabilities for " + userToken.getUserCode());

		BaseEntity userBE = beUtils.getUserBaseEntity();
		List<BaseEntity> roles = roleMan.getRoles(userBE);
		CapabilitySet capabilities;
		
		if(!roles.isEmpty()) {
			info("User Roles:");
			BaseEntity role = roles.get(0);
			capabilities = getEntityCapabilities(role);
			for(int i = 1; i < roles.size(); i++) {
				role = roles.get(i);
				CapabilitySet roleCaps = getEntityCapabilities(role);
				// Being careful about accidentally duplicating capabilities 
				// (given the nature of the hashCode and equals methods in Capability.java)
				for(Capability cap : roleCaps) {
					// Find preexisting capability. If it exists, merge the Nodes in the way that
					// grants the most permission possible
					Capability preexistingCap = cap.hasCodeInSet(capabilities);
					if(preexistingCap != null) {
						capabilities.remove(preexistingCap);
						cap = preexistingCap.merge(cap, true);
					}
					capabilities.add(cap);
				}
			}
		} else {
			capabilities = new CapabilitySet(userBE);
		}

		// Now overwrite with user capabilities
		CapabilitySet userCapabilities = getEntityCapabilities(userBE);
		for(Capability capability : userCapabilities) {
			// Try and find a preexisting capability to overwrite.
			// If it exists, remove so we can override the role-based capability
			Capability otherCapability = capability.hasCodeInSet(capabilities);
			if(otherCapability != null) {
				capabilities.remove(otherCapability);
				capability = otherCapability.merge(capability, false);
			}
			capabilities.add(capability);
		}
		
		return new ReqConfig(capabilities, requiresAllCaps, requiresAllModes);
	}

	public ReqConfig getUserCapabilities(boolean requiresAllCaps) {
		return getUserCapabilities(requiresAllCaps, ReqConfig.DEFAULT_ALL_MODES);
	}

	public ReqConfig getUserCapabilities() {
		return getUserCapabilities(ReqConfig.DEFAULT_ALL_CAPS);
	}
	
	/**
	 * Get a single entity's capabilities (excluding roles)
	 * @param productCode
	 * @param target
	 * @return
	 */
	public CapabilitySet getEntityCapabilities(final BaseEntity target) {
		Set<EntityAttribute> capabilities = new HashSet<>(target.findPrefixEntityAttributes(Prefix.CAP));
		info("		- " + target.getCode() + "(" + capabilities.size() + " capabilities)");
		if(capabilities.isEmpty()) {
			return new CapabilitySet(target);
		}
		CapabilitySet cSet = new CapabilitySet(target);
		cSet.addAll(capabilities.stream()
			.map((EntityAttribute ea) -> {
				System.out.println("	[!] " + ea.getAttributeCode() + " = " + ea.getValueString());
				return Capability.getFromEA(ea);
			})
			.collect(Collectors.toSet()));
		return cSet;
	}

	/**
	 * Add a capability to a BaseEntity.
	 * 
	 * @param productCode    The product code
	 * @param target         The target entity
	 * @param code The capability code
	 * @param nodes          The nodes to set
	 */
	private void updateCapability(String productCode, BaseEntity target, final Attribute capability,
			final CapabilityNode... nodes) {
		// Update base entity
		if (capability == null) {
			throw new NullParameterException("capability");
		}

		if(target == null) {
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

			if(target == null) {
				throw new NullParameterException("target");
			}

			target.addAttribute(capability, 0.0, getModeString(modeList));
			CacheUtils.putObject(productCode, target.getCode() + ":" + capability.getCode(), modeList.toArray(new CapabilityNode[0]));
			beUtils.updateBaseEntity(target);
		}

	public Attribute createCapability(final String productCode, final String rawCapabilityCode, final String name) {
		return createCapability(productCode, rawCapabilityCode, name, false);
	}

	public Attribute createCapability(final String productCode, final String rawCapabilityCode, final String name, boolean cleanedCode) {
		String cleanCapabilityCode = cleanedCode ? rawCapabilityCode : cleanCapabilityCode(rawCapabilityCode);
		Attribute attribute = null;
		try {
			attribute = qwandaUtils.getAttribute(productCode, cleanCapabilityCode);
		} catch(ItemNotFoundException e) {
			log.debug("Could not find Attribute: " + cleanCapabilityCode + ". Creating new Capability");
		}

		if (attribute == null) {
			log.trace("Creating Capability : " + cleanCapabilityCode + " : " + name);
			attribute = new Attribute(cleanCapabilityCode, name, new DataType(String.class));
			qwandaUtils.saveAttribute(productCode, attribute);
		}

		return attribute;
	}

	/**
	 * Check a single EntityAttribute capability if it has one or all of given capability modes
	 * @param capability
	 * @param hasAll
	 * @param checkModes
	 * @return
	 */
	public boolean checkCapability(EntityAttribute capability, boolean hasAll, CapabilityNode... checkModes) {
		if (StringUtils.isBlank(capability.getValueString())) {
			return false;
		}

		String modeString = capability.getValueString();
		Set<CapabilityNode> capabilities = deserializeCapSet(modeString);

		return checkCapability(capabilities, hasAll, checkModes);
	}

	public boolean checkCapability(EntityAttribute capEa, boolean hasAll, Collection<CapabilityNode> checkCollection) {
		return checkCapability(capEa, hasAll, checkCollection.toArray(new CapabilityNode[0]));
	}

	public static boolean checkCapability(Set<CapabilityNode> capabilitySet, boolean hasAll, CapabilityNode... checkModes) {
		capabilitySet = cascadeCapabilities(capabilitySet);
		if (hasAll) {
			for (CapabilityNode checkMode : checkModes) {
				boolean hasMode = capabilitySet.contains(checkMode);
				if (!hasMode) {
					return false;
				}
			}

			return true;
		} else {
			for (CapabilityNode checkMode : checkModes) {
				boolean hasMode = capabilitySet.contains(checkMode);
				if (hasMode) {
					return true;
				}
			}

			System.out.println("Doesn't have at least one of " + CommonUtils.getArrayString(checkModes) + " in " + CommonUtils.getArrayString(capabilitySet));
			return false;
		}
	}

private static Set<CapabilityNode> cascadeCapabilities(Set<CapabilityNode> capSet) {
		// Allocate new list with max size of all combinations of CapMode and PermMode
		List<CapabilityNode> newCaps = new ArrayList<>(capSet.size() * CapabilityMode.values().length * PermissionMode.values().length);
		for(CapabilityNode node : capSet) {
			newCaps.addAll(Arrays.asList(node.getLesserNodes()));
		}

		capSet.addAll(newCaps);
		return capSet;
	}

	public BaseEntity addCapabilityToBaseEntity(String productCode, BaseEntity targetBe, Attribute capabilityAttribute,
			final CapabilityNode... modes) {
		if(capabilityAttribute == null) {
			throw new ItemNotFoundException(productCode, "Capability Attribute");
		}

		// Check the user token has required capabilities
		if(!shouldOverride()) {
			log.error(userToken.getUserCode() + " is NOT ALLOWED TO ADD CAP: " + capabilityAttribute.getCode()
					+ " TO BASE ENTITITY: " + targetBe.getCode());
			return targetBe;
		}

		// ===== Old capability check ===
		// if (!hasCapability(cleanCapabilityCode, true, modes)) {
		// 	log.error(userToken.getUserCode() + " is NOT ALLOWED TO ADD CAP: " + cleanCapabilityCode
		// 			+ " TO BASE ENTITITY: " + targetBe.getCode());
		// 	return targetBe;
		// }

		updateCapability(productCode, targetBe, capabilityAttribute, modes);
		return targetBe;
	}

	public BaseEntity addCapabilityToBaseEntity(String productCode, BaseEntity targetBe, Attribute capabilityAttribute,
			final List<CapabilityNode> modes) {
		if(capabilityAttribute == null) {
			throw new ItemNotFoundException(productCode, "Capability Attribute");
		}

		// Check the user token has required capabilities
		if(!shouldOverride()) {
			log.error(userToken.getUserCode() + " is NOT ALLOWED TO ADD CAP: " + capabilityAttribute.getCode()
					+ " TO BASE ENTITITY: " + targetBe.getCode());
			return targetBe;
		}

		// ===== Old capability check ===
		// if (!hasCapability(cleanCapabilityCode, true, modes)) {
		// 	log.error(userToken.getUserCode() + " is NOT ALLOWED TO ADD CAP: " + cleanCapabilityCode
		// 			+ " TO BASE ENTITITY: " + targetBe.getCode());
		// 	return targetBe;
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

	/**
	 * Go through a list of capability modes and check that the user can manipulate
	 * the modes for the provided capabilityCode
	 * 
	 * @param rawCapabilityCode capabilityCode to check against (will be cleaned
	 *                          before use)
	 * @param checkModes        array of modes to check against
	 * @return whether or not the token can manipulate all the supplied modes for
	 *         the supplied capabilityCode
	 */
	public boolean hasCapability(final BaseEntity user, final String rawCapabilityCode, boolean hasAll, final CapabilityNode... checkModes) {
		DebugTimer timer = new DebugTimer(log::debug);
		// 1. Check override
		// allow keycloak admin and devs to do anything
		if (shouldOverride()) {
			timer.logTime();
			return true;
		}
		// 2. Check user capabilities
		final String cleanCapabilityCode = cleanCapabilityCode(rawCapabilityCode);
		
		if(entityHasCapability(user, cleanCapabilityCode, hasAll, checkModes)) {
			timer.logTime();
			return true;
		}

		// 3. Check user role capabilities
		List<String> roleCodes = beUtils.getBaseEntityCodeArrayFromLinkAttribute(user, Attribute.LNK_ROLE);

		try {
			for (String code : roleCodes) {
				BaseEntity role = beUtils.getBaseEntity(code);
				if(role == null) {
					log.error("Could not find role: " + code);
					continue;
				}

				if(entityHasCapability(role, rawCapabilityCode, hasAll, checkModes)) {
					timer.logTime();
					return true;
				}
			}
		} catch (RoleException re) {
			log.error(re.getMessage());
		}

		timer.logTime();
		return false;
	}


	/**
	 * Check if a (single) entity has one or all capability modes in a capability. This does not account for roles
	 * @param target - target
	 * @param rawCapabilityCode - capability to check
	 * @param hasAll - whether or not the target requires all of the supplied checkModes or just one of them
	 * @param checkModes - one or more {@link CapabilityNode}s
	 * @return <b>true</b> if target satisfies the requirements specified by the args or <b>false</b> if not
	 * @throws RoleException - if the target doesn't have the capability
	 */
	public boolean entityHasCapability(final BaseEntity target, final String rawCapabilityCode, boolean hasAll, final CapabilityNode... checkModes) 
		throws RoleException {
		final String cleanCapabilityCode = cleanCapabilityCode(rawCapabilityCode);
		final String code = target.getCode();

		// check cache first
		if (entityHasCapabilityCached(userToken.getProductCode(), code, cleanCapabilityCode, hasAll, checkModes))
			return true;

		if (entityHasCapabilityFromDB(target, cleanCapabilityCode, hasAll, checkModes))
			return true;

		return false;
	}

	public static Capability deserializeCapability(String capabilityCode, String modeString) {
		List<CapabilityNode> caps = deserializeCapArray(modeString);
		return new Capability(capabilityCode, caps);
	}
	
	/**
	 * Deserialise a stringified array of modes to a set of {@link CapabilityNode}
	 * @param modeString
	 * @return
	 */
	@Deprecated
	public static Set<CapabilityNode> deserializeCapSet(String modeString) {
		return CommonUtils.getSetFromString(modeString, CapabilityNode::parseCapability);
	}

	/**
	 * Deserialise a stringified array of modes to an array of {@link CapabilityNode}
	 * @param modeString
	 * @return
	 */
	@Deprecated
	public static List<CapabilityNode> deserializeCapArray(String modeString) {
		return CommonUtils.getListFromString(modeString, CapabilityNode::parseCapability);
	}

	/**
	 * Clean a raw capability code.
	 * Prepends the Capability Code Prefix if missing and forces uppercase
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
	 * @param productCode - product code to create capability attributes from
	 * @param attribData - 2D array of Strings (each entry in the first array is an array of 2 strings, one for name and one for code)
	 * 			- e.g [ ["CAP_ADMIN", "Manipulate Admin"], ["CAP_STAFF", "Manipulate Staff"]]
	 * @return a map going from attribute code (capability code) to attribute (capability)
	 */
	public Map<String, Attribute> getCapabilityAttributeMap(String productCode, String[][] attribData) {
		Map<String, Attribute> capabilityMap = new HashMap<String, Attribute>();

		Arrays.asList(attribData).stream()
		// Map data to capability. If capability name/tag is missing then use the code with standard capitalisation
		.map((String[] item) -> createCapability(productCode, item[0], (item[1] != null ? item[1] : CommonUtils.normalizeString(item[0]))))
		// add each capability attribute to the capability map, stripping the CAP_ prefix to be used with the constants
		.forEach((Attribute attr) -> capabilityMap.put(attr.getCode(), attr));
		
		return capabilityMap;
	}

	/**
	 * Serialize an array of {@link CapabilityNode}s to a string
	 * @param modes
	 * @return
	 */
	public static String getModeString(CapabilityNode... capabilities) {
		return CommonUtils.getArrayString(capabilities, (capability) -> capability.toString());
	}

	public static String getModeString(Collection<CapabilityNode> capabilities) {
		return CommonUtils.getArrayString(capabilities, (capability) -> capability.toString());
	}

	/**
	 * Get a set of capability modes for a target and capability combination.
	 * 
	 * @param target         The target entity
	 * @param capabilityCode The capability code
	 * @return The Capability pertaining to the target and capabilityCode in cache
	 */
	private Capability getEntityCapabilityFromCache(final String productCode, final String targetCode,
			final String capabilityCode) throws RoleException {

		String key = getCacheKey(targetCode, capabilityCode);

		CapabilityNode[] modes = CacheUtils.getObject(productCode, key, CapabilityNode[].class);
		if (modes == null)
			throw new RoleException("Nothing present for capability combination: ".concat(key));
		Capability cap = new Capability(capabilityCode, modes);
		return cap;
	}

	/**
	 * Check if a target has a capability from the database
	 * @param target
	 * @param cleanCapabilityCode
	 * @param hasAll
	 * @param checkModes
	 * @return
	 * @throws RoleException
	 */
	private boolean entityHasCapabilityFromDB(final BaseEntity target, final String cleanCapabilityCode, boolean hasAll,
			final CapabilityNode... checkModes)
			throws RoleException {

		Optional<EntityAttribute> optBeCapability = target.findEntityAttribute(cleanCapabilityCode);
		if (optBeCapability.isPresent()) {
			EntityAttribute beCapability = optBeCapability.get();
			return checkCapability(beCapability, hasAll, checkModes);
		} else {
			return false;
		}
	}

	/**
	 * Check if a target has a capability in the cache
	 * @param productCode
	 * @param targetCode
	 * @param cleanCapabilityCode
	 * @param hasAll
	 * @param checkModes
	 * @return
	 */
	private boolean entityHasCapabilityCached(final String productCode, final String targetCode, final String cleanCapabilityCode, boolean hasAll,
			final CapabilityNode... checkCapabilities) {
		Capability capability;
		try {
			capability = getEntityCapabilityFromCache(productCode, targetCode, cleanCapabilityCode);
		} catch (RoleException e) {
			log.warn("Could not find " + targetCode + ":" + cleanCapabilityCode + " in cache");
			return false;
		}

		return capability.checkPerms(hasAll, checkCapabilities);
	}

	/**
	 * Construct a cache key for fetching capabilities
	 * 
	 * @param roleCode
	 * @param capCode
	 * @return
	 */
	private static String getCacheKey(String targetCode, String capCode) {
		return new StringBuilder(30)
				.append(targetCode)
				.append(":")
				.append(capCode).toString();
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
