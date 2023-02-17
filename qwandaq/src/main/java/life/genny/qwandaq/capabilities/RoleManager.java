package life.genny.qwandaq.capabilities;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.datatype.DataType;

import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;

import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.Definition;
import life.genny.qwandaq.exception.checked.RoleException;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.DefUtils;
import life.genny.qwandaq.utils.QwandaUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@ApplicationScoped
public class RoleManager {

	@Inject
	UserToken userToken;

	@Inject
	DefUtils defUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	Logger log;

	@Inject
	QwandaUtils qwandaUtils;

	// Product-agnostic attributes (to access these attributes it is a requirement to specify the product code)
	private static AttributeProductDecorator lnkRoleAttribute;
	private static AttributeProductDecorator lnkChildAttribute = new AttributeProductDecorator(
		new Attribute(Attribute.LNK_CHILDREN, "Child Roles Link", new DataType(String.class))
	);

	@Inject
	CapabilitiesController controller;

	public RoleManager() {/* json constructor */}

	/**
	 * Search all product codes (if more than 1 exists) for the core attributes.
	 * If none are found, scream about it.
	 */
	@PostConstruct
	private void initDecorators() {
	}

	/**
	 * Initialize a single attribute product decorator by finding the relevant attribute in at least one
	 * of the loaded products
	 * @param decorator
	 * @param attributeCode
	 * @return
	 */
	private AttributeProductDecorator initDecorator(String attributeCode) {
		String[] productCodes = GennySettings.productCodes();
		for(int i = 0; i < productCodes.length; i++) {
			try {
				Attribute attribute = qwandaUtils.getAttribute(productCodes[i], attributeCode);
				// we found it without erroring out so we don't have to look through the rest
				return new AttributeProductDecorator(attribute);
			} catch(ItemNotFoundException e) {
				log.error("Core Attribute: LNK_ROLE is undefinied in the database. Please bootq or check the sheets to ensure it exists");
			}
		}
	}
    
	/**
	 * Attach a role to a person base entity
	 * @param target
	 * @param roleCode
	 * @return
	 */
	public BaseEntity attachRole(BaseEntity target, String roleCode) {
		if(target == null)
			throw new NullParameterException("person target when attaching role: " + roleCode);

		// Check we're working with a person
		if(!target.isPerson())
			throw new RoleException("Error attaching role to target: " + target.getCode() + ". Target is not a person");
		
		roleCode = cleanRoleCode(roleCode);
		BaseEntity role = beUtils.getBaseEntity(roleCode);

		if (role == null) 
			throw new RoleException("Error attaching role: " + roleCode + ". Role does not exist");

		return attachRole(target, role);
	}

	public BaseEntity attachRole(BaseEntity target, BaseEntity role) {
		Optional<EntityAttribute> eaOpt = target.findEntityAttribute(Attribute.LNK_ROLE);

		// Create it
		if(!eaOpt.isPresent()) {
			target.addAttribute(lnkRoleAttribute.get(target.getRealm()), 1.0, "[\"" + role.getCode() + "\"]");
			beUtils.updateBaseEntity(target);
			return target;
		}

		EntityAttribute lnkRoleEA = eaOpt.get();
		String value = lnkRoleEA.getValueString();
		value = CommonUtils.addToStringArray(value, role.getCode());
		target.addEntityAttribute(lnkRoleEA.getAttribute(), lnkRoleEA.getWeight(), false, value);
		beUtils.updateBaseEntity(target);
		return target;
	}
	
	public BaseEntity removeRole(BaseEntity target, BaseEntity role) {
		return removeRole(target, role.getCode());
	}

	public BaseEntity removeRole(BaseEntity target, String roleCode) {
		Optional<EntityAttribute> eaOpt = target.findEntityAttribute(Attribute.LNK_ROLE);

		// Create it
		if(!eaOpt.isPresent()) {
			log.warn("No roles found for target: " + target + " (Checked in " + Attribute.LNK_ROLE + ")");
			return target;
		}

		EntityAttribute lnkRoleEA = eaOpt.get();
		String value = lnkRoleEA.getValueString();
		value = CommonUtils.removeFromStringArray(value, roleCode);
		target.addEntityAttribute(lnkRoleEA.getAttribute(), lnkRoleEA.getWeight(), false, value);
		beUtils.updateBaseEntity(target);
		return target;
	}

	/**
	 * Create a new role under a given product code
	 * @param productCode
	 * @param roleCode
	 * @param roleName
	 * @return
	 */
	public BaseEntity createRole(String productCode, String roleCode, String roleName) {
		
		BaseEntity role = null;
		roleCode = cleanRoleCode(roleCode);
		try {
			log.debug("Found existing " + roleCode);
			role = beUtils.getBaseEntity(productCode, roleCode);
		} catch(ItemNotFoundException e) {
			log.debug("Previous role: " + roleCode + " not found. Generating");
			Definition defRole = defUtils.getDEF(Definition.DEF_ROLE);
			role = beUtils.create(defRole, roleName, roleCode);
		}
		
		return role;
	}

	/**
	 * Replace the current set of children for a given role with new children
	 * @param productCode - product code of the target role
	 * @param targetRole - role to replace LNK_CHILDREN value
	 * @param childrenCodes - child codes to replace with
	 * @return - updated role
	 */
	public BaseEntity setChildren(String productCode, BaseEntity targetRole, String... childrenCodes) {
		if (targetRole == null)
			throw new NullParameterException("targetRole");
		Optional<EntityAttribute> optChildren = targetRole.findEntityAttribute(lnkChildAttribute.get(productCode));

		String codeString = CommonUtils.getArrayString(childrenCodes);

		// add/edit LNK_CHILDREN
		if(!optChildren.isPresent()) {
			targetRole.addEntityAttribute(lnkChildAttribute.get(productCode), 0.0, false, codeString);
		} else {
			EntityAttribute childrenEA = optChildren.get();
			childrenEA.setValue(codeString);
		}

		// same persistence rules as addCapability
		if(controller.willPersist())
			beUtils.updateBaseEntity(targetRole);
		return targetRole;
	}

	/**
	 * add new children to the current set of children for a given role
	 * @param productCode - product code of the target role
	 * @param targetRole - role to add to LNK_CHILDREN value
	 * @param childrenCodes - child codes to add
	 * @return - updated role
	 */
	public BaseEntity addChildren(String productCode, BaseEntity targetRole, String... childrenCodes) {
		if(targetRole == null)
			throw new NullParameterException("targetRole");
		
		Optional<EntityAttribute> optChildren = targetRole.findEntityAttribute(Attribute.LNK_CHILDREN);

		// add/edit LNK_CHILDREN
		if(!optChildren.isPresent()) {
			String children = CommonUtils.addToStringArray(CommonUtils.STR_ARRAY_EMPTY, childrenCodes);
			targetRole.addEntityAttribute(lnkChildAttribute.get(productCode), 1.0, false, children);
		} else {
			EntityAttribute childrenEA = optChildren.get();
			String children = childrenEA.getValueString();
			children = CommonUtils.addToStringArray(children, childrenCodes);
			targetRole.addEntityAttribute(childrenEA, children);
		}

		beUtils.updateBaseEntity(targetRole);
		return targetRole;
	}

	/**
	 * Retrieve the children codes for a given role
	 * @param targetRole role base entity to target
	 */
	public List<String> getChildrenCodes(BaseEntity targetRole) {
		if (targetRole == null)
			throw new NullParameterException("targetRole");
		
		Optional<EntityAttribute> optChildren = targetRole.findEntityAttribute(Attribute.LNK_CHILDREN);
		if(!optChildren.isPresent()) {
			log.warn("No editable children found for: " + targetRole.getCode());
			return new ArrayList<String>();
		}
		String roleCodes = optChildren.get().getValueString();
		if(StringUtils.isBlank(roleCodes)) {
			return new ArrayList<String>();
		}
		
		roleCodes = CommonUtils.cleanUpAttributeValue(roleCodes);
		return Arrays.asList(roleCodes.split(","));
	}


	/**
	 * Retrieve the children for a given role
	 * @param targetRole role base entity to target
	 */
	public Set<BaseEntity> getChildren(BaseEntity targetRole) {
		return getChildrenCodes(targetRole).stream().map((String beCode) -> {
			BaseEntity be = beUtils.getBaseEntity(beCode);
			if(be == null) {
				log.error("Could not find Role: " + beCode);
			}

			return be;
		})
		.filter(be -> be != null).collect(Collectors.toSet());
	}

	/**
	 * Get the descendant codes of a target role
	 * @param role
	 * @return
	 * 
	 * @see {@link #getDescendants(BaseEntity)}
	 */
	public Map<String, Set<String>> getDescendantCodes(BaseEntity role) {
		Map<String, Set<String>> descendantCodeMap = new HashMap<>();
		Map<BaseEntity, Set<BaseEntity>> descendantMap = getDescendants(role);

		for(Entry<BaseEntity, Set<BaseEntity>> descendantRole : descendantMap.entrySet()) {
			descendantCodeMap.put(descendantRole.getKey().getCode(), descendantRole.getValue().stream()
																		.map(BaseEntity::getCode)
																		.collect(Collectors.toSet()));
		}

		return descendantCodeMap;
	}

	/**
	 * Get all of the children and descendants for a given role, where a descendant is any role X levels below the target role
	 * @param role role to get descendants of
	 * @return a map going from role to children of that role (as a set)
	 */
	public Map<BaseEntity, Set<BaseEntity>> getDescendants(BaseEntity role) {
		Map<BaseEntity, Set<BaseEntity>> descendantMap = new HashMap<>();
		return getDescendants(role, descendantMap);
	}

	private Map<BaseEntity, Set<BaseEntity>> getDescendants(BaseEntity role, Map<BaseEntity, Set<BaseEntity>> descendantMap) {
		Set<BaseEntity> children = getChildren(role);
		if(children.isEmpty()) {
			return descendantMap;
		}

		// add children
		descendantMap.put(role, children);
		for(BaseEntity child : children) {
			Set<BaseEntity> childChildren = getChildren(child);
			if(!childChildren.isEmpty())
				descendantMap = getDescendants(role, descendantMap);
		}

		return descendantMap;
	}

	/**
	 * Have one role inherit another
	 * @param productCode - productCode that target role is defined in
	 * @param role - target role
	 * @param parentRole - role to inherit
	 * @return - new target role
	 */
	public BaseEntity inheritRole(String productCode, BaseEntity role, final BaseEntity parentRole) {
		BaseEntity ret = role;
		List<EntityAttribute> perms = parentRole.findPrefixEntityAttributes(Prefix.CAP_);

		for (EntityAttribute permissionEA : perms) {
			Attribute permission = permissionEA.getAttribute();
			List<CapabilityNode> capabilities = CapabilitiesController.deserializeCapArray(permissionEA.getValue());
			ret = controller.addCapability(productCode, ret, permission.getCode(), capabilities);

			// Same persistence rules as CapabilitiesController#addCapability
			if(controller.willPersist())
				beUtils.updateBaseEntity(ret);
		}

		return ret;
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
		List<String> roles = getRoleCodes(user);

		// TODO: return redirect for roles based on priority
		for (String role : roles) {
			try {
				// return first found redirect
				return getRoleRedirectCode(role);
			} catch (RoleException e) {
				log.debug(e.getMessage());
			}
		}

		throw new RoleException(String.format("No redirect in roles %s", roles));
	}

	public String getRoleRedirectCode(String roleCode) throws RoleException {
		return getRoleRedirectCode(userToken.getProductCode(), roleCode);
	}

	/**
	 * Get the redirect code for a role.
	 * 
	 * @param roleCode The code of the role
	 * @return The redirect code
	 * @throws RoleException If no redirect is found for the role
	 */
	public String getRoleRedirectCode(String productCode, String roleCode) throws RoleException {

		if (roleCode == null)
			throw new NullParameterException(roleCode);

		String key = roleCode.concat(":REDIRECT");

		// TODO: grab redirect for role
		String redirectCode = CacheUtils.getObject(productCode, key, String.class);

		if (redirectCode == null)
			throw new RoleException("No redirect found in role ".concat(roleCode));

		return redirectCode;
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
	
	public List<String> getRoleCodes(BaseEntity personBaseEntity) {
		List<String> roles = beUtils.getBaseEntityCodeArrayFromLinkAttribute(personBaseEntity, Attribute.LNK_ROLE);

		if (roles == null || roles.isEmpty())
			return new ArrayList<>();
		return roles;
	}

	public List<BaseEntity> getRoles(BaseEntity personBaseEntity) {
		List<String> roles = getRoleCodes(personBaseEntity);
		
		return roles.stream().map((String roleCode) -> {
			BaseEntity be = beUtils.getBaseEntity(roleCode);
			if(be == null) {
				log.error("Could not find role: " + roleCode);
			}
			return be;
		}).filter((BaseEntity roleBe) -> (roleBe != null)).collect(Collectors.toList());
	}

	public static String cleanRoleCode(final String rawRoleCode) {
		String cleanRoleCode = rawRoleCode.toUpperCase();
		if (!cleanRoleCode.startsWith(Prefix.ROL_)) {
			cleanRoleCode = Prefix.ROL_ + cleanRoleCode;
		}

		return cleanRoleCode;
	}
    
}
