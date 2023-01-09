package life.genny.qwandaq.managers.capabilities.role;

import static life.genny.qwandaq.constants.GennyConstants.DEF_ROLE_CODE;

import java.util.ArrayList;
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
import javax.persistence.NoResultException;

import org.apache.commons.lang3.StringUtils;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.checked.RoleException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.managers.Manager;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.utils.CommonUtils;

@ApplicationScoped
public class RoleManager extends Manager {

	public final static DataType dtt = new DataType(String.class);

    private BaseEntity roleDef;
	private Attribute lnkRolAttribute;
	private Attribute lnkChildrenAttribute;

	@Inject
	CacheManager cm;

	@Inject
	CapabilitiesManager capManager;

	public RoleManager() {}
    
	@Override
	@PostConstruct
	protected void init() {
		super.init();
		// Should only need to find this once.
		roleDef = beUtils.getBaseEntity(DEF_ROLE_CODE);
		if(roleDef == null)
			throw new NullParameterException(DEF_ROLE_CODE);
		
		lnkRolAttribute = cm.getAttribute(userToken.getProductCode(), Attribute.LNK_ROLE);
		if(lnkRolAttribute == null) {
			error(Attribute.LNK_ROLE + " is missing. Adding!");
			lnkRolAttribute = new Attribute(Attribute.LNK_ROLE, "Role Link", dtt);
			qwandaUtils.saveAttribute(lnkRolAttribute);
		}
		try {
			lnkChildrenAttribute = cm.getAttribute(userToken.getProductCode(), Attribute.LNK_CHILDREN);
		} catch(NoResultException e) {
			error(Attribute.LNK_CHILDREN + " is missing. Adding!");
			lnkChildrenAttribute = new Attribute(Attribute.LNK_CHILDREN, "Children Role Link", dtt);
			cm.saveAttribute(lnkChildrenAttribute);
		}
	}

	/**
	 * Attach a role to a person base entity
	 * @param target
	 * @param roleCode
	 * @return
	 */
	public BaseEntity attachRole(BaseEntity target, String roleCode) {
		
		// Check we're working with a person
		if(target == null || !target.isPerson())
			throw new RoleException("Error attaching role to target: " + target.getCode() + ". Target is not a person");
		
		roleCode = cleanRoleCode(roleCode);
		BaseEntity role = beUtils.getBaseEntity(roleCode);

		if(role == null) 
			throw new RoleException("Error attaching role: " + roleCode + ". Role does not exist");

		return attachRole(target, role);
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
			role = dbUtils.findBaseEntityByCode(productCode, roleCode);
		} catch(NoResultException e) {
			role = new BaseEntity(roleCode, roleName);
			role.setRealm(productCode);
			beUtils.updateBaseEntity(role);
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
		if(targetRole == null)
			throw new NullParameterException("targetRole");
		Optional<EntityAttribute> optChildren = targetRole.findEntityAttribute(lnkChildrenAttribute);

		String codeString = CommonUtils.getArrayString(childrenCodes);

		// add/edit LNK_CHILDREN
		if(!optChildren.isPresent()) {
			targetRole.addAttribute(lnkChildrenAttribute, 1.0, codeString);
		} else {
			EntityAttribute childrenEA = optChildren.get();
			childrenEA.setValue(codeString);
		}

		// TODO: Keep an eye on this becasue it may have just broken
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
		Optional<EntityAttribute> optChildren = targetRole.findEntityAttribute(lnkChildrenAttribute);

		EntityAttribute childrenEA;
		List<String> childrenCodeList = Arrays.asList(childrenCodes);
		// add/edit LNK_CHILDREN
		if(!optChildren.isPresent()) {
			childrenEA = targetRole.addAttribute(lnkChildrenAttribute, 1.0);
		} else {
			childrenEA = optChildren.get();
			String[] preexistingChildren = beUtils.cleanUpAttributeValue(childrenEA.getValueString()).split(",");
			childrenCodeList.addAll(Arrays.asList(preexistingChildren));
		}

		String codeString = CommonUtils.getArrayString(childrenCodeList);
		childrenEA.setValue(codeString);

		// TODO: Keep an eye on this becasue it may have just broken
		beUtils.updateBaseEntity(targetRole);
		return targetRole;
	}

	/**
	 * Retrieve the children codes for a given role
	 * @param targetRole role base entity to target
	 */
	public List<String> getChildrenCodes(BaseEntity targetRole) {
		if(targetRole == null)
			throw new NullParameterException("targetRole");
		
		Optional<EntityAttribute> optChildren = targetRole.findEntityAttribute(lnkChildrenAttribute);
		if(!optChildren.isPresent()) {
			warn("No editable children found for: " + targetRole.getCode());
			return new ArrayList<String>();
		}
		String roleCodes = optChildren.get().getValueString();
		if(StringUtils.isBlank(roleCodes)) {
			return new ArrayList<String>();
		}
		
		roleCodes = beUtils.cleanUpAttributeValue(roleCodes);
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
				error("Could not find Role: " + beCode);
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

		for(BaseEntity descendantRole : descendantMap.keySet()) {
			descendantCodeMap.put(descendantRole.getCode(), 
				descendantMap.get(descendantRole).stream()
								.map((BaseEntity child) -> child.getCode())
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
		List<EntityAttribute> perms = parentRole.findPrefixEntityAttributes(Prefix.CAP);
		for (EntityAttribute permissionEA : perms) {
			Attribute permission = permissionEA.getAttribute();
			List<CapabilityNode> capabilities = CapabilitiesManager.deserializeCapArray(permissionEA.getValue());
			ret = capManager.addCapabilityToBaseEntity(productCode, ret, permission.getCode(), capabilities);

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
				debug(e.getMessage());
			}
		}

		throw new RoleException(String.format("No redirect in roles %s", roles));
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
		String redirectCode = cm.getObject(product, key, String.class);

		if (redirectCode == null)
			throw new RoleException("No redirect found in role ".concat(roleCode));

		return redirectCode;
	}

	public BaseEntity attachRole(BaseEntity target, BaseEntity role) {
		Optional<EntityAttribute> eaOpt = target.findEntityAttribute(Attribute.LNK_ROLE);

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
			value = "[\"" + role.getCode() + "\"]";
		}

		lnkRoleEA.setValue(value);

		beUtils.updateBaseEntity(target);
		return target;
	}

	/**
	 * Set the redirect code for a role.
	 * 
	 * @param productCode  The product code
	 * @param role         The role to set the redirect for
	 * @param redirectCode The code to set as redirect
	 */
	public void setRoleRedirect(String productCode, BaseEntity role, String redirectCode) {
		cm.putObject(productCode, String.format("%s:REDIRECT", role.getCode()), redirectCode);
	}
	
	public List<String> getRoleCodes(BaseEntity personBaseEntity) {
		List<String> roles = beUtils.getBaseEntityCodeArrayFromLinkAttribute(personBaseEntity, Attribute.LNK_ROLE);

		if (roles == null || roles.isEmpty())
			return new ArrayList<String>();// throw new RoleException(String.format("No roles found for base entity: ", personBaseEntity.getCode()));
		return roles;
	}

	public List<BaseEntity> getRoles(BaseEntity personBaseEntity) {
		List<String> roles = getRoleCodes(personBaseEntity);
		
		return roles.stream().map((String roleCode) -> {
			BaseEntity be = beUtils.getBaseEntity(roleCode);
			if(be == null) {
				error("Could not find role: " + roleCode);
			}
			return be;
		}).filter((BaseEntity roleBe) -> (roleBe != null)).collect(Collectors.toList());
	}

	public static String cleanRoleCode(final String rawRoleCode) {
		String cleanRoleCode = rawRoleCode.toUpperCase();
		if (!cleanRoleCode.startsWith(Prefix.ROL)) {
			cleanRoleCode = Prefix.ROL + cleanRoleCode;
		}

		return cleanRoleCode;
	}
    
}
