package life.genny.qwandaq.serialization.adapters;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.bind.adapter.JsonbAdapter;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.managers.capabilities.CapabilitiesManager;
import life.genny.qwandaq.serialization.adapters.CapabilitySetAdapter;


public class CapabilitySetAdapter implements JsonbAdapter<Set<Capability>, JsonArray> {
    private static final String CAPABILITY_DELIMITER = "  ";
	private static final String ARRAY_START = "[";
	private static final Logger log = Logger.getLogger(CapabilitySetAdapter.class);

    // Method handles
    @Override
    public JsonArray adaptToJson(Set<Capability> deserializedSet) throws Exception {
        JsonArrayBuilder array = Json.createArrayBuilder();
        if(deserializedSet == null || deserializedSet.isEmpty()) {
            return array.build();
        }
        for(Capability cap : deserializedSet) {
            array.add(CapabilityAdapter.toJson(cap));
        }
        return array.build();
    }


    @Override
    public Set<Capability> adaptFromJson(JsonArray serializedSet) throws Exception {
        if(serializedSet == null || serializedSet.size() == 0)
            return new HashSet<>();
        Set<Capability> caps = new HashSet<>(serializedSet.size());
        for(int i = 0; i < serializedSet.size(); i++) {
            caps.add(CapabilityAdapter.fromJson(serializedSet.getJsonObject(i)));
        }
        return caps;
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

    public static Set<Capability> convertToEA(String dbData) {
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

    private static Capability deserializeOneCapability(String capData) {
        int delimIndex = capData.indexOf(ARRAY_START);
        if(delimIndex == -1) {
            log.error("Could not find array start in capability string: " + capData);
            log.error("Delimiter: " + ARRAY_START);
            throw new BadDataException("dbData: " + capData);
        }

        String code = capData.substring(0, delimIndex);
        String nodes = capData.substring(delimIndex);
        Capability c = new Capability(code, nodes);
        return c;
    }

    private static StringBuilder serializeOneCapability(StringBuilder sb, Capability capability) {
        return sb.append(capability.code).append(CapabilitiesManager.getModeString(capability.nodes));
    }
}
