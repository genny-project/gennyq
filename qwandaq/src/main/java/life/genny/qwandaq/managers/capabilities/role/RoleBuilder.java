package life.genny.qwandaq.managers.capabilities.role;


import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.arc.Arc;

import org.jboss.logging.Logger;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.datatype.CapabilityMode;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.checked.RoleException;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.utils.CommonUtils;

public class RoleBuilder {
    static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());
    
    private CapabilitiesManager capManager;
    private RoleManager roleMan;

    private BaseEntity targetRole;
    
    private String productCode;

    private List<BaseEntity> inheritedRoles = new ArrayList<>();

    private List<String> childrenCodes = new ArrayList<>();

    private Map<String, CapabilityMode[]> roleCapabilities = new HashMap<>();

    private String redirectCode;

    // TODO: Again I want to get rid of product code chains like this
    // TODO: Hopefully we can firm up how product codes are assigned to tokens
    public RoleBuilder(String roleCode, String roleName, String productCode) {
        this.capManager = Arc.container().select(CapabilitiesManager.class).get();
        this.roleMan = capManager.getRoleManager();
        this.productCode = productCode;
        targetRole = roleMan.createRole(productCode, roleCode, roleName);
    }

    public RoleBuilder setRoleRedirect(String redirectCode) {
        this.redirectCode = redirectCode;
        return this;
    }

    public RoleBuilder inheritRole(BaseEntity... otherRoles) {
        inheritedRoles.addAll(Arrays.asList(otherRoles));
        return this;
    }

    public RoleBuilder addView(String capabilityCode) {
        return addCapability(capabilityCode, CapabilityMode.VIEW);
    }

    public RoleBuilder addCapability(String capabilityCode, CapabilityMode... capModes) {
        capabilityCode = CommonUtils.safeStripPrefix(capabilityCode);
        roleCapabilities.put(capabilityCode, capModes);
        return this;
    }

    public RoleBuilder addChildren(String... roleCodes) {
        this.childrenCodes.addAll(Arrays.asList(roleCodes));
        return this;
    }

    public BaseEntity build() throws RoleException {
        // Redirect
        roleMan.setRoleRedirect(productCode, targetRole, redirectCode);

        // Capabilities
        for(String capabilityCode : roleCapabilities.keySet()) {
            capManager.addCapabilityToBaseEntity(productCode, targetRole, fetch(capabilityCode), roleCapabilities.get(capabilityCode));
        }
        
        // Role inherits
        for(BaseEntity parentRole : this.inheritedRoles) {
            roleMan.inheritRole(productCode, targetRole, parentRole);
        }

        // Children
        roleMan.setChildren(productCode, targetRole, childrenCodes.toArray(new String[0]));

        return targetRole;
    }
}
