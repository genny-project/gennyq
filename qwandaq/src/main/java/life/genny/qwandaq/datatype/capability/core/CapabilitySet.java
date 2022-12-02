package life.genny.qwandaq.datatype.capability.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

import life.genny.qwandaq.entity.BaseEntity;

/**
 * A container class that extends HashSet of Capability. This allows the Capability Set
 * of a particular BaseEntity to be directly related to the BaseEntity itself, by storing a local
 * reference to the base entity
 */
public class CapabilitySet extends HashSet<Capability> {

    private final BaseEntity entity;

    public CapabilitySet(BaseEntity entity) {
        super();
        this.entity = entity;
    }

    // deliberately separate constructors to capitalize on java's impl of HashSet<>(Collection)
    public CapabilitySet(BaseEntity entity, Collection<Capability> capabilities) {
        super(capabilities);
        this.entity = entity;
    }

    public Optional<Capability> getCapabilityByCode(String code) {
        return this.stream().filter((cap) -> cap.code.equals(code)).findFirst();
    }

    public String getEntityCode() {
        return entity.getCode();
    }

    public BaseEntity getEntity() {
        return entity;
    }

    public String toString() {
        return new StringBuilder("CapabilitySet [ ")
        .append(this.size())
        .append(" ")
        .append(getEntityCode())
        .append(" capabilities: ")
        .append(super.toString())
        .append(" ]")
        .toString();
    }

    public boolean equals(Object other) {
        if(!(other instanceof CapabilitySet))
            return false;
        
        CapabilitySet otherSet = (CapabilitySet)other;
        if(!getEntityCode().equals(otherSet.getEntityCode())) {
            return false;
        }
        return super.equals(other);
    }
}
