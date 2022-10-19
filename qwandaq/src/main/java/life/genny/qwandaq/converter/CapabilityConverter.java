package life.genny.qwandaq.converter;

import javax.persistence.AttributeConverter;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.datatype.capability.Capability;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;

public class CapabilityConverter implements AttributeConverter<Capability, String> {
	private static final String ARRAY_START = "[";
	private static final Logger log = Logger.getLogger(CapabilitiesManager.class);

    @Override
    public String convertToDatabaseColumn(Capability attribute) {
        if(attribute == null) {
            return "";
        }
        return new StringBuilder(attribute.code).append(CapabilitiesManager.getModeString(attribute.nodes)).toString();
    }

    @Override
    public Capability convertToEntityAttribute(String dbData) {
        if(StringUtils.isBlank(dbData))
            return null;
        int delimIndex = dbData.indexOf(ARRAY_START);
        if(delimIndex == -1) {
            log.error("Could not find array start in capability string: " + dbData);
            log.error("Delimiter: " + ARRAY_START);
            throw new BadDataException("dbData: " + dbData);
        }

        String code = dbData.substring(0, delimIndex);
        String nodes = dbData.substring(delimIndex);
        return new Capability(code, nodes);
    }
}
