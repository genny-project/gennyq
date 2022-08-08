package life.genny.qwandaq.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import io.vertx.core.json.DecodeException;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.AttributeText;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.datatype.Allowed;
import life.genny.qwandaq.datatype.CapabilityMode;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.serialization.baseentity.BaseEntityKey;

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

	// Capability Attribute Prefix
	public static final String CAP_CODE_PREFIX = "PRM_";
	public static final String ROLE_BE_PREFIX = "ROL_";

	public static final String PRI_IS_PREFIX = "PRI_IS_";

	// TODO: Confirm we want DEFs to have capabilities as well
	public static final String[] ACCEPTED_CAP_PREFIXES = { ROLE_BE_PREFIX, "PER_", "DEF_" };

	public static final String LNK_ROLE_CODE = "LNK_ROLE";
	public static final String LNK_DEF_CODE = "LNK_DEF";

	List<Attribute> capabilityManifest = new ArrayList<Attribute>();

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

	static BaseEntity ROL_DEF;

	public CapabilityUtils() {
		ROL_DEF = beUtils.getBaseEntityByCode("DEF_ROLE");
	}

	public BaseEntity createRole(String roleCode) {
		roleCode = sanitizeRoleCode(roleCode);
		BaseEntity role = beUtils.create(ROL_DEF, roleCode.substring(4), roleCode);
		log.debug("Created role: " + role.getCode());
		return role;
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

	public Attribute addCapability(final String rawCapabilityCode, final String name) {
		String cleanCapabilityCode = cleanCapabilityCode(rawCapabilityCode);
		log.info("Setting Capability : " + cleanCapabilityCode + " : " + name);
		
		Attribute attribute = qwandaUtils.getAttribute(cleanCapabilityCode);

		if (attribute == null) {
			attribute = new AttributeText(cleanCapabilityCode, name);
			qwandaUtils.saveAttribute(attribute);
		}

		capabilityManifest.add(attribute);
		return attribute;
	}

	public BaseEntity addCapabilityToBaseEntity(BaseEntity targetBe, final String rawCapabilityCode,
			final CapabilityMode... modes) {
		// Ensure the capability is well defined
		String cleanCapabilityCode = cleanCapabilityCode(rawCapabilityCode);
		// Check the user token has required capabilities
		if (!hasCapability(cleanCapabilityCode, modes)) {
			log.error(userToken.getUserCode() + " is NOT ALLOWED TO ADD CAP: " + cleanCapabilityCode
					+ " TO BASE ENTITITY: " + targetBe.getCode());
			return targetBe;
		}

		updateCachedRoleSet(targetBe.getCode(), cleanCapabilityCode, modes);
		return targetBe;
	}

	private CapabilityMode[] getCapabilitiesFromCache(final String roleCode, final String cleanCapabilityCode) {
		String productCode = userToken.getProductCode();
		String key = getCacheKey(roleCode, cleanCapabilityCode);
		String cachedObject = (String) CacheUtils.readCache(productCode, key);

		JsonObject object = jsonb.fromJson(cachedObject, JsonObject.class);

		if ("error".equals(object.getString("status"))) {
			log.error("Error reading cache for realm: " + productCode + " with key: " + key);
			return null;
		}

		String modeString = object.getString("value");
		return getCapModesFromString(modeString);
	}

	/**
	 * @param role
	 * @param capabilityCode
	 * @param mode
	 */
	private String updateCachedRoleSet(final String roleCode, final String cleanCapabilityCode,
			final CapabilityMode... modes) {
		String productCode = userToken.getProductCode();
		String key = getCacheKey(roleCode, cleanCapabilityCode);
		String modesString = getModeString(modes);

		log.info("updateCachedRoleSet test:: " + key);
		// if no cache then create
		return CacheUtils.writeCache(productCode, key, modesString);
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
	public boolean hasCapability(final String rawCapabilityCode, final CapabilityMode... checkModes) {
		// allow keycloak admin and devcs to do anything
		if (userToken.hasRole("admin", "dev")) {
			return true;
		}
		final String cleanCapabilityCode = cleanCapabilityCode(rawCapabilityCode);
		BaseEntity user = beUtils.getUserBaseEntity();

		// Look through all the user entity attributes for the capability code. If it is
		// there check the cache with the roleCode as the userCode
		// TODO: Will need to revisit this implementation with Jasper

		Optional<EntityAttribute> lnkRole = user.findEntityAttribute(LNK_ROLE_CODE);

		// Make a list for the modes that have been found in the user's various roles
		// TODO: Potentially change this to a system that matches from multiple roles
		// instead of a single role
		// List<CapabilityMode> foundModes = new ArrayList<>();

		if (lnkRole.isPresent()) {
			String rolesValue = lnkRole.get().getValueString();
			try {
				// Look through cache using each role
				JsonArray roleArray = jsonb.fromJson(rolesValue, JsonArray.class);
				for (int i = 0; i < roleArray.size(); i++) {
					String roleCode = roleArray.getString(i);

					CapabilityMode[] modes = getCapabilitiesFromCache(roleCode, cleanCapabilityCode);
					List<CapabilityMode> modeList = Arrays.asList(modes);
					for (CapabilityMode checkMode : checkModes) {
						if (!modeList.contains(checkMode))
							return false;
					}
				}

				// There is a malformed LNK_ROLE Attribute, so we assume they don't have the
				// capability
			} catch (DecodeException exception) {
				log.error("Error decoding LNK_ROLE for BaseEntity: " + user.getCode());
				log.error("Value: " + rolesValue + ". Expected: a json array of roles");
				return false;
			}
		}

		// TODO: Implement user checking
		Set<EntityAttribute> entityAttributes = user.getBaseEntityAttributes();
		for(EntityAttribute eAttribute : entityAttributes) {
			if(!eAttribute.getAttributeCode().startsWith(CAP_CODE_PREFIX))
				continue;
		}

		// Since we are iterating through an array of modes to check, the above impl
		// will have returned false if any of them were missing
		return true;
	}

	private boolean shouldOverride() {
		// allow keycloak admin and devcs to do anything
		return (userToken.hasRole("admin", "dev") || ("service".equals(userToken.getUsername())));
	}

	public boolean hasCapabilityThroughRoles(String rawCapabilityCode, CapabilityMode mode) {
		if(shouldOverride())
			return true;

		final String cleanCapabilityCode = cleanCapabilityCode(rawCapabilityCode);
		BaseEntity user = beUtils.getUserBaseEntity();
		if(user == null) {
			log.error("Null user detected for token with uuid: " + userToken.getUuid());
			log.error("Token: " + userToken.getToken());
			return false;
		}
		JsonArray roles = jsonb.fromJson(user.getValueAsString(LNK_ROLE_CODE), JsonArray.class);

		BaseEntity currentRole;
		for(int i = 0; i < roles.size(); i++) {
			String roleCode = roles.getString(i);
			currentRole = beUtils.getBaseEntityByCode(roleCode);
			if(currentRole == null) {
				log.error("Could not find role when looking at PRI IS for base entity " + user.getCode() + ": " + roleCode);
				continue;
			}

			Optional<EntityAttribute> optEa = currentRole.findEntityAttribute(cleanCapabilityCode);
			if(!optEa.isPresent())
				continue;
			EntityAttribute ea = optEa.get();
			String modes = ea.getValueString();
			if(StringUtils.isBlank(modes)) {
				log.error("Dumb Capability detected! Role: " + roleCode + " has capability " + cleanCapabilityCode + " but not modes attached!");
				continue;
			}

			if(modes.contains(mode.name()))
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

		if(priIsAttributes.size() == 0) {
			log.error("Could not find any PRI_IS attributes for base entity: " + user.getCode());
			return false;
		}

		return priIsAttributes.stream().anyMatch((EntityAttribute priIsAttribute) -> {
			String priIsCode = priIsAttribute.getAttributeCode();
			String roleCode = ROLE_BE_PREFIX + priIsCode.substring(PRI_IS_PREFIX.length());
			log.debug("[!] Scanning Role: " + roleCode);
			BaseEntity roleBe = beUtils.getBaseEntityByCode(roleCode);
			if(roleBe == null) {
				return false;
			}

			String modeString = roleBe.getValueAsString(cleanCapabilityCode);
			if(StringUtils.isBlank(modeString))
				return false;
			return modeString.contains(mode.name());
		});
	}

	// TODO: Rewrite
	public void process() {
		String productCode = userToken.getProductCode();

		// Find all existing capabilities for a given product code
		// remove all capabilities in the manifest

		// Process the rest of the existing capabilities??
			// remove existing capability 
			// remove said capability from all roles that contain it
			// update roles in database and cache
		List<Attribute> existingCapability = dbUtils.findAttributesWithPrefix(productCode, CAP_CODE_PREFIX);

		/* Remove any capabilities not in this forced list from roles */
		existingCapability.removeAll(getCapabilityManifest());

		/*
		 * for every capability that exists that is not in the manifest , find all
		 * entityAttributes
		 */
		for (Attribute toBeRemovedCapability : existingCapability) {
			// Remove capability from db
			String attributeCode = toBeRemovedCapability.getCode();
			dbUtils.deleteAttribute(productCode, attributeCode);
			/* update all the roles that use this attribute by reloading them into cache */
			List<BaseEntity> cachedRoles = CacheUtils.getBaseEntitiesByPrefix(productCode, ROLE_BE_PREFIX);
			if(cachedRoles.size() > 0) {
				for (BaseEntity role : cachedRoles) {
					String roleCode = role.getCode();
					CacheUtils.removeEntry(productCode, getCacheKey(roleCode, attributeCode));
					role.removeAttribute(toBeRemovedCapability.getCode());
					/* Now update the db role to only have the attributes we want left */
					BaseEntityKey beKey = new BaseEntityKey(productCode, role.getCode());
					CacheUtils.saveEntity(productCode, beKey, role);
				}
			}
		}
	}

	/**
	 * @return the beUtils
	 */
	public BaseEntityUtils getBeUtils() {
		return beUtils;
	}

	/**
	 * @return the capabilityManifest
	 */
	public List<Attribute> getCapabilityManifest() {
		return capabilityManifest;
	}

	/**
	 * @param capabilityManifest the capabilityManifest to set
	 */
	public void setCapabilityManifest(List<Attribute> capabilityManifest) {
		this.capabilityManifest = capabilityManifest;
	}
/**
	 * @param condition the condition to check
	 * @return Boolean
	 */
	public Boolean conditionMet(String condition) {

		if (condition == null) {
			log.error("condition is NULL!");
			return false;
		}

		log.info("Testing condition with value: " + condition);
		String[] conditionArray = condition.split(":");

		String capability = conditionArray[0];
		String mode = conditionArray[1];

		// check for NOT operator
		Boolean not = capability.startsWith("!");
		capability = not ? capability.substring(1) : capability;

		// check for Capability
		Boolean hasCap = hasCapabilityThroughPriIs(capability, CapabilityMode.getMode(mode));

		// XNOR operator
		return hasCap ^ not;
	}
	
	@Override
	public String toString() {
		return "CapabilityUtils [" + (capabilityManifest != null ? "capabilityManifest=" + capabilityManifest : "")
				+ "]";
	}

	/**
	 * @param user User BaseEntity to generate alloweds for (TODO: Migrate this)
	 * @return a list of alloweds ready to be inserted into the kieSessions
	 */
	public List<Allowed> generateAlloweds(BaseEntity user) {
		String productCode = userToken.getProductCode();
		List<Allowed> allowables = new CopyOnWriteArrayList<Allowed>();

		// Look for user capabilities
		List<EntityAttribute> capabilities = user.findPrefixEntityAttributes(CAP_CODE_PREFIX);
		for (EntityAttribute capability : capabilities) {
			String modeString = capability.getValueString();
			if (modeString != null) {
				CapabilityMode[] modes = getCapModesFromString(modeString);
				String cleanCapabilityCode = cleanCapabilityCode(capability.getAttributeCode());
				allowables.add(new Allowed(cleanCapabilityCode, modes));
			}
		}

		Optional<EntityAttribute> LNK_ROLEOpt = user.findEntityAttribute(LNK_ROLE_CODE);

		JsonArray roleCodesArray = null;

		if (LNK_ROLEOpt.isPresent()) {
			roleCodesArray = jsonb.fromJson(LNK_ROLEOpt.get().getValueString(), JsonArray.class);
		} else {
			roleCodesArray = jsonb.fromJson("[]", JsonArray.class);
			log.info("Could not find " + LNK_ROLE_CODE + " in user: " + user.getCode());
		}

		// Add keycloak roles
		// for (String role : userToken.getUserRoles()) {
		// roleCodesArray.add(role);
		// }

		for (int i = 0; i < roleCodesArray.size(); i++) {
			String roleBECode = roleCodesArray.getString(i);

			BaseEntityKey beKey = new BaseEntityKey(productCode, roleBECode);
			BaseEntity roleBE = (BaseEntity) CacheUtils.getEntity(GennyConstants.CACHE_NAME_BASEENTITY, beKey);
			if (roleBE == null) {
				log.info("facts: could not find roleBe: " + roleBECode + " in cache: " + userToken.getProductCode());
				continue;
			}

			// Go through all the entity
			capabilities = roleBE.findPrefixEntityAttributes(CAP_CODE_PREFIX);
			for (EntityAttribute ea : capabilities) {
				String modeString = null;
				Boolean ignore = false;

				String cleanCapabilityCode = cleanCapabilityCode(ea.getAttributeCode());
				try {
					Object val = ea.getValue();
					if (val instanceof Boolean) {
						log.error("capability attributeCode=" + cleanCapabilityCode + " is BOOLEAN??????");
						ignore = true;
					} else {
						modeString = ea.getValue();
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (!ignore) {
					CapabilityMode[] modes = getCapModesFromString(modeString);
					allowables.add(new Allowed(cleanCapabilityCode, modes));
				}

			}
		}

		/* now force the keycloak ones */
		for (String role : userToken.getUserRoles()) {
			allowables.add(
					new Allowed(role.toUpperCase(), CapabilityMode.VIEW));
		}

		return allowables;
	}

	public static String getModeString(CapabilityMode... modes) {
		return CommonUtils.getArrayString(modes, (mode) -> mode.name());
	}

	public static CapabilityMode[] getCapModesFromString(String modeString) {
		JsonArray array = jsonb.fromJson(modeString, JsonArray.class);
		CapabilityMode[] modes = new CapabilityMode[array.size()];

		for (int i = 0; i < array.size(); i++) {
			modes[i] = CapabilityMode.valueOf(array.getString(i));
		}

		return modes;
	}

	public static String sanitizeRoleCode(final String rawRoleCode) {
		String cleanRoleCode = rawRoleCode.toUpperCase();
		if (!cleanRoleCode.startsWith(ROLE_BE_PREFIX)) {
			//TODO: CHECK FOR OTHER PREFIX

			cleanRoleCode = ROLE_BE_PREFIX + cleanRoleCode;
		}

		return cleanRoleCode;
	}

	public static String cleanCapabilityCode(final String rawCapabilityCode) {
		String cleanCapabilityCode = rawCapabilityCode.toUpperCase();
		if (!cleanCapabilityCode.startsWith(CAP_CODE_PREFIX)) {
			cleanCapabilityCode = CAP_CODE_PREFIX + cleanCapabilityCode;
		}

		String[] components = cleanCapabilityCode.split("_");
		// Should be of the form PRM_<OWN/OTHER>_<CODE>
		/*
		 * 1. PRM
		 * 2. OWN or OTHER
		 * 3. CODE
		 */
		if (components.length < 3) {
			log.warn("Capability Code: " + rawCapabilityCode + " missing OWN/OTHER declaration.");
		} else {
			Boolean affectsOwn = "OWN".equals(components[1]);
			Boolean affectsOther = "OTHER".equals(components[1]);

			if (!affectsOwn && !affectsOther) {
				log.warn("Capability Code: " + rawCapabilityCode + " has malformed OWN/OTHER declaration.");
			}
		}

		return cleanCapabilityCode;
	}

	/**
	 * Construct a cache key for fetching capabilities
	 * @param roleCode
	 * @param capCode
	 * @return
	 */
	private static String getCacheKey(String roleCode, String capCode) {
		return roleCode + ":" + capCode;
	}
}
