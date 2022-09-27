package life.genny.qwandaq.managers.capabilities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.bind.JsonbException;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.AttributeText;
import life.genny.qwandaq.attribute.EntityAttribute;

import life.genny.qwandaq.datatype.CapabilityMode;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.checked.RoleException;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.managers.Manager;
import life.genny.qwandaq.managers.capabilities.role.RoleManager;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.CommonUtils;

import static life.genny.qwandaq.constants.GennyConstants.CAP_CODE_PREFIX;
import static life.genny.qwandaq.constants.GennyConstants.PRI_IS_PREFIX;
import static life.genny.qwandaq.constants.GennyConstants.ROLE_BE_PREFIX;

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
	}

	// == TODO LIST
	// 1. I want to get rid of the productCode chain here. When we have multitenancy properly established this should be possible
	// but until then this is my best bet for getting this working reliably (don't trust the tokens just yet, as service token has productCode improperly set)
	
	/*
	 * Refactor Structure
	 */
	
	public List<EntityAttribute> getEntityCapabilities(final String productCode, final BaseEntity target) {
		List<EntityAttribute> capabilities = new ArrayList<>();
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
		List<EntityAttribute> capabilities = getEntityCapabilities(productCode, target);

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
			final CapabilityMode... modes) {
		// Update base entity
		if (capability == null) {
			throw new NullParameterException("capability");
		}

		if(target == null) {
			throw new NullParameterException("target");
		}

		target.addAttribute(capability, 0.0, getModeString(modes));
		CacheUtils.putObject(productCode, target.getCode() + ":" + capability.getCode(), modes);
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
	public boolean checkCapability(EntityAttribute capability, boolean hasAll, CapabilityMode... checkModes) {
		if (StringUtils.isBlank(capability.getValueString())) {
			return false;
		}

		String modeString = capability.getValueString().toUpperCase();
		if (hasAll) {
			for (CapabilityMode checkMode : checkModes) {
				boolean hasMode = modeString.contains(checkMode.name());
				if (!hasMode)
					return false;
			}
			return true;
		} else {
			for (CapabilityMode checkMode : checkModes) {
				boolean hasMode = modeString.contains(checkMode.name());
				if (hasMode)
					return true;
			}
			return false;
		}
	}

	public BaseEntity addCapabilityToBaseEntity(String productCode, BaseEntity targetBe, Attribute capabilityAttribute,
			final CapabilityMode... modes) {
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
			final CapabilityMode... modes) {
				// Ensure the capability is well defined
				String cleanCapabilityCode = cleanCapabilityCode(rawCapabilityCode);

				// Don't need to catch here since we don't want to create
				Attribute attribute = qwandaUtils.getAttribute(productCode, cleanCapabilityCode);

				return addCapabilityToBaseEntity(productCode, targetBe, attribute, modes);
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
	public boolean hasCapability(final BaseEntity user, final String rawCapabilityCode, boolean hasAll, final CapabilityMode... checkModes) {
		CommonUtils.DebugTimer timer = new CommonUtils.DebugTimer(log::debug);
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
	 * @param checkModes - one or more {@link CapabilityMode}s
	 * @return <b>true</b> if target satisfies the requirements specified by the args or <b>false</b> if not
	 * @throws RoleException - if the target doesn't have the capability
	 */
	public boolean entityHasCapability(final BaseEntity target, final String rawCapabilityCode, boolean hasAll, final CapabilityMode... checkModes) 
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
	 * Checks if the user has a capability using any PRI_IS_ attributes.
	 *
	 * NOTE: This should be temporary until the LNK_ROLE attribute is properly in
	 * place!
	 * Lets do it in 10.1.0!!!
	 *
	 * @param rawCapabilityCode The code of the capability.
	 * @param mode              The mode of the capability.
	 * @return Boolean True if the user has the capability, False otherwise.
	 */
	@Deprecated
	public boolean hasCapabilityThroughPriIs(String rawCapabilityCode, CapabilityMode mode) {
		log.warn("[!] Assessing roles through PRI_IS attribs for user with uuid: " + userToken.getCode());
		if (shouldOverride())
			return true;

		final String cleanCapabilityCode = cleanCapabilityCode(rawCapabilityCode);
		BaseEntity user = beUtils.getUserBaseEntity();
		if (user == null) {
			log.error("Null user detected for token: " + userToken.getToken());
			return false;
		}
		List<EntityAttribute> priIsAttributes = user.findPrefixEntityAttributes(PRI_IS_PREFIX);

		return priIsAttributes.stream().anyMatch((EntityAttribute priIsAttribute) -> {
			String priIsCode = priIsAttribute.getAttributeCode();
			String roleCode = ROLE_BE_PREFIX + priIsCode.substring(PRI_IS_PREFIX.length());
			BaseEntity roleBe = beUtils.getBaseEntityByCode(roleCode);
			if (roleBe == null) {
				log.error("Could not find role: " + roleCode);
				return false;
			}

			String modeString = roleBe.getValueAsString(cleanCapabilityCode);
			if (StringUtils.isBlank(modeString))
				return false;
			return modeString.contains(mode.name());
		});
	}

	/**
	 * @param condition the condition to check
	 * 
	 * @return Boolean
	 */
	@Deprecated
	public Boolean conditionMet(String condition) {

		if (StringUtils.isBlank(condition)) {
			log.error("condition is NULL!");
			return false;
		}

		log.debug("Testing condition with value: " + condition);
		String[] conditionArray = condition.split(":");

		String capability = conditionArray[0];
		String mode = conditionArray[1];

		// check for NOT operator
		Boolean not = capability.startsWith("!");
		capability = not ? capability.substring(1) : capability;

		// check for Capability
		BaseEntity user = beUtils.getUserBaseEntity();
		Boolean hasCap = hasCapability(user, capability, false, CapabilityMode.getMode(mode))
				|| hasCapabilityThroughPriIs(capability, CapabilityMode.getMode(mode));

		// XNOR operator
		return hasCap ^ not;
	}

	/**
	 * Deserialise a stringified array of modes to an array of {@link CapabilityMode}
	 * @param modeString
	 * @return
	 */
	public CapabilityMode[] getCapModesFromString(String modeString) {

		JsonArray array = null;
		if (modeString.startsWith("[")) {
			try {
				array = jsonb.fromJson(modeString, JsonArray.class);
			} catch (JsonbException e) {
				log.error("Could not deserialize CapabilityMode array modeString: " + modeString);
				return null;
			}
		} else {
			CapabilityMode mode = CapabilityMode.valueOf(modeString);
			return new CapabilityMode[] { mode };

		}

		CapabilityMode[] modes = new CapabilityMode[array.size()];

		for (int i = 0; i < array.size(); i++) {
			modes[i] = CapabilityMode.valueOf(array.getString(i));
		}

		return modes;
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
	 * Serialize an array of {@link CapabilityMode}s to a string
	 * @param modes
	 * @return
	 */
	public static String getModeString(CapabilityMode... modes) {
		return CommonUtils.getArrayString(modes, (mode) -> mode.name());
	}


	/**
	 * Get a set of capability modes for a target and capability combination.
	 * 
	 * @param target         The target entity
	 * @param capabilityCode The capability code
	 * @return An array of CapabilityModes
	 */
	private CapabilityMode[] getEntityCapabilityFromCache(final String productCode, final String targetCode,
			final String capabilityCode) throws RoleException {

		String key = getCacheKey(targetCode, capabilityCode);

		CapabilityMode[] modes = CacheUtils.getObject(productCode, key, CapabilityMode[].class);
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
			final CapabilityMode... checkModes)
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
			final CapabilityMode... checkModes) {
		Set<CapabilityMode> modes;
		try {
			CapabilityMode[] modeArray = getEntityCapabilityFromCache(productCode, targetCode, cleanCapabilityCode);
			modes = Arrays.asList(modeArray).stream().collect(Collectors.toSet());
		} catch (RoleException e) {
			log.info("Could not find " + targetCode + ":" + cleanCapabilityCode + " in cache");
			return false;
		}

		log.info("Found " + modes.size() + " modes");
		// Two separate loops so we don't check hasAll over and over again
		if (hasAll) {
			for (CapabilityMode checkMode : checkModes) {
				log.info("Checking " + checkMode.name());
				boolean hasMode = modes.contains(checkMode);
				log.info(" 		- Success: " + hasMode);
				if (!hasMode)
					return false;
			}
			return true;
		} else {
			for (CapabilityMode checkMode : checkModes) {
				log.info("Checking " + checkMode.name());
				boolean hasMode = modes.contains(checkMode);
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
