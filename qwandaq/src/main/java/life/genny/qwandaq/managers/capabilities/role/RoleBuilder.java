package life.genny.qwandaq.managers.capabilities.role;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.CapabilityBuilder;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.checked.RoleException;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;

public class RoleBuilder {
    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());
    
    private final CapabilitiesManager capManager;
    private final RoleManager roleMan;

    private final BaseEntity targetRole;
    
    private final String productCode;

    private final List<BaseEntity> inheritedRoles = new ArrayList<>();

    private final List<String> childrenCodes = new ArrayList<>();

    /**
     * A map from Attribute (Capability) Code to Attribute (Capability)
     */
    private Map<String, Attribute> capabilityMap;

    /**
     * A map from Capability Code to Capabilities to add to the role for that Capability
     */
    private Map<String, Capability> roleCapabilities = new HashMap<>();

    private String redirectCode;

    public RoleBuilder(String roleCode, String roleName, String productCode) {
        this.capManager = Arc.container().select(CapabilitiesManager.class).get();
        this.roleMan = capManager.getRoleManager();
        this.productCode = productCode;
        targetRole = roleMan.createRole(productCode, roleCode, roleName, true);
    }

    /**
     * Set the {@link RoleBuilder#capabilityMap}
     * @param capData a 2D String array, where each element of the first array is of the form {Code, Name}
     * Example:
     * <pre>
     *{
     *  {"CAP_ADMIN", "Manipulate Admin"},
     *  {"CAP_TENANT", "Manipulate Tenant"}
     *}
     * </pre>
     * @return this RoleBuilder
     * 
     * @see {@link CapabilitiesManager#getCapabilityAttributeMap(String, String[][])}
     */
    public RoleBuilder setCapabilityMap(String[][] capData) {
        this.capabilityMap = capManager.getCapabilityAttributeMap(productCode, capData);
        return this;
    }

    /**
     * Set the {@link RoleBuilder#capabilityMap}
     * @param capabilityMap - the map generated by {@link CapabilitiesManager#getCapabilityMap}
     * @return this RoleBuilder
     * 
     * @see {@link CapabilitiesManager#getCapabilityAttributeMap(String, String[][])}
     */
    public RoleBuilder setCapabilityMap(Map<String, Attribute> capabilityMap) {
        this.capabilityMap = capabilityMap;
        return this;
    }

    public RoleBuilder setRoleRedirect(String redirectCode) {
        this.redirectCode = redirectCode;
        return this;
    }

    /**
     * Add one or more roles for this role to inherit
     * @param otherRoles - other role base entities
     * @return
     */
    public RoleBuilder inheritRole(BaseEntity... otherRoles) {
        inheritedRoles.addAll(Arrays.asList(otherRoles));
        return this;
    }

    /**
     * Start creating a capability and add it when it is finished building
     * @param capabilityCode
     * @return
     */
    public CapabilityBuilder addCapability(String capabilityCode) {
        fetch(capabilityCode);
        return new CapabilityBuilder(this, capabilityCode);
    }

    public RoleBuilder addChildren(String... roleCodes) {
        this.childrenCodes.addAll(Arrays.asList(roleCodes));
        return this;
    }

    public Map<String, Capability> getCapabilities() {
        return roleCapabilities;
    }

    public BaseEntity build() throws RoleException {
        if(capabilityMap == null) {
            throw new RoleException("Capability Map not set. Try using setCapabilityMap(Map<String, Attribute> capabilityMap) before building.");
        }

        // Redirect
        roleMan.setRoleRedirect(productCode, targetRole, redirectCode);

        // Capabilities
        for(String capabilityCode : roleCapabilities.keySet()) {
            capManager.addCapabilityToBaseEntity(targetRole, roleCapabilities.get(capabilityCode));
        }
        
        // Role inherits
        for(BaseEntity parentRole : this.inheritedRoles) {
            roleMan.inheritRole(targetRole, parentRole);
        }

        // Children
        roleMan.setChildren(productCode, targetRole, childrenCodes.toArray(new String[0]));

        return targetRole;
    }

	private Attribute fetch(String attrCode) throws ItemNotFoundException {
        attrCode = CapabilitiesManager.cleanCapabilityCode(attrCode);
		Attribute attribute = capabilityMap.get(attrCode);
		if(attribute == null) {
			log.error("Could not find capability in map: " + attrCode);
			throw new ItemNotFoundException("capability map", "capability", attrCode);
		}
		return attribute;
	}
}
