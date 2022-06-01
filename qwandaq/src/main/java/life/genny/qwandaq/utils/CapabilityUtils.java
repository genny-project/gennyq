package life.genny.qwandaq.utils;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import io.vertx.core.json.DecodeException;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.AttributeText;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.datatype.Allowed;
import life.genny.qwandaq.datatype.CapabilityMode;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.serialization.baseentity.BaseEntityKey;

/*
 * A non-static utility class for managing roles and capabilities.
 * 
 * @author Adam Crow
 * @author Jasper Robison
 * @author Bryn Mecheam
 */
@ApplicationScoped
public class CapabilityUtils {
	protected static final Logger log = Logger.getLogger(CapabilityUtils.class);
	
	static Jsonb jsonb = JsonbBuilder.create();

	// Capability Attribute Prefix
	public static final String CAP_CODE_PREFIX = "PRM_";
	public static final String ROLE_BE_PREFIX = "ROL_";

	// TODO: Confirm we want DEFs to have capabilities as well
	public static final String[] ACCEPTED_CAP_PREFIXES = { ROLE_BE_PREFIX, "PER_", "DEF_" };

	public static final String LNK_ROLE_CODE = "LNK_ROLE";

	List<Attribute> capabilityManifest = new ArrayList<Attribute>();

	@Inject
	UserToken userToken;

	@Inject
	ServiceToken serviceToken;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	BaseEntityUtils beUtils;

	public CapabilityUtils() {
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
	 * @param capabilityCode capabilityCode to check against
	 * @param modes          array of modes to check against
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
		// Set<EntityAttribute> entityAttributes = user.getBaseEntityAttributes();
		// for(EntityAttribute eAttribute : entityAttributes) {
		// if(!eAttribute.getAttributeCode().startsWith(CAP_CODE_PREFIX))
		// continue;
		// }

		// Since we are iterating through an array of modes to check, the above impl
		// will have returned false if any of them were missing
		return true;
	}

	// TODO: Rewrite
	public void process() {
		List<Attribute> existingCapability = new ArrayList<Attribute>();

		String productCode = userToken.getProductCode();

		for (String existingAttributeCode : RulesUtils.realmAttributeMap.get(productCode)
				.keySet()) {
			if (existingAttributeCode.startsWith(CAP_CODE_PREFIX)) {
				existingCapability.add(RulesUtils.realmAttributeMap.get(productCode)
						.get(existingAttributeCode));
			}
		}

		/* Remove any capabilities not in this forced list from roles */
		existingCapability.removeAll(getCapabilityManifest());

		/*
		 * for every capability that exists that is not in the manifest , find all
		 * entityAttributes
		 */
		for (Attribute toBeRemovedCapability : existingCapability) {
			try {
				RulesUtils.realmAttributeMap.get(productCode)
						.remove(toBeRemovedCapability.getCode()); // remove from cache
				if (!VertxUtils.cachedEnabled) { // only post if not in junit
					QwandaUtils.apiDelete(GennySettings.qwandaServiceUrl + "/qwanda/baseentitys/attributes/"
							+ toBeRemovedCapability.getCode(), serviceToken.getToken());
				}
				/* update all the roles that use this attribute by reloading them into cache */
				QDataBaseEntityMessage rolesMsg = VertxUtils.getObject(productCode, "ROLES",
						productCode, QDataBaseEntityMessage.class);
				if (rolesMsg != null) {

					for (BaseEntity role : rolesMsg.getItems()) {
						role.removeAttribute(toBeRemovedCapability.getCode());
						/* Now update the db role to only have the attributes we want left */
						if (!VertxUtils.cachedEnabled) { // only post if not in junit
							QwandaUtils.apiPutEntity(GennySettings.qwandaServiceUrl + "/qwanda/baseentitys/force",
									JsonUtils.toJson(role), productCode);
						}

					}
				}

			} catch (IOException e) {
				/* TODO Auto-generated catch block */
				e.printStackTrace();
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

	@Override
	public String toString() {
		return "CapabilityUtils [" + (capabilityManifest != null ? "capabilityManifest=" + capabilityManifest : "")
				+ "]";
	}

	/**
	 * @param userToken
	 * @param user
	 * @return
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
		String modeString = "[";
		for (CapabilityMode mode : modes) {
			modeString += "\"" + mode.name() + "\"" + ",";
		}

		return modeString.substring(0, modeString.length() - 1) + "]";
	}

	public static CapabilityMode[] getCapModesFromString(String modeString) {
		JsonArray array = jsonb.fromJson(modeString, JsonArray.class);
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

	private static String getCacheKey(String keyCode, String capCode) {
		return keyCode + ":" + capCode;
	}
}
