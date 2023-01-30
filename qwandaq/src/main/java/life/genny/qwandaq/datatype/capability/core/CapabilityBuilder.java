package life.genny.qwandaq.datatype.capability.core;

import static life.genny.qwandaq.datatype.capability.core.node.CapabilityMode.*;

import java.util.ArrayList;
import java.util.List;

import life.genny.qwandaq.datatype.capability.core.node.CapabilityMode;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.datatype.capability.core.node.PermissionMode;
import life.genny.qwandaq.managers.capabilities.CapabilitiesController;
import life.genny.qwandaq.managers.capabilities.RoleBuilder;

/**
 * Builder class to make adding capability nodes to Capabilities easier
 */
public class CapabilityBuilder {
    private final RoleBuilder roleBuilder;

    /**
     * Capability Nodes of the constructed capability
     */
    private List<CapabilityNode> nodes = new ArrayList<>();

    /**
     * Capability Code of the constructed Capability
     */
    private final String capabilityCode;

    /**
     * Create a new builder for a capability with the given code
     * @param capabilityCode
     * @return
     */
    public static CapabilityBuilder code(String capabilityCode) {
        return new CapabilityBuilder(capabilityCode);
    }

    /**
     * <p>Create a new Capability Builder for the given capabilityCode and link this to a given RoleBuilder</p>
     * <p>this constructor cleans the capability code as per {@link Engine#cleanCapabilityCode(String)}</p>
     * @param rb - parent {@link RoleBuilder}
     * @param capabilityCode - Capability Code of capability to create
     */
    public CapabilityBuilder(RoleBuilder rb, String capabilityCode) {
        this.roleBuilder = rb;
        this.capabilityCode = CapabilitiesController.cleanCapabilityCode(capabilityCode);
    }

    /**
     * <p>Create a new Capability Builder for the given capabilityCode and link this to a given RoleBuilder</p>
     * <p>this constructor cleans the capability code as per {@link Engine#cleanCapabilityCode(String)}</p>
     * @param rb - parent {@link RoleBuilder}
     * @param capabilityCode - Capability Code of capability to create
     */
    public CapabilityBuilder(String capabilityCode) {
        this(null, capabilityCode);
    }

    /**
     * Set the permission of this Capability with regards to the ADD mode
     * 
     * @param scope - permission level to set for adding
     * @param negate - whether or not to negate the add capability node for this capability (default false)
     * 
     * @see CapabilityMode#ADD
     */
    public CapabilityBuilder add(PermissionMode scope, boolean negate) {
        return addNode(ADD, scope, negate);
    }

    /**
     * Set the permission of this Capability with regards to the ADD mode
     * 
     * @param scope - permission level to set for adding
     * 
     * @see CapabilityMode#ADD
     */
    public CapabilityBuilder add(PermissionMode scope) {
        return add(scope, false);
    }

    /**
     * Set the permission of this Capability with regards to the EDIT mode
     * @param scope - permission level to set for editing
     * @param negate - whether or not to negate the edit capability node for this capability (default false)
     * 
     * @see CapabilityMode#EDIT
     */
    public CapabilityBuilder edit(PermissionMode scope, boolean negate) {
        return addNode(EDIT, scope, negate);
    }

    /**
     * Set the permission of this Capability with regards to the EDIT mode
     * 
     * @param scope - permission level to set for editing
     * 
     * @see CapabilityMode#EDIT
     */
    public CapabilityBuilder edit(PermissionMode scope) {
        return edit(scope, false);
    }

    /**
     * Set the permission of this Capability with regards to the DELETE mode
     * @param scope - permission level to set for deleting
     * @param negate - whether or not to negate the delete capability node for this capability (default false)
     * 
     * @see CapabilityMode#DELETE
     */
    public CapabilityBuilder delete(PermissionMode scope, boolean negate) {
        return addNode(DELETE, scope, negate);
    }

    /**
     * Set the permission of this Capability with regards to the DELETE mode
     * 
     * @param scope - permission level to set for deleting
     * 
     * @see CapabilityMode#DELETE
     */
    public CapabilityBuilder delete(PermissionMode scope) {
        return delete(scope, false);
    }

    /**
     * Set the permission of this Capability with regards to the VIEW mode
     * 
     * @param scope - permission level to set for viewing
     * @param negate - whether or not to negate the view capability node for this capability (default false)
     * 
     * @see CapabilityMode#VIEW
     */
    public CapabilityBuilder view(PermissionMode scope, boolean negate) {
        return addNode(VIEW, scope, negate);
    }

    /**
     * Set the permission of this Capability with regards to the VIEW mode
     * 
     * @param scope - permission level to set for viewing
     * 
     * @see CapabilityMode#VIEW
     */
    public CapabilityBuilder view(PermissionMode scope) {
        return view(scope, false);
    }

    /**
     * Add a new {@link CapabilityNode} to the given Capability
     * @param mode - mode to set (any of {@link CapabilityMode#values()})
     * @param scope - scope (permissions) to set for this capability node (any of {@link PermissionMode#values()})
     * @param negate - whether or not this {@link CapabilityNode} will negate
     * @return this
     */
    public CapabilityBuilder addNode(CapabilityMode mode, PermissionMode scope, boolean negate) {
        nodes.add(new CapabilityNode(mode, scope, negate));
        return this;
    }

    /**
     * Add a new {@link CapabilityNode} to the given Capability
     * @param modeIdentifier - identifier of mode to set (any of {@link CapabilityMode#idMap})
     * @param scopeIdentifier - identifier of scope (permissions) to set for this capability node (any of {@link PermissionMode#idMap})
     * @param negate - whether or not this {@link CapabilityNode} will negate
     * @return this
     */
    public CapabilityBuilder addNode(char modeIdentifier, char scopeIdentifier, boolean negate) {
        CapabilityMode mode = CapabilityMode.getByIdentifier(modeIdentifier);
        PermissionMode scope = PermissionMode.getByIdentifier(scopeIdentifier);
        return addNode(mode, scope, negate);
    }

    /**
     * Construct this capability and inject into the RoleBuilder this CapabilityBuilder is linked to
     * @return the RoleBuilder this is linked to
     * 
     * @see CapabilityBuilder#roleBuilder
     */
    public RoleBuilder build() {
        
        // TODO: add negate to rolebuilder
        if(roleBuilder == null)
            throw new UnsupportedOperationException("Cannot call build() on a CapabilityBuilder that was instantiated with new CapabilityBuilder(String code). Use .buildCap() instead");
        roleBuilder.getCapabilities().put(capabilityCode, nodes.toArray(new CapabilityNode[0]));
        return roleBuilder;
    }

    public Capability buildCap() {
        Capability c = new Capability(capabilityCode, nodes);
        return c;
    }
}