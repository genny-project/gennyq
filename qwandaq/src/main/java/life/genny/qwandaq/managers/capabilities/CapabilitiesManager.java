package life.genny.qwandaq.managers.capabilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.bind.JsonbException;

import org.apache.commons.lang3.StringUtils;

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
import life.genny.qwandaq.utils.BaseEntityUtils;
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

	@Inject
	private RoleManager roleMan;

	private Map<CapabilityMode, Attribute> coreAttributes = new HashMap<>();

	/**
	 * Post-Construct initialization of capability attributes based on capability mode enum
	 */
	@PostConstruct
	private void init() {

		// Initialize the capability mode attributes
		CapabilityMode[] modes = CapabilityMode.values();
		for(CapabilityMode mode : modes) {
			String attributeCode = "CAP_".concat(mode.name().toUpperCase());
			Attribute attribute = null;

			try {
				attribute = qwandaUtils.getAttribute(serviceToken.getProductCode(), attributeCode);
			} catch(ItemNotFoundException e) {
				debug("Capability Attribute: " + attributeCode + " not in system under product code: " + serviceToken.getProductCode() + ". Creating now!");
			}
	
			if (attribute == null) {
				attribute = new AttributeText(attributeCode, CommonUtils.normalizeString(mode.name()) + " capability");
				qwandaUtils.saveAttribute(serviceToken.getProductCode(), attribute);
			}

			coreAttributes.put(mode, attribute);
		}
	}

	/**
	 * Add a capability to a BaseEntity.
	 * 
	 * @param productCode    The product code
	 * @param target         The target entity
	 * @param cleanCapabilityCode The capability code
	 * @param modes          The modes to set
	 */
	private void updateCapability(String productCode, BaseEntity target, final String cleanCapabilityCode,
			final CapabilityMode... modes) {
		// Update base entity
		if (cleanCapabilityCode == null) {
			throw new NullParameterException("capability code");
		}

		if(target == null) {
			throw new NullParameterException("target");
		}

		// Update each attribute in modes
		for(CapabilityMode mode : modes) {
			Attribute capAttribute = coreAttributes.get(mode);
			Optional<EntityAttribute> optEa = target.findEntityAttribute(capAttribute);
			
			EntityAttribute entityAttribute;
			
			if(optEa.isPresent()) {
				entityAttribute = optEa.get();
			} else {
				entityAttribute = new EntityAttribute(target, capAttribute, 0.0);
			}

			String capabilityString = entityAttribute.getValueString();
			if(capabilityString == null)
				capabilityString = "";
			
			// add the capability code to the mode set
			capabilityString = CommonUtils.cleanUpAttributeValue(capabilityString);
			Set<String> capabilities = Arrays.asList(capabilityString.split(",")).stream().collect(Collectors.toSet());
			capabilities.add(cleanCapabilityCode);

			// Convert back to comma-delimited array
			String valueString = CommonUtils.getArrayString(capabilities, (String c) -> c);
			entityAttribute.setValue(valueString);
			
			// Update cache and entity attribute
			target.addAttribute(entityAttribute);
			String key = getCacheKey(target.getCode(), entityAttribute.getAttributeCode());
			CacheUtils.putObject(productCode, key, valueString);
		}
		
		beUtils.updateBaseEntity(target);
	}

	/**
	 * Get a map of capabilities for a target, with all of the capability modes as the key and
	 * the capability codes of each as the value
	 * @param target - target base entity
	 * @return a map of modes to capability codes for a target
	 */
	public Map<CapabilityMode, String[]> getTargetCapabilityMap(BaseEntity target) {
		return getTargetCapabilityMap(target, CapabilityMode.values());
	}

	/**
	 * Get a map of capabilities for a target, with each of the requested modes as the key and
	 * the capability codes of each as the value
	 * @param target - target base entity
	 * @param modes - modes to get
	 * @return a map of modes to capability codes for a target
	 */
	public Map<CapabilityMode, String[]> getTargetCapabilityMap(BaseEntity target, CapabilityMode... modes) {
		Map<CapabilityMode, String[]> capMap = new HashMap<>();

		/*
		 * For each capability mode in the requested modes, add a string array
		 * of capability codes that exist in the target's capability attributes
		 */
		for(CapabilityMode mode : modes) {
			String[] capabilities = getTargetCapabilities(target, mode);
			capMap.put(mode, capabilities);
		}

		return capMap;
	}

	public String[] getTargetCapabilities(BaseEntity target, CapabilityMode mode) {
		Attribute capAttrib = coreAttributes.get(mode);
		Optional<EntityAttribute> optEa = target.findEntityAttribute(capAttrib);
		String[] capabilities = new String[0];
		if(optEa.isPresent()) {
			EntityAttribute entityAttribute = optEa.get();
			
			String valueString = entityAttribute.getValueString();
			if(valueString != null) {
				valueString = CommonUtils.cleanUpAttributeValue(valueString);
				capabilities = valueString.split(",");
			}
		}

		return capabilities;
	}

	public BaseEntity addCapabilityToBaseEntity(String productCode, BaseEntity targetBe, String capabilityCode,
			final CapabilityMode... modes) {
		if(capabilityCode == null) {
			throw new ItemNotFoundException(productCode, "Capability Attribute");
		}

		capabilityCode = cleanCapabilityCode(capabilityCode);

		// Check the user token has required capabilities
		if(!shouldOverride()) {
			error(userToken.getUserCode() + " is NOT ALLOWED TO ADD CAP: " + capabilityCode
					+ " TO BASE ENTITITY: " + targetBe.getCode());
			return targetBe;
		}

		// ===== Old capability check ===
		// if (!hasCapability(cleanCapabilityCode, true, modes)) {
		// 	error(userToken.getUserCode() + " is NOT ALLOWED TO ADD CAP: " + cleanCapabilityCode
		// 			+ " TO BASE ENTITITY: " + targetBe.getCode());
		// 	return targetBe;
		// }

		updateCapability(productCode, targetBe, capabilityCode, modes);
		return targetBe;
	}

	/**
	 * Go through a list of capability modes and check that the token can manipulate
	 * the modes for the provided capabilityCode
	 * 
	 * @param capabilityCode capabilityCode to check against (will be cleaned
	 *                          before use)
	 * @param checkModes        array of modes to check against
	 * @return whether or not the token can manipulate all the supplied modes for
	 *         the supplied capabilityCode
	 */
	public boolean hasCapability(final BaseEntity user, String capabilityCode, boolean hasAll, final CapabilityMode... checkModes) {

		// 1. Check override

		// allow keycloak admin and devs to do anything
		if (shouldOverride()) {
			return true;
		}

		// 2. Check user capabilities
		capabilityCode = cleanCapabilityCode(capabilityCode);

		Map<CapabilityMode, String[]> capabilitiesMap = getTargetCapabilityMap(user, checkModes);


		if(hasAll) {
			for(String[] capabilityCodes : capabilitiesMap.values()) {
				if(!CommonUtils.isInArray(capabilityCodes, capabilityCode)) {
					return false;
				}
			}

			return true;
		} else {
			for(String[] capabilityCodes : capabilitiesMap.values()) {
				
			}

			return false;
		}


		// 3. Check user role capabilities
		List<String> roleCodes = beUtils.getBaseEntityCodeArrayFromLinkAttribute(user, ROLE_LINK_CODE);

		try {
			for (String code : roleCodes) {
				BaseEntity role = beUtils.getBaseEntity(code);
				if(role == null) {
					error("Could not find role: " + code);
					continue;
				}
				if(entityHasCapability(role, capabilityCode, hasAll, checkModes))
					return true;
			}
		} catch (RoleException re) {
			error(re.getMessage());
		}

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
		warn("[!] Assessing roles through PRI_IS attribs for user with uuid: " + userToken.getCode());
		if (shouldOverride())
			return true;

		final String cleanCapabilityCode = cleanCapabilityCode(rawCapabilityCode);
		BaseEntity user = beUtils.getUserBaseEntity();
		if (user == null) {
			error("Null user detected for token: " + userToken.getToken());
			return false;
		}
		List<EntityAttribute> priIsAttributes = user.findPrefixEntityAttributes(PRI_IS_PREFIX);

		return priIsAttributes.stream().anyMatch((EntityAttribute priIsAttribute) -> {
			String priIsCode = priIsAttribute.getAttributeCode();
			String roleCode = ROLE_BE_PREFIX + priIsCode.substring(PRI_IS_PREFIX.length());
			BaseEntity roleBe = beUtils.getBaseEntityByCode(roleCode);
			if (roleBe == null) {
				error("Could not find role: " + roleCode);
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
			error("condition is NULL!");
			return false;
		}

		debug("Testing condition with value: " + condition);
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
				error("Could not deserialize CapabilityMode array modeString: " + modeString);
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
			info("Could not find " + targetCode + ":" + cleanCapabilityCode + " in cache");
			return false;
		}

		info("Found " + modes.size() + " modes");
		// Two separate loops so we don't check hasAll over and over again
		if (hasAll) {
			for (CapabilityMode checkMode : checkModes) {
				info("Checking " + checkMode.name());
				boolean hasMode = modes.contains(checkMode);
				info(" 		- Success: " + hasMode);
				if (!hasMode)
					return false;
			}
			return true;
		} else {
			for (CapabilityMode checkMode : checkModes) {
				info("Checking " + checkMode.name());
				boolean hasMode = modes.contains(checkMode);
				info(" 		- Success: " + hasMode);
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
