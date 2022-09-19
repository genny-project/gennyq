package life.genny.qwandaq.managers.capabilities;

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

import static life.genny.qwandaq.constants.GennyConstants.LNK_ROLE_CODE;
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
	public boolean hasCapability(final String rawCapabilityCode, boolean hasAll, final CapabilityMode... checkModes) {

		// 1. Check override

		// allow keycloak admin and devs to do anything
		if (shouldOverride()) {
			return true;
		}

		// 2. Check user capabilities
		BaseEntity user = beUtils.getUserBaseEntity();
		final String cleanCapabilityCode = cleanCapabilityCode(rawCapabilityCode);
		if(entityHasCapability(user, cleanCapabilityCode, hasAll, checkModes))
			return true;

		// 3. Check user role capabilities
		List<String> roleCodes = beUtils.getBaseEntityCodeArrayFromLinkAttribute(user, LNK_ROLE_CODE);

		try {
			for (String code : roleCodes) {
				BaseEntity role = beUtils.getBaseEntity(code);
				if(role == null) {
					log.error("Could not find role: " + code);
					continue;
				}
				if(entityHasCapability(role, rawCapabilityCode, hasAll, checkModes))
					return true;
			}
		} catch (RoleException re) {
			log.error(re.getMessage());
		}

		return false;
	}


	public boolean entityHasCapability(final BaseEntity entity, final String rawCapabilityCode, boolean hasAll, final CapabilityMode... checkModes) 
		throws RoleException {
		final String cleanCapabilityCode = cleanCapabilityCode(rawCapabilityCode);
		final String code = entity.getCode();
		
		// check cache first
		if (entityHasCapabilityCached(userToken.getProductCode(), code, cleanCapabilityCode, hasAll, checkModes))
			return true;

		if (entityHasCapabilityFromDB(entity, cleanCapabilityCode, hasAll, checkModes))
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
		Boolean hasCap = hasCapability(capability, false, CapabilityMode.getMode(mode))
				|| hasCapabilityThroughPriIs(capability, CapabilityMode.getMode(mode));

		// XNOR operator
		return hasCap ^ not;
	}

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

	public static String cleanCapabilityCode(final String rawCapabilityCode) {
		String cleanCapabilityCode = rawCapabilityCode.toUpperCase();
		if (!cleanCapabilityCode.startsWith(CAP_CODE_PREFIX)) {
			cleanCapabilityCode = CAP_CODE_PREFIX + cleanCapabilityCode;
		}

		return cleanCapabilityCode;
	}

	public Map<String, Attribute> getCapabilityMap(String productCode, String[][] attribData) {
		Map<String, Attribute> capabilityMap = new HashMap<String, Attribute>();

		Arrays.asList(attribData).stream()
		// Map data to capability. If capability name/tag is missing then use the code with standard capitalisation
		.map((String[] item) -> createCapability(productCode, item[0], (item[1] != null ? item[1] : normalizeCode(item[0]))))
		// add each capability attribute to the capability map, stripping the CAP_ prefix to be used with the constants
		.forEach((Attribute attr) -> capabilityMap.put(attr.getCode().substring(4), attr));
		
		return capabilityMap;
	}

	public static String getModeString(CapabilityMode... modes) {
		return CommonUtils.getArrayString(modes, (mode) -> mode.name());
	}

	private String normalizeCode(String code) {
		return code.substring(0, 1).toUpperCase() + code.substring(1).toLowerCase();
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

	private boolean entityHasCapabilityFromDB(final BaseEntity target, final String cleanCapabilityCode, boolean hasAll,
			final CapabilityMode... checkModes)
			throws RoleException {

		Optional<EntityAttribute> optBeCapability = target.findEntityAttribute(cleanCapabilityCode);
		if (optBeCapability.isPresent()) {
			EntityAttribute beCapability = optBeCapability.get();
			if (StringUtils.isBlank(beCapability.getValueString())) {
				throw new RoleException(
					"BaseEntity: " + target.getCode() + "does not have capability in database: " + cleanCapabilityCode);
			}

			String modeString = beCapability.getValueString().toUpperCase();
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
		} else {
			throw new RoleException(
				"BaseEntity: " + target.getCode() + "does not have capability in database: " + cleanCapabilityCode);
		}
	}

	private boolean entityHasCapabilityCached(final String productCode, final String targetCode, final String cleanCapabilityCode, boolean hasAll,
			final CapabilityMode... checkModes) {
		Set<CapabilityMode> modes;
		try {
			CapabilityMode[] modeArray = getEntityCapabilityFromCache(productCode, targetCode, cleanCapabilityCode);
			modes = Arrays.asList(modeArray).stream().collect(Collectors.toSet());
		} catch (RoleException e) {
			return false;
		}

		// Two separate loops so we don't check hasAll over and over again
		if (hasAll) {
			for (CapabilityMode checkMode : checkModes) {
				boolean hasMode = modes.contains(checkMode);
				if (!hasMode)
					return false;
			}
			return true;
		} else {
			for (CapabilityMode checkMode : checkModes) {
				boolean hasMode = modes.contains(checkMode);
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
