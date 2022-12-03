package life.genny.qwandaq.datatype.capability.core;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.json.bind.annotation.JsonbTypeAdapter;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.serialization.adapters.CapabilityAdapter;
import life.genny.qwandaq.utils.CommonUtils;

/**
 * A lightweight class meant to purely store deserialized capabilities and the respective capabilitycode
 * Can be derived from an EntityAttribute
 * 
 * @author Bryn Meachem
 */
@JsonbTypeAdapter(CapabilityAdapter.class)
@RegisterForReflection
public class Capability implements Serializable {
    
    public String code;

    public Set<CapabilityNode> nodes;

    public Capability() {
        code="JSON-CONSTRUCTED";
        nodes=new HashSet<>();
    }

    public Capability(String capabilityCode, Set<CapabilityNode> nodes) {
        this.code = CapabilitiesManager.cleanCapabilityCode(capabilityCode);
        this.nodes = nodes;
    }

    public Capability(String capabilityCode, String capNodes) {
        this(capabilityCode, CapabilitiesManager.deserializeCapArray(capNodes));
    }

    public Capability(String capabilityCode, CapabilityNode... nodes) {
        this(capabilityCode, new LinkedHashSet<>(Arrays.asList(nodes)));
    }

    public Capability(String capabilityCode, List<CapabilityNode> nodes) {
        this(capabilityCode, new LinkedHashSet<>(nodes));
    }

    /**
     * Merge this Capability with another, creating a new Capability that
     * is either the most or least permissive given the pool of permission nodes
     * <p>Will return the value of the caller if the two Capability codes are different</p>
     * 
     * @param other
     * @param mostPermissive
     * @return
     */
    public Capability merge(Capability other, boolean mostPermissive) {
        if(!this.code.equals(other.code))
            return this;

        Set<CapabilityNode> newNodes = new HashSet<>();
        // this handles all 3 cases (this is empty, other is empty and both are empty)
        if(other.nodes.isEmpty()) {
            return this;
        }
        if(this.nodes.isEmpty()) {
            return other;
        }
       
        // For each node, find the most permissive nodes
        for(CapabilityNode node : this.nodes) {
            for(CapabilityNode otherNode : other.nodes) {
                if(node.capMode.equals(otherNode.capMode)) {
                    newNodes.add(node.compareNodes(otherNode, mostPermissive));
                } else {
                    newNodes.add(otherNode);
                    newNodes.add(node);
                }
            }
        }

        return new Capability(this.code, newNodes);
    }

    /**
     * Check if this capability already belongs in a specified set of capabilities (code-based equality).
     * If it exists then return that capability object
     * @param capabilities - the set of capabilities to check
     * @return the capability object that shares the same capability code, or null if none could be found (returns the first instance)
     */
    public Capability hasCodeInSet(Set<Capability> capabilities) {
        for(Capability c : capabilities) {
            if(c.code.equals(this.code)) {
                return c;
            }
        }

        return null;
    }

    public static Capability getFromEA(EntityAttribute ea) {
        String capCode = ea.getAttributeCode();
        List<CapabilityNode> caps = CapabilitiesManager.deserializeCapArray(ea.getValueString());
        return new Capability(capCode, caps);
    }

    public String nodeString() {
        return CommonUtils.getArrayString(nodes);
    }

    @Override
    public boolean equals(Object other) {
        if(!this.getClass().equals(other.getClass())) {
            return false;
        }

        Capability otherCap = (Capability)other;
        if(!otherCap.code.equals(this.code)) {
            return false;
        }

        if(!this.nodes.equals(otherCap.nodes)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(code)
            .append(nodes)
            .build();
    }

    public String toString() {
        return new StringBuilder(this.code)
            .append(" = ")
            .append(CommonUtils.getArrayString(nodes))
            .toString();
    }
}
