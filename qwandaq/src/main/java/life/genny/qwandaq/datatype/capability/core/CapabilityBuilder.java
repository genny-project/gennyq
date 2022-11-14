package life.genny.qwandaq.datatype.capability.core;

import static life.genny.qwandaq.datatype.capability.core.node.CapabilityMode.*;

import java.util.ArrayList;
import java.util.List;

import life.genny.qwandaq.datatype.capability.core.node.CapabilityMode;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.datatype.capability.core.node.PermissionMode;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.managers.capabilities.role.RoleBuilder;

/**
 * Builder class to make adding capability nodes to Capabilities easier
 */
public class CapabilityBuilder {
    private final RoleBuilder roleBuilder;

    /**
     * Capability Nodes of the constructed capability
     */
    List<CapabilityNode> nodes = new ArrayList<>();

    /**
     * Capability Code of the constructed Capability
     */
    final String capabilityCode;

    /**
     * <p>Create a new Capability Builder for the given capabilityCode and link this to a given RoleBuilder</p>
     * <p>this constructor cleans the capability code as per {@link CapabilitiesManager#cleanCapabilityCode(String)}</p>
     * @param rb - parent {@link RoleBuilder}
     * @param capabilityCode - Capability Code of capability to create
     */
    public CapabilityBuilder(RoleBuilder rb, String capabilityCode) {
        this.roleBuilder = rb;
        this.capabilityCode = CapabilitiesManager.cleanCapabilityCode(capabilityCode);
    }

    /**
     * <p>Create a new Capability Builder for the given capabilityCode and link this to a given RoleBuilder</p>
     * <p>this constructor cleans the capability code as per {@link CapabilitiesManager#cleanCapabilityCode(String)}</p>
     * @param rb - parent {@link RoleBuilder}
     * @param capabilityCode - Capability Code of capability to create
     */
    public CapabilityBuilder(String capabilityCode) {
        this(null, capabilityCode);
    }

    /**
     * Set multiple CapabilityModes to the same scope
     * @param scope - scope to set the modes to
     * @param modes - modes to set
     */
    public CapabilityBuilder setModes(PermissionMode scope, CapabilityMode... modes) {
        for(CapabilityMode mode : modes) {
            addNode(mode, scope);
        }
        return this;
    }

    /**
     * Set the permission of this Capability with regards to the ADD mode
     * 
     * @see CapabilityMode#ADD
     */
    public CapabilityBuilder add(PermissionMode scope) {
        return addNode(ADD, scope);
    }

    /**
     * Set the permission of this Capability with regards to the EDIT mode
     * 
     * @see CapabilityMode#EDIT
     */
    public CapabilityBuilder edit(PermissionMode scope) {
        return addNode(EDIT, scope);
    }

    /**
     * Set the permission of this Capability with regards to the DELETE mode
     * 
     * @see CapabilityMode#DELETE
     */
    public CapabilityBuilder delete(PermissionMode scope) {
        return addNode(DELETE, scope);
    }

    /**
     * Set the permission of this Capability with regards to the VIEW mode
     * 
     * @see CapabilityMode#VIEW
     */
    public CapabilityBuilder view(PermissionMode scope) {
        return addNode(VIEW, scope);
    }

    /**
     * Add a new {@link CapabilityNode} to the given Capability
     * @param mode - mode to set (any of {@link CapabilityMode#values()})
     * @param scope - scope (permissions) to set for this capability node (any of {@link PermissionMode#values()})
     * @return this
     */
    public CapabilityBuilder addNode(CapabilityMode mode, PermissionMode scope) {
        nodes.add(CapabilityNode.get(mode, scope));
        return this;
    }

    /**
     * Add a new {@link CapabilityNode} to the given Capability
     * @param modeString - name of mode to set (any of {@link CapabilityMode#values()})
     * @param scopeString - name of scope (permissions) to set for this capability node (any of {@link PermissionMode#values()})
     * @return this
     */
    public CapabilityBuilder addNode(String modeString, String scopeString) {
        CapabilityMode mode = CapabilityMode.valueOf(modeString.toUpperCase());
        PermissionMode scope = PermissionMode.valueOf(scopeString.toUpperCase());
        return addNode(mode, scope);
    }

    /**
     * Add a new {@link CapabilityNode} to the given Capability
     * @param modeIdentifier - identifier of mode to set (any of {@link CapabilityMode#idMap})
     * @param scopeIdentifier - identifier of scope (permissions) to set for this capability node (any of {@link PermissionMode#idMap})
     * @return this
     */
    public CapabilityBuilder addNode(char modeIdentifier, char scopeIdentifier) {
        CapabilityMode mode = CapabilityMode.getByIdentifier(modeIdentifier);
        PermissionMode scope = PermissionMode.getByIdentifier(scopeIdentifier);
        return addNode(mode, scope);
    }

    /**
     * Construct this capability and inject into the RoleBuilder this CapabilityBuilder is linked to
     * @return the RoleBuilder this is linked to
     * 
     * @see CapabilityBuilder#roleBuilder
     */
    public RoleBuilder build() {
        
        if(roleBuilder == null)
            throw new UnsupportedOperationException("Cannot call build() on a CapabilityBuilder that was instantiated with new CapabilityBuilder(String code). Use .buildCap() instead");
        roleBuilder.getCapabilities().put(capabilityCode, nodes.toArray(new CapabilityNode[0]));
        return roleBuilder;
    }

    public Capability buildCap() {
        return new Capability(capabilityCode, nodes);
    }
}