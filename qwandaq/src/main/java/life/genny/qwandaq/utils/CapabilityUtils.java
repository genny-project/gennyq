package life.genny.qwandaq.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbException;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.AttributeText;
import life.genny.qwandaq.attribute.EntityAttribute;

import life.genny.qwandaq.datatype.CapabilityMode;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.checked.RoleException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;


import static life.genny.qwandaq.constants.GennyConstants.LNK_ROLE_CODE;
import static life.genny.qwandaq.constants.GennyConstants.CAP_CODE_PREFIX;
import static life.genny.qwandaq.constants.GennyConstants.PRI_IS_PREFIX;
import static life.genny.qwandaq.constants.GennyConstants.ROLE_BE_PREFIX;

/*
 * A non-static utility class for managing roles and capabilities.
 * 
 * @author Adam Crow
 * @author Jasper Robison
 * @author Bryn Meachem
 */
@ApplicationScoped
public class CapabilityUtils {
	protected static final Logger log = Logger.getLogger(CapabilityUtils.class);
	
	static Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	ServiceToken serviceToken;

	@Inject
	DatabaseUtils dbUtils;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	BaseEntityUtils beUtils;

	private final BaseEntity roleDef;

	public CapabilityUtils() {
		// Should only need to find this once.
		roleDef = beUtils.getBaseEntity("DEF_ROLE");
	}

  public void updateCapability(String productCode, BaseEntity target, String rawCapabilityCode, final CapabilityMode... modes) {
    // find attrbute
    String ccCode = cleanCapabilityCode(rawCapabilityCode);
    Attribute attribute = dbUtils.findAttributeByCode(productCode, ccCode);
    updateCapability(productCode, target, attribute ,modes);
  }

	/**
	 * Add a capability to a BaseEntity.
	 * @param productCode The product code
	 * @param target The target entity
	 * @param capabilityCode The capability code
	 * @param modes The modes to set
	 */
	public void updateCapability(String productCode, BaseEntity target, final Attribute capability,
			final CapabilityMode... modes) {

		// Update base entity
		if(capability == null) {
			throw new NullParameterException("Missing Capability in updateCapability");
		}
		// Save to cache
		String key = getCacheKey(target.getCode(), capability.getCode());

		Set<CapabilityMode> set = new HashSet<>(Arrays.asList(modes));

		CacheUtils.putObject(productCode, key, set);
		log.debugf("[^] Cached in %s -> %s:%s", productCode, key);
	}

	public BaseEntity createRole(String roleCode, String roleName) {
		roleCode = cleanRoleCode(roleCode);
		return beUtils.create(roleDef, roleName, roleCode);
	}

	public BaseEntity inheritRole(BaseEntity role, final BaseEntity parentRole) {
		BaseEntity ret = role;
		List<EntityAttribute> perms = parentRole.findPrefixEntityAttributes(CAP_CODE_PREFIX);
		for (EntityAttribute permissionEA : perms) {
			Attribute permission = permissionEA.getAttribute();
			CapabilityMode[] modes = getCapModesFromString(permissionEA.getValue());
			ret = addCapabilityToBaseEntity(ret, permission.getCode(), modes);
		}
		
		return ret;
	}

	public Attribute createCapability(final String rawCapabilityCode, final String name) {
		return createCapability(rawCapabilityCode, name, false);
	}

	public Attribute createCapability(final String rawCapabilityCode, final String name, boolean cleanedCode) {
		String cleanCapabilityCode = cleanedCode ? rawCapabilityCode : cleanCapabilityCode(rawCapabilityCode);
		
		
		Attribute attribute = qwandaUtils.getAttribute(cleanCapabilityCode);

		if (attribute == null) {
			log.trace("Creating Capability : " + cleanCapabilityCode + " : " + name);
			attribute = new AttributeText(cleanCapabilityCode, name);
			qwandaUtils.saveAttribute(attribute);
		}
		
		return attribute;
	}

	public BaseEntity addCapabilityToBaseEntity(BaseEntity targetBe, final String rawCapabilityCode,
			final CapabilityMode... modes) {
		// Ensure the capability is well defined
		String cleanCapabilityCode = cleanCapabilityCode(rawCapabilityCode);
		// Check the user token has required capabilities
		if (!hasCapability(cleanCapabilityCode, true, modes)) {
			log.error(userToken.getUserCode() + " is NOT ALLOWED TO ADD CAP: " + cleanCapabilityCode
					+ " TO BASE ENTITITY: " + targetBe.getCode());
			return targetBe;
		}

		saveCapability(targetBe.getCode(), cleanCapabilityCode, modes);
		return targetBe;
	}

	/**
	 * Go through a list of capability modes and check that the token can manipulate
	 * the modes for the provided capabilityCode
	 * 
	 * @param rawCapabilityCode capabilityCode to check against (will be cleaned before use)
	 * @param checkModes          array of modes to check against
	 * @return whether or not the token can manipulate all the supplied modes for
	 *         the supplied capabilityCode
	 */
	public boolean hasCapability(final String rawCapabilityCode, boolean hasAll, final CapabilityMode... checkModes) {

		// 1. Check override

		// allow keycloak admin and devcs to do anything
		if (shouldOverride()) {
			return true;
		}

		// 2. Check user capabilities
		BaseEntity user = beUtils.getUserBaseEntity();
		final String cleanCapabilityCode = cleanCapabilityCode(rawCapabilityCode);
		if(entityHasCapability(user.getCode(), cleanCapabilityCode, hasAll, checkModes)) {
			return true;
		}

		// 3. Check user role capabilities
		List<String> roleCodes = beUtils.getBaseEntityCodeArrayFromLinkAttribute(user, LNK_ROLE_CODE);

		for (String code : roleCodes) {
			if(entityHasCapability(code, cleanCapabilityCode, hasAll, checkModes))
				return true;
		}

		return false;
	}

	/**
	 * Checks if the user has a capability using any PRI_IS_ attributes.
	 *
	 * NOTE: This should be temporary until the LNK_ROLE attribute is properly in place!
	 * Lets do it in 10.1.0!!!
	 *
	 * @param rawCapabilityCode The code of the capability.
	 * @param mode The mode of the capability.
	 * @return Boolean True if the user has the capability, False otherwise.
	 */
	@Deprecated
	public boolean hasCapabilityThroughPriIs(String rawCapabilityCode, CapabilityMode mode) {
		log.info("Assessing roles through PRI_IS attribs for user with uuid: " + userToken.getCode());
		if(shouldOverride())
			return true;

		final String cleanCapabilityCode = cleanCapabilityCode(rawCapabilityCode);
		BaseEntity user = beUtils.getUserBaseEntity();
		if(user == null) {
			log.error("Null user detected for token: " + userToken.getToken());
			return false;
		}
		List<EntityAttribute> priIsAttributes = user.findPrefixEntityAttributes(PRI_IS_PREFIX);

		return priIsAttributes.stream().anyMatch((EntityAttribute priIsAttribute) -> {
			String priIsCode = priIsAttribute.getAttributeCode();
			String roleCode = ROLE_BE_PREFIX + priIsCode.substring(PRI_IS_PREFIX.length());
			log.debug("[!] Scanning Role: " + roleCode);
			BaseEntity roleBe = beUtils.getBaseEntityByCode(roleCode);
			if(roleBe == null) {
				log.error("Could not find role: " + roleCode);
				return false;
			}

			String modeString = roleBe.getValueAsString(cleanCapabilityCode);
			if(StringUtils.isBlank(modeString))
				return false;
			return modeString.contains(mode.name());
		});
	}

	/*
	 * @param condition the condition to check
	 * @return Boolean
	 */
	public Boolean conditionMet(String condition) {

		if (condition == null) {
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
		Boolean hasCap = hasCapability(capability, false, CapabilityMode.getMode(mode)) || hasCapabilityThroughPriIs(capability, CapabilityMode.getMode(mode));

		// XNOR operator
		return hasCap ^ not;
	}

	/**
	 * Get a redirect code for user based on their roles.
	 * @return The redirect code
	 * @throws RoleException If no roles are found for the user, or 
	 * 		none of roles found have any associated redirect code
	 */
	public String getUserRoleRedirectCode() throws RoleException {
		
		// grab user role codes
		BaseEntity user = beUtils.getUserBaseEntity();
		List<String> roles = beUtils.getBaseEntityCodeArrayFromLinkAttribute(user, LNK_ROLE_CODE);

		if (roles == null || roles.isEmpty())
			throw new RoleException(String.format("No roles found for user %s", user.getCode()));

		log.info(roles.toString());

		// TODO: return redirect for roles based on priority
		for (String role : roles) {
			try {
				// return first found redirect
				return getRoleRedirectCode(role);
			} catch (RoleException e) {
				log.debug(e.getMessage());
			}
		}

		throw new RoleException(String.format("No redirect in roles %s", roles.toString()));
	}


	/**
	 * Set the redirect code for a role.
	 * @param productCode The product code
	 * @param role The role to set the redirect for
	 * @param redirectCode The code to set as redirect
	 */
	public void setRoleRedirect(String productCode, BaseEntity role, String redirectCode) {
		 
		CacheUtils.putObject(productCode, String.format("%s:REDIRECT", role.getCode()), redirectCode);
	}

	/**
	 * Get the redirect code for a role.
	 * @param roleCode The code of the role
	 * @return The redirect code
	 * @throws RoleException If no redirect is found for the role
	 */
	public String getRoleRedirectCode(String roleCode) throws RoleException {
		
		if (roleCode == null)
			throw new NullParameterException(roleCode);

		String product = userToken.getProductCode();
		String key = roleCode.concat(":REDIRECT");

		log.info(key);

		// TODO: grab redirect for role
		String redirectCode = CacheUtils.getObject(product, key, String.class);

		if (redirectCode == null)
			throw new RoleException("No redirect found in role ".concat(roleCode));

		return redirectCode;
	}
	
	public static String getModeString(CapabilityMode... modes) {
		return CommonUtils.getArrayString(modes, (mode) -> mode.name());
	}

	public static CapabilityMode[] getCapModesFromString(String modeString) {
		
		JsonArray array = null;
		try {
			array = jsonb.fromJson(modeString, JsonArray.class);
		} catch(JsonbException e) {
			log.error("Could not deserialize modeString: " + modeString);
			return null;
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

	public static String cleanRoleCode(final String rawRoleCode) {
		String cleanRoleCode = rawRoleCode.toUpperCase();
		if (!cleanRoleCode.startsWith(ROLE_BE_PREFIX)) {
			cleanRoleCode = ROLE_BE_PREFIX + cleanRoleCode;
		}

		return cleanRoleCode;
	}

	/**
	 * Get a set of capability modes for a target and capability combination.
	 * @param target The target entity
	 * @param capabilityCode The capability code
	 * @return An array of CapabilityModes
	 */
	private Set<CapabilityMode> getEntityCapability(final String targetCode, 
			final String capabilityCode) throws RoleException {

		String productCode = userToken.getProductCode();
		String key = getCacheKey(targetCode, capabilityCode);

		Set<CapabilityMode> modes = CacheUtils.getObject(productCode, key, HashSet.class);
		if (modes == null)
			throw new RoleException("Nothing present for capability combination: ".concat(key));

		return modes;
	}

	/**
	 * @param role
	 * @param capabilityCode
	 * @param mode
	 */
	private String saveCapability(final String beCode, final String cleanCapabilityCode,
			final CapabilityMode... modes) {
		String productCode = userToken.getProductCode();
		String key = getCacheKey(beCode, cleanCapabilityCode);
		String modesString = getModeString(modes);

		return CacheUtils.writeCache(productCode, key, modesString);
	}

	private boolean entityHasCapability(final String targetCode, final String cleanCapabilityCode, boolean hasAll, final CapabilityMode... checkModes) {
		Set<CapabilityMode> modes = null;
		try {
			modes = getEntityCapability(targetCode, cleanCapabilityCode);
		} catch (RoleException e) {
			log.debug(e.getMessage());
			return false;
		}

		// Two separate loops so we don't check hasAll over and over again
		if(hasAll) {
			for (CapabilityMode checkMode : checkModes) {
				boolean hasMode = modes.contains(checkMode);
				if(!hasMode)
					return false;
			}
		} else {
			for (CapabilityMode checkMode : checkModes) {
				boolean hasMode = modes.contains(checkMode);
				if(hasMode)
					return true;
			}
		}

		return false;
	}

	/**
	 * Construct a cache key for fetching capabilities
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
}
