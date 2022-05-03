package life.genny.qwandaq.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.attribute.AttributeText;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.Allowed;
import life.genny.qwandaq.datatype.CapabilityMode;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.models.UserToken;

/*
 * A non-static utility class for managing roles and capabilities.
 * 
 * @author Adam Crow
 * @author Jasper Robison
 * @author Bryn Mecheam
 */
@RegisterForReflection
@ApplicationScoped
public class CapabilityUtils implements Serializable {

	static final Logger log = Logger.getLogger(CapabilityUtils.class);

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	UserToken userToken;

	@Inject
	BaseEntityUtils beUtils;

	List<Attribute> capabilityManifest = new ArrayList<Attribute>();

	public CapabilityUtils() { }

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
	 * @return String
	 */
	@Override
	public String toString() {
		return "CapabilityUtils [" + (capabilityManifest != null ? "capabilityManifest=" + capabilityManifest : "") + "]";
	}

	/**
	 * @param role       the role to add to
	 * @param parentRole the parentRole to inherit
	 * @return BaseEntity
	 */
	public BaseEntity inheritRole(BaseEntity role, final BaseEntity parentRole) {

		BaseEntity ret = role;
		List<EntityAttribute> perms = parentRole.findPrefixEntityAttributes("PRM_");
		for (EntityAttribute permissionEA : perms) {
			Attribute permission = permissionEA.getAttribute();
			CapabilityMode mode = CapabilityMode.getMode(permissionEA.getValue());
			ret = addCapabilityToRole(ret, permission.getCode(), mode);
		}
		return ret;
	}

	/**
	 * @param capabilityCode the capabilityCode to add to
	 * @param name           the name to add
	 * @return Attribute
	 */
	public Attribute addCapability(final String capabilityCode, final String name) {

		String fullCapabilityCode = "PRM_" + capabilityCode.toUpperCase();
		log.info("Setting Capability : " + fullCapabilityCode + " : " + name);
		Attribute attribute = qwandaUtils.getAttribute(fullCapabilityCode);

		if (attribute != null) {
			// add to manifest
			capabilityManifest.add(attribute);
		} else {
			// create new attribute and save it
			attribute = new AttributeText(fullCapabilityCode, name);
			databaseUtils.saveAttribute(attribute);
			capabilityManifest.add(attribute);
		}

		return attribute;
	}

	/**
	 * @param role           the role to add to
	 * @param capabilityCode the capabilityCode to add
	 * @param mode           the mode to add
	 * @return BaseEntity
	 */
	public BaseEntity addCapabilityToRole(BaseEntity role, final String capabilityCode, final CapabilityMode mode) {

		// check if the userToken is allowed to do this!
		if (!hasCapability(capabilityCode, mode)) {
			log.error(userToken.getUserCode() + " is NOT ALLOWED TO ADD THIS CAPABILITY TO A ROLE :"
					+ role.getCode());
			return role;
		}

		Answer answer = new Answer(userToken.getUserCode(), role.getCode(), "PRM_" + capabilityCode,
				mode.toString());

		String prefixedCode = capabilityCode;
		if (!capabilityCode.startsWith("PRM_")) {
			prefixedCode = "PRM_" + capabilityCode;
		}

		Attribute capabilityAttribute = qwandaUtils.getAttribute(prefixedCode);
		answer.setAttribute(capabilityAttribute);
		role = beUtils.saveAnswer(answer);

		// now update the list of roles associated with the key
		switch (mode) {
			case NONE:
				updateCachedRoleSet(role.getCode(), capabilityCode, CapabilityMode.NONE);
				break;
			case VIEW:
				updateCachedRoleSet(role.getCode(), capabilityCode, CapabilityMode.NONE);
				updateCachedRoleSet(role.getCode(), capabilityCode, CapabilityMode.VIEW);
				break;
			case EDIT:
				updateCachedRoleSet(role.getCode(), capabilityCode, CapabilityMode.NONE);
				updateCachedRoleSet(role.getCode(), capabilityCode, CapabilityMode.VIEW);
				updateCachedRoleSet(role.getCode(), capabilityCode, CapabilityMode.EDIT);
				break;
			case ADD:
				updateCachedRoleSet(role.getCode(), capabilityCode, CapabilityMode.NONE);
				updateCachedRoleSet(role.getCode(), capabilityCode, CapabilityMode.VIEW);
				updateCachedRoleSet(role.getCode(), capabilityCode, CapabilityMode.EDIT);
				updateCachedRoleSet(role.getCode(), capabilityCode, CapabilityMode.ADD);
				break;
			case DELETE:
			case SELF:
				updateCachedRoleSet(role.getCode(), capabilityCode, CapabilityMode.NONE);
				updateCachedRoleSet(role.getCode(), capabilityCode, CapabilityMode.VIEW);
				updateCachedRoleSet(role.getCode(), capabilityCode, CapabilityMode.EDIT);
				updateCachedRoleSet(role.getCode(), capabilityCode, CapabilityMode.ADD);
				updateCachedRoleSet(role.getCode(), capabilityCode, CapabilityMode.DELETE);
				break;
		}

		return role;
	}

	/**
	 * Update the cached role set for a capability.
	 *
	 * @param code           The code of the role of which to update the set.
	 * @param capabilityCode The code of the capability for the role set.
	 * @param mode           The mode of the roleset.
	 */
	private void updateCachedRoleSet(String code, String capabilityCode, CapabilityMode mode) {

		String productCode = userToken.getProductCode();

		String key = productCode + ":" + capabilityCode + ":" + mode.name();
		log.debug("Updating cached roleset: " + key);

		// fetch from cache and parse as arraylist
		String json = (String) CacheUtils.readCache(productCode, key);
		List<String> roleCodes = Arrays.asList(json.split(","));

		// add to role set if not already existing
		if (!roleCodes.contains(code)) {
			roleCodes.add(code);

			// replace all spaces and hard brackets
			json = roleCodes.toString().replaceAll("[ \\[\\]]", "");

			// write back to cache
			CacheUtils.writeCache(productCode, key, json);
		}
	}

	/**
	 * Checks if the user has a capability
	 *
	 * @param code The code of the capability.
	 * @param mode The mode of the capability.
	 * @return Boolean True if the user has the capability, False otherwise.
	 */
	public boolean hasCapability(String code, CapabilityMode mode) {

		String productCode = userToken.getProductCode();

		// allow keycloak admin and devcs to do anything
		if (userToken.hasRole("admin") || userToken.hasRole("dev") || ("service".equals(userToken.getUsername()))) {
			return true;
		}

		// create a capability code and mode combined unique key
		String key = productCode + ":" + code + ":" + mode.name();

		// fetch from cache and parse as arraylist
		String json = (String) CacheUtils.readCache(productCode, key);
		List<String> roleCodes = Arrays.asList(json.split(","));

		// fetch user baseentity
		BaseEntity user = beUtils.getUserBaseEntity();

		// check if the user has any of these roles
		for (String roleCode : roleCodes) {
			if (user.getBaseEntityAttributes().parallelStream()
					.anyMatch(item -> item.getAttributeCode().equals(roleCode))) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks if the user has a capability using any PRI_IS_ attributes.
	 *
	 * NOTE: This should be temporary until ROL_ attributes are properly in place
	 *
	 * @param code The code of the capability.
	 * @param mode The mode of the capability.
	 * @return Boolean True if the user has the capability, False otherwise.
	 */
	public boolean hasCapabilityThroughPriIs(String code, CapabilityMode mode) {

		String productCode = userToken.getProductCode();

		// allow keycloak admin and devcs to do anything
		if (userToken.hasRole("admin") || userToken.hasRole("dev") || ("service".equals(userToken.getUsername()))) {
			return true;
		}

		// create a capability code and mode combined unique key
		String key = productCode + ":" + code + ":" + mode.name();

		// fetch from cache and parse as arraylist
		String json = (String) CacheUtils.readCache(productCode, key);
		List<String> roleCodes = Arrays.asList(json.split(","));

		// fetch user baseentity
		BaseEntity user = beUtils.getUserBaseEntity();

		for (String roleCode : roleCodes) {
			String priIsCode = "PRI_IS_" + roleCode.split("ROL_")[1];
			if (user.getBaseEntityAttributes().parallelStream()
					.anyMatch(item -> item.getAttributeCode().equals(priIsCode))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Process capability attributes. This should remove any unused capabilities.
	 */
	public void process() {

		String productCode = userToken.getProductCode();
		List<Attribute> existingCapability = new ArrayList<Attribute>();

		// iterate attributes in memory
		for (String existingAttributeCode : qwandaUtils.attributes.get(productCode).keySet()) {
			if (existingAttributeCode.startsWith("PRM_")) {
				// fetch and add attribute to list
				Attribute attribute = qwandaUtils.getAttribute(existingAttributeCode);
				existingCapability.add(attribute);
			}
		}

		// Remove any capabilities not in this forced list from roles
		existingCapability.removeAll(getCapabilityManifest());

		// find all entityAttributes for each existing capability not in the manifest
		for (Attribute toBeRemovedCapability : existingCapability) {

			qwandaUtils.removeAttributeFromMemory(toBeRemovedCapability.getCode());
			databaseUtils.deleteAttribute(productCode, toBeRemovedCapability.getCode());

			// update all the roles that use this attribute by reloading them into cache
			QDataBaseEntityMessage msg = CacheUtils.getObject(productCode, "ROLES_" + productCode, QDataBaseEntityMessage.class);

			if (msg != null) {

				for (BaseEntity role : msg.getItems()) {
					// update the db role to only have the attributes we want left
					role.removeAttribute(toBeRemovedCapability.getCode());
					beUtils.updateBaseEntity(role);
				}
			}
		}
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

	/**
	 * Generate a list of {@link Allowed} objects for the user.
	 *
	 * @param userToken A {@link GennyToken}.
	 * @param user      The user {@link BaseEntity} to process.
	 * @return List A list of {@link Allowed} objects.
	 */
	public static List<Allowed> generateAlloweds(GennyToken userToken, BaseEntity user) {

		List<EntityAttribute> roles = user.findPrefixEntityAttributes("PRI_IS_");
		List<Allowed> allowable = new CopyOnWriteArrayList<Allowed>();

		// should store in cached map
		for (EntityAttribute role : roles) {

			Boolean value = false;
			if (role.getValue() instanceof Boolean) {
				value = role.getValue();
			} else if (role.getValue() instanceof String) {
				value = "TRUE".equalsIgnoreCase(role.getValue());
			}

			if (value) {

				String roleBeCode = "ROL_" + role.getAttributeCode().substring("PRI_IS_".length());
				BaseEntity roleBE = CacheUtils.getObject(userToken.getRealm(), roleBeCode, BaseEntity.class);
				if (roleBE == null) {
					continue;
				}

				// Add the actual role to capabilities
				allowable.add(new Allowed(role.getAttributeCode().substring("PRI_IS_".length()), CapabilityMode.VIEW));
				List<EntityAttribute> capabilities = roleBE.findPrefixEntityAttributes("PRM_");

				for (EntityAttribute ea : capabilities) {

					String modeString = null;
					Boolean ignore = false;

					try {
						Object val = ea.getValue();
						if (val instanceof Boolean) {
							log.error("capability attributeCode=" + ea.getAttributeCode() + " is BOOLEAN??????");
							ignore = true;
						} else {
							modeString = ea.getValue();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

					if (!ignore) {
						CapabilityMode mode = CapabilityMode.getMode(modeString);
						// This is my cunning switch statement that takes into consideration the
						// priority order of the modes... (note, no breaks and it relies upon the fall
						// through)
						switch (mode) {
							case DELETE:
								allowable.add(new Allowed(ea.getAttributeCode().substring(4), CapabilityMode.DELETE));
							case ADD:
								allowable.add(new Allowed(ea.getAttributeCode().substring(4), CapabilityMode.ADD));
							case EDIT:
								allowable.add(new Allowed(ea.getAttributeCode().substring(4), CapabilityMode.EDIT));
							case VIEW:
								allowable.add(new Allowed(ea.getAttributeCode().substring(4), CapabilityMode.VIEW));
							case NONE:
								allowable.add(new Allowed(ea.getAttributeCode().substring(4), CapabilityMode.NONE));
						}
					}

				}
			}
		}

		// now force the keycloak ones
		for (String role : userToken.getUserRoles()) {
			allowable.add(new Allowed(role.toUpperCase(), CapabilityMode.VIEW));
		}

		return allowable;
	}

	/**
	 * Recursively ensure the user has the capability to view each ask in a group
	 *
	 * @param ask The ask to traverse
	 */
	public void recursivelyCheckSidebarAskForCapability(Ask ask) {

		ArrayList<Ask> askList = new ArrayList<>();

		for (Ask childAsk : ask.getChildAsks()) {
			String code = "SIDEBAR_" + childAsk.getQuestionCode();

			if (hasCapabilityThroughPriIs(code, CapabilityMode.VIEW)) {

				recursivelyCheckSidebarAskForCapability(childAsk);
				askList.add(childAsk);
			}
		}

		Ask[] items = askList.toArray(new Ask[askList.size()]);
		ask.setChildAsks(items);
	}
}
