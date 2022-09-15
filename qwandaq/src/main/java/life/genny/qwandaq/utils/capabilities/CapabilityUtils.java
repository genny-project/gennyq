package life.genny.qwandaq.utils.capabilities;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbException;
import javax.persistence.NoResultException;

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
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.QwandaUtils;

import static life.genny.qwandaq.constants.GennyConstants.LNK_ROLE_CODE;
import static life.genny.qwandaq.constants.GennyConstants.CAP_CODE_PREFIX;
import static life.genny.qwandaq.constants.GennyConstants.PRI_IS_PREFIX;
import static life.genny.qwandaq.constants.GennyConstants.ROLE_BE_PREFIX;
import static life.genny.qwandaq.constants.GennyConstants.DEF_ROLE_CODE;

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

	private BaseEntity roleDef;
	private Attribute lnkRolAttribute;

	public CapabilityUtils() {
	}

	// == TODO LIST
	// 1. I want to get rid of the productCode chain here. When we have multitenancy properly established this should be possible
	// but until then this is my best bet for getting this working reliably (don't trust the tokens just yet, as service token has productCode improperly set)

	@PostConstruct
	public void init() {		
		// Should only need to find this once.
		roleDef = beUtils.getBaseEntity(DEF_ROLE_CODE);
		if(roleDef == null) {
			log.error("Could not find DEF_ROLE BASE ENTITY!");
		}
		lnkRolAttribute = dbUtils.findAttributeByCode(userToken.getProductCode(), LNK_ROLE_CODE);
		if(lnkRolAttribute == null) {
			log.error("Could not find LNK_ROLE attribute!");
		}
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

		target.addAttribute(capability, 0.0, getModeString(modes));
		CacheUtils.putObject(productCode, target.getCode() + ":" + capability.getCode(), modes);
		beUtils.updateBaseEntity(target);
	}

	public BaseEntity createRole(String productCode, String roleCode, String roleName) {
		
		BaseEntity role = null;
		roleCode = cleanRoleCode(roleCode);
		try {
			role = dbUtils.findBaseEntityByCode(productCode, roleCode);
		} catch(NoResultException e) {
			role = new BaseEntity(roleCode, roleName);
			role.setRealm(productCode);
			beUtils.updateBaseEntity(role);
		}
		
		return role;
	}

	public BaseEntity inheritRole(String productCode, BaseEntity role, final BaseEntity parentRole) {
		BaseEntity ret = role;
		List<EntityAttribute> perms = parentRole.findPrefixEntityAttributes(CAP_CODE_PREFIX);
		for (EntityAttribute permissionEA : perms) {
			Attribute permission = permissionEA.getAttribute();
			CapabilityMode[] modes = getCapModesFromString(permissionEA.getValue());
			ret = addCapabilityToBaseEntity(productCode, ret, permission.getCode(), modes);

			beUtils.updateBaseEntity(ret);
		}

		return ret;
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

	public BaseEntity attachRole(BaseEntity target, String roleCode) {
		
		// Check we're working with a person
		if(!target.isPerson())
			throw new RoleException("Error attaching role to target: " + target.getCode() + ". Target is not a person");
		
		roleCode = cleanRoleCode(roleCode);
		BaseEntity role = beUtils.getBaseEntity(roleCode);

		if(role == null) 
			throw new RoleException("Error attaching role: " + roleCode + ". Role does not exist");

		return attachRole(target, role);
	}

	public BaseEntity attachRole(BaseEntity target, BaseEntity role) {
		Optional<EntityAttribute> eaOpt = target.findEntityAttribute(LNK_ROLE_CODE);

		// Create it
		if(!eaOpt.isPresent()) {
			target.addAttribute(lnkRolAttribute, 1.0, "[" + role.getCode() + "]");
			beUtils.updateBaseEntity(target);
			return target;
		}

		EntityAttribute lnkRoleEA = eaOpt.get();
		String value = lnkRoleEA.getValueString();
		if(!StringUtils.isBlank(value)) {
			Set<String> values = Arrays.asList(beUtils.cleanUpAttributeValue(value).split(",")).stream().collect(Collectors.toSet());
			values.add(role.getCode());
			value = CommonUtils.getArrayString(values, (String v) -> v);			
		} else {
			value = "[" + role.getCode() + "]";
		}

		lnkRoleEA.setValue(value);

		beUtils.updateBaseEntity(target);
		return target;
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

		if (entityHasCapabilityCached(userToken.getProductCode(), user.getCode(), cleanCapabilityCode, hasAll, checkModes)) {
			return true;
		}

		if (entityHasCapability(user, cleanCapabilityCode, hasAll, checkModes))
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
				if(roleHasCapability(role, rawCapabilityCode, hasAll, checkModes))
					return true;
			}
		} catch (RoleException re) {
			log.error(re.getMessage());
		}

		return false;
	}

	public boolean roleHasCapability(final BaseEntity role, final String rawCapabilityCode, boolean hasAll, final CapabilityMode... checkModes) 
		throws RoleException {
		final String cleanCapabilityCode = cleanCapabilityCode(rawCapabilityCode);
		final String code = role.getCode();
		// check cache first
		if (entityHasCapabilityCached(userToken.getProductCode(), code, cleanCapabilityCode, hasAll, checkModes))
			return true;

		if (entityHasCapability(role, cleanCapabilityCode, hasAll, checkModes))
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

	/*
	 * @param condition the condition to check
	 * 
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
		Boolean hasCap = hasCapability(capability, false, CapabilityMode.getMode(mode))
				|| hasCapabilityThroughPriIs(capability, CapabilityMode.getMode(mode));

		// XNOR operator
		return hasCap ^ not;
	}

	/**
	 * Get a redirect code for user based on their roles.
	 * 
	 * @return The redirect code
	 * @throws RoleException If no roles are found for the user, or
	 *                       none of roles found have any associated redirect code
	 */
	public String getUserRoleRedirectCode() throws RoleException {

		// grab user role codes
		BaseEntity user = beUtils.getUserBaseEntity();
		List<String> roles = beUtils.getBaseEntityCodeArrayFromLinkAttribute(user, LNK_ROLE_CODE);

		if (roles == null || roles.isEmpty())
			throw new RoleException(String.format("No roles found for user %s", user.getCode()));

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
	 * 
	 * @param productCode  The product code
	 * @param role         The role to set the redirect for
	 * @param redirectCode The code to set as redirect
	 */
	public void setRoleRedirect(String productCode, BaseEntity role, String redirectCode) {

		CacheUtils.putObject(productCode, String.format("%s:REDIRECT", role.getCode()), redirectCode);
	}

	/**
	 * Get the redirect code for a role.
	 * 
	 * @param roleCode The code of the role
	 * @return The redirect code
	 * @throws RoleException If no redirect is found for the role
	 */
	public String getRoleRedirectCode(String roleCode) throws RoleException {

		if (roleCode == null)
			throw new NullParameterException(roleCode);

		String product = userToken.getProductCode();
		String key = roleCode.concat(":REDIRECT");

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

	public static String cleanRoleCode(final String rawRoleCode) {
		String cleanRoleCode = rawRoleCode.toUpperCase();
		if (!cleanRoleCode.startsWith(ROLE_BE_PREFIX)) {
			cleanRoleCode = ROLE_BE_PREFIX + cleanRoleCode;
		}

		return cleanRoleCode;
	}



	public Map<String, Attribute> getMap(String productCode, String[][] attribData) {
		Map<String, Attribute> capabilityMap = new HashMap<String, Attribute>();

		Arrays.asList(attribData).stream()
		// Map data to capability. If capability name/tag is missing then use the code with standard capitalisation
		.map((String[] item) -> createCapability(productCode, item[0], (item[1] != null ? item[1] : normalizeCode(item[0]))))
		// add each capability attribute to the capability map, stripping the CAP_ prefix to be used with the constants
		.forEach((Attribute attr) -> capabilityMap.put(attr.getCode().substring(4), attr));
		
		return capabilityMap;
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

	private boolean entityHasCapability(final BaseEntity target, final String cleanCapabilityCode, boolean hasAll,
			final CapabilityMode... checkModes)
			throws RoleException {
		RoleException re = new RoleException(
				"BaseEntity: " + target.getCode() + "does not have capability in database: " + cleanCapabilityCode);

		Optional<EntityAttribute> optBeCapability = target.findEntityAttribute(cleanCapabilityCode);
		if (optBeCapability.isPresent()) {
			EntityAttribute beCapability = optBeCapability.get();
			if (beCapability.getValueString() == null) {
				throw re;
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
			throw re;
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
}
