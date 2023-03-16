package life.genny.qwandaq.converter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.AttributeConverter;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;

public class CapabilityConverter implements AttributeConverter<Set<Capability>, String> {
    private static final Logger log = Logger.getLogger(CapabilityConverter.class);
    private static final String CAPABILITY_DELIMITER = "  ";
	private static final String ARRAY_START = "[";

    // Converter handles
    public String convertToDatabaseColumn(Set<Capability> attribute) {
        return convertToDBColumn(attribute);
    }

    public Set<Capability> convertToEntityAttribute(String dbData) {
        return convertToEA(dbData);
    }


    public static String convertToDBColumn(Set<Capability> attributeSet) {
        if(attributeSet == null || attributeSet.isEmpty()) {
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

    public static Set<Capability> convertToEA(String dbData) 
        throws BadDataException {
        if(StringUtils.isBlank(dbData))
            return new HashSet<>();
        Set<Capability> capSet = new HashSet<>();
        String[] capabilities = dbData.split(CAPABILITY_DELIMITER);
        for(String cap : capabilities) {
            capSet.add(deserializeOneCapability(cap));
        }

        return capSet;
    }

    private static Capability deserializeOneCapability(String capData) 
        throws BadDataException {
        int delimIndex = capData.indexOf(ARRAY_START);
        if(delimIndex == -1) {
            log.error("Could not find array start in capability string: " + capData);
            log.error("Delimiter: " + ARRAY_START);
            throw new BadDataException("Could not find array start in capability string: " + capData + "\nDelimiter: " + ARRAY_START);
        }
        String code = capData.substring(0, delimIndex);
        if(!code.startsWith(Prefix.CAP_))
            throw new BadDataException("Capability Requirement code does not start with prefix: " + Prefix.CAP_ + ". Offending code: " + code);
        String nodes = capData.substring(delimIndex);
        List<CapabilityNode> nodeData = CapabilitiesManager.deserializeCapArray(nodes);
        return new Capability(code, nodeData);
    }

    private static StringBuilder serializeOneCapability(StringBuilder sb, Capability capability) {
        return sb.append(capability.code).append(CapabilitiesManager.getModeString(capability.nodes));
    }
}