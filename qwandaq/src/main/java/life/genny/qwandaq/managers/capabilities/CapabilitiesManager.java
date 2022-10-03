package life.genny.qwandaq.managers.capabilities;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
import life.genny.qwandaq.attribute.AttributeText;
import life.genny.qwandaq.attribute.EntityAttribute;

import life.genny.qwandaq.datatype.Capability;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.checked.RoleException;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.managers.Manager;
import life.genny.qwandaq.managers.capabilities.role.RoleManager;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.CommonUtils;

import static life.genny.qwandaq.constants.GennyConstants.CAP_CODE_PREFIX;

import static life.genny.qwandaq.constants.GennyConstants.ROLE_LINK_CODE;
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
	
	public Set<EntityAttribute> getEntityCapabilities(final String productCode, final BaseEntity target) {
		Set<EntityAttribute> capabilities = new HashSet<>();
		if(target.isPerson()) {
			List<String> roleCodes = beUtils.getBaseEntityCodeArrayFromLinkAttribute(target, ROLE_LINK_CODE);
			for(String roleCode : roleCodes) {
				BaseEntity role = beUtils.getBaseEntity(roleCode);
				capabilities.addAll(getEntityCapabilities(productCode, role));
			}
		}

		// TODO: Properly Prio User Capabilities
		capabilities.addAll(target.findPrefixEntityAttributes(CAP_CODE_PREFIX));
		return capabilities;
	}

	public Map<String, EntityAttribute> getEntityCapabilitiesMap(final String productCode, final BaseEntity target) {
		Map<String, EntityAttribute> capabilitiesMap = new HashMap<>();
		Set<EntityAttribute> capabilities = getEntityCapabilities(productCode, target);

		for(EntityAttribute cap : capabilities) {
			capabilitiesMap.put(cap.getAttributeCode(), cap);
		}

		return capabilitiesMap;
	}

	/**
	 * Add a capability to a BaseEntity.
	 * 
	 * @param productCode    The product code
	 * @param target         The target entity
	 * @param capabilityCode The capability code
	 * @param modes          The modes to set
	 */
	private void updateCapability(String productCode, BaseEntity target, final Attribute capability,
			final Capability... modes) {
		// Update base entity
		if (capability == null) {
			throw new NullParameterException("capability");
		}

		if(target == null) {
			throw new NullParameterException("target");
		}

		target.addAttribute(capability, 0.0, getModeString(modes));
		CacheUtils.putObject(productCode, target.getCode() + ":" + capability.getCode(), getModeString(modes));
		beUtils.updateBaseEntity(target);
	}

	private void updateCapability(String productCode, BaseEntity target, final Attribute capability,
		final List<Capability> modeList) {
			// Update base entity
			if (capability == null) {
				throw new NullParameterException("capability");
			}

			if(target == null) {
				throw new NullParameterException("target");
			}

			target.addAttribute(capability, 0.0, getModeString(modeList));
			CacheUtils.putObject(productCode, target.getCode() + ":" + capability.getCode(), modeList.toArray(new Capability[0]));
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
			attribute = new AttributeText(cleanCapabilityCode, name);
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
	public boolean checkCapability(EntityAttribute capability, boolean hasAll, Capability... checkModes) {
		String modeString = capability.getValueString();
		Set<Capability> capabilities = deserializeCapSet(modeString);
		if (StringUtils.isBlank(capability.getValueString())) {
			return false;
		}

		if (hasAll) {
			for (Capability checkMode : checkModes) {
				boolean hasMode = capabilities.contains(checkMode);
				if (!hasMode) {
					return false;
				}
			}
			return true;
		} else {
			for (Capability checkMode : checkModes) {
				boolean hasMode = capabilities.contains(checkMode);
				if (hasMode) {
					return true;
				}
			}
			return false;
		}
	}

	public BaseEntity addCapabilityToBaseEntity(String productCode, BaseEntity targetBe, Attribute capabilityAttribute,
			final Capability... modes) {
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
			final List<Capability> modes) {
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
			final Capability... modes) {
				// Ensure the capability is well defined
				String cleanCapabilityCode = cleanCapabilityCode(rawCapabilityCode);

				// Don't need to catch here since we don't want to create
				Attribute attribute = qwandaUtils.getAttribute(productCode, cleanCapabilityCode);

				return addCapabilityToBaseEntity(productCode, targetBe, attribute, modes);
	}

	public BaseEntity addCapabilityToBaseEntity(String productCode, BaseEntity target, final String rawCapCode,
			final List<Capability> capabilityList) {
				// Ensure the capability is well defined
				String cleanCapabilityCode = cleanCapabilityCode(rawCapCode);

				// Don't need to catch here since we don't want to create
				Attribute attribute = qwandaUtils.getAttribute(productCode, cleanCapabilityCode);
				return addCapabilityToBaseEntity(productCode, target, attribute, capabilityList);
			}

	/**
	 * Go through a list of capability modes and check that the token can manipulate
	 * the modes for the provided capabilityCode
	 * 
	 * @param rawCapabilityCode capabilityCode to check against (will be cleaned
	 *                          before use)
	 * @param checkModes        array of modes to check against
	 * @return whether or not the token can manipulate all the supplied modes for
	 *         the supplied capabilityCode
	 */
	public boolean hasCapability(final BaseEntity user, final String rawCapabilityCode, boolean hasAll, final Capability... checkModes) {
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
		List<String> roleCodes = beUtils.getBaseEntityCodeArrayFromLinkAttribute(user, ROLE_LINK_CODE);

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
	 * Check if an entity has one or all capability modes in a capability
	 * @param target - target
	 * @param rawCapabilityCode - capability to check
	 * @param hasAll - whether or not the target requires all of the supplied checkModes or just one of them
	 * @param checkModes - one or more {@link Capability}s
	 * @return <b>true</b> if target satisfies the requirements specified by the args or <b>false</b> if not
	 * @throws RoleException - if the target doesn't have the capability
	 */
	public boolean entityHasCapability(final BaseEntity target, final String rawCapabilityCode, boolean hasAll, final Capability... checkModes) 
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
	
	/**
	 * Deserialise a stringified array of modes to a set of {@link Capability}
	 * @param modeString
	 * @return
	 */
	public Set<Capability> deserializeCapSet(String modeString) {
		return CommonUtils.getSetFromString(modeString, Capability::parseCapability);
	}

	/**
	 * Deserialise a stringified array of modes to an array of {@link Capability}
	 * @param modeString
	 * @return
	 */
	public List<Capability> deserializeCapArray(String modeString) {
		return CommonUtils.getArrayFromString(modeString, Capability::parseCapability);
	}

	/**
	 * Clean a raw capability code.
	 * Prepends the Capability Code Prefix if missing and forces uppercase
	 * @param rawCapabilityCode
	 * @return
	 */
	public static String cleanCapabilityCode(final String rawCapabilityCode) {
		String cleanCapabilityCode = rawCapabilityCode.toUpperCase();
		if (!cleanCapabilityCode.startsWith(CAP_CODE_PREFIX)) {
			cleanCapabilityCode = CAP_CODE_PREFIX + cleanCapabilityCode;
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
	public Map<String, Attribute> getCapabilityMap(String productCode, String[][] attribData) {
		Map<String, Attribute> capabilityMap = new HashMap<String, Attribute>();

		Arrays.asList(attribData).stream()
		// Map data to capability. If capability name/tag is missing then use the code with standard capitalisation
		.map((String[] item) -> createCapability(productCode, item[0], (item[1] != null ? item[1] : CommonUtils.normalizeString(item[0]))))
		// add each capability attribute to the capability map, stripping the CAP_ prefix to be used with the constants
		.forEach((Attribute attr) -> capabilityMap.put(attr.getCode().substring(4), attr));
		
		return capabilityMap;
	}

	/**
	 * Serialize an array of {@link Capability}s to a string
	 * @param modes
	 * @return
	 */
	public static String getModeString(Capability... capabilities) {
		return CommonUtils.getArrayString(capabilities, (capability) -> capability.toString());
	}

	public static String getModeString(List<Capability> capabilities) {
		return CommonUtils.getArrayString(capabilities, (capability) -> capability.toString());
	}

	/**
	 * Get a set of capability modes for a target and capability combination.
	 * 
	 * @param target         The target entity
	 * @param capabilityCode The capability code
	 * @return An array of Capabilitys
	 */
	private Capability[] getEntityCapabilityFromCache(final String productCode, final String targetCode,
			final String capabilityCode) throws RoleException {

		String key = getCacheKey(targetCode, capabilityCode);

		Capability[] modes = CacheUtils.getObject(productCode, key, Capability[].class);
		if (modes == null)
			throw new RoleException("Nothing present for capability combination: ".concat(key));
		return modes;
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
			final Capability... checkModes)
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
			final Capability... checkCapabilities) {
		Set<Capability> modes;
		try {
			Capability[] modeArray = getEntityCapabilityFromCache(productCode, targetCode, cleanCapabilityCode);
			modes = Arrays.asList(modeArray).stream().collect(Collectors.toSet());
		} catch (RoleException e) {
			log.info("Could not find " + targetCode + ":" + cleanCapabilityCode + " in cache");
			return false;
		}

		log.info("Found " + modes.size() + " modes");
		// Two separate loops so we don't check hasAll over and over again
		if (hasAll) {
			for (Capability capability : checkCapabilities) {
				log.info("Checking " + capability.toString());
				boolean hasMode = modes.contains(capability);
				log.info(" 		- Success: " + hasMode);
				if (!hasMode)
					return false;
			}
			return true;
		} else {
			for (Capability capability : checkCapabilities) {
				log.info("Checking " + capability.toString());
				boolean hasMode = modes.contains(capability);
				log.info(" 		- Success: " + hasMode);
				if (hasMode)
					return true;
			}
			return false;
		}
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
