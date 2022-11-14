package life.genny.qwandaq.converter;

import java.util.Set;

import javax.persistence.AttributeConverter;

import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.serialization.adapters.CapabilityAdapter;

public class CapabilityConverter implements AttributeConverter<Set<Capability>, String> {

    // Converter handles
    public String convertToDatabaseColumn(Set<Capability> attribute) {
        return CapabilityAdapter.convertToDBColumn(attribute);
    }

    public Set<Capability> convertToEntityAttribute(String dbData) {
        return CapabilityAdapter.convertToEA(dbData);
    }
}