package life.genny.qwandaq.managers.capabilities.role;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.NoResultException;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.CapabilityMode;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.checked.RoleException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.managers.Manager;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.CommonUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static life.genny.qwandaq.constants.GennyConstants.DEF_ROLE_CODE;
import static life.genny.qwandaq.constants.GennyConstants.LNK_ROLE_CODE;
import static life.genny.qwandaq.constants.GennyConstants.CAP_CODE_PREFIX;
import static life.genny.qwandaq.constants.GennyConstants.ROLE_BE_PREFIX;

@ApplicationScoped
public class RoleManager extends Manager {
	protected static final Logger log = Logger.getLogger(RoleManager.class);

    private BaseEntity roleDef;
	private Attribute lnkRolAttribute;

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
		
		lnkRolAttribute = dbUtils.findAttributeByCode(userToken.getProductCode(), LNK_ROLE_CODE);
		if(lnkRolAttribute == null)
			throw new NullParameterException(LNK_ROLE_CODE);
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
			CapabilityMode[] modes = capManager.getCapModesFromString(permissionEA.getValue());
			ret = capManager.addCapabilityToBaseEntity(productCode, ret, permission.getCode(), modes);

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

		throw new RoleException(String.format("No redirect in roles %s", roles.toString()));
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
		List<String> roles = beUtils.getBaseEntityCodeArrayFromLinkAttribute(personBaseEntity, LNK_ROLE_CODE);

		if (roles == null || roles.isEmpty())
			throw new RoleException(String.format("No roles found for base entity: ", personBaseEntity.getCode()));
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
		if (!cleanRoleCode.startsWith(ROLE_BE_PREFIX)) {
			cleanRoleCode = ROLE_BE_PREFIX + cleanRoleCode;
		}

		return cleanRoleCode;
	}
    
}
