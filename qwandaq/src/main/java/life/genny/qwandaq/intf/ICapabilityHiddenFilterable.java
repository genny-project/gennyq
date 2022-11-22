package life.genny.qwandaq.intf;

import java.util.Set;

import javax.json.bind.annotation.JsonbTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import life.genny.qwandaq.datatype.capability.core.Capability;

public interface ICapabilityHiddenFilterable extends ICapabilityFilterable {
    
    @Override
    @JsonbTransient
    @JsonIgnore
    public Set<Capability> getCapabilityRequirements();
    
    @Override
    @JsonbTransient
    @JsonIgnore
    public void setCapabilityRequirements(Set<Capability> requirements);
}
