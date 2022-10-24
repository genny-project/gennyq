package life.genny.qwandaq.converter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.persistence.AttributeConverter;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.datatype.capability.Capability;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;

public class CapabilityConverter implements AttributeConverter<Set<Capability>, String> {
    private static final String CAPABILITY_DELIMITER = "$_$";
	private static final String ARRAY_START = "[";
	private static final Logger log = Logger.getLogger(CapabilitiesManager.class);

    @Override
    public String convertToDatabaseColumn(Set<Capability> attributeSet) {
        if(attributeSet.isEmpty()) {
            return "";
        }

        StringBuilder data = new StringBuilder();
        Iterator<Capability> iterator = attributeSet.iterator();
        for(int i = 0; i < attributeSet.size(); i++) {
            Capability cap = iterator.next();
            serializeOneCapability(data, cap);
            if(iterator.hasNext())
                data.append(CAPABILITY_DELIMITER);
        }

        return data.toString();
    }

    @Override
    public Set<Capability> convertToEntityAttribute(String dbData) {
        if(StringUtils.isBlank(dbData))
            return new HashSet<>();
        Set<Capability> capSet = new HashSet<>();
        String[] capabilities = dbData.split(CAPABILITY_DELIMITER);
        // TODO: May need to verify capability string
        for(String cap : capabilities) {
            capSet.add(deserializeOneCapability(cap));
        }

        return capSet;
    }

    private Capability deserializeOneCapability(String capData) {
        int delimIndex = capData.indexOf(ARRAY_START);
        if(delimIndex == -1) {
            log.error("Could not find array start in capability string: " + capData);
            log.error("Delimiter: " + ARRAY_START);
            throw new BadDataException("dbData: " + capData);
        }
        String code = capData.substring(0, delimIndex);
        String nodes = capData.substring(delimIndex);
        return new Capability(code, nodes);
    }

    private StringBuilder serializeOneCapability(StringBuilder sb, Capability capability) {
        return sb.append(capability.code).append(CapabilitiesManager.getModeString(capability.nodes));
    }
}
