package life.genny.qwandaq.serialization.adapters.capabilities;

import javax.json.bind.adapter.JsonbAdapter;

import life.genny.qwandaq.datatype.capability.core.node.CapabilityMode;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.datatype.capability.core.node.PermissionMode;
import life.genny.qwandaq.exception.runtime.BadDataException;

public class CapabilityNodeAdapter implements JsonbAdapter<CapabilityNode[], String[]> {

    @Override
    public String[] adaptToJson(CapabilityNode[] obj) throws Exception {
        String[] strings = new String[obj.length];
        for(int i = 0; i < obj.length; i++) {
            strings[i] = adaptOneToJson(obj[i]);
        }

        return strings;
    }

    public static String adaptOneToJson(CapabilityNode obj) throws Exception {
        return obj.capMode.getIdentifier() + CapabilityNode.DELIMITER + obj.permMode.getIdentifier();
    }

    @Override
    public CapabilityNode[] adaptFromJson(String[] obj) throws Exception {
        CapabilityNode[] nodes = new CapabilityNode[obj.length];

        for(int i = 0; i < obj.length; i++) {
            nodes[i] = adaptOneFromJson(obj[i]);
        }

        return nodes;
    }

    public static CapabilityNode adaptOneFromJson(String obj) {
        char modeId = obj.charAt(0);
        char permId = obj.charAt(2);
        return CapabilityNode.get(modeId, permId);
    }

    public static CapabilityNode parseCapability(String capabilityString) 
    throws BadDataException {
        CapabilityMode capMode;
        PermissionMode permMode;

        if(capabilityString.length() != 3) {
            System.err.println("Expected length 3. Got: " + capabilityString.length());
            throw new BadDataException("Could not deserialize capability node: " + capabilityString);
        }

        capMode = CapabilityMode.getByIdentifier(capabilityString.charAt(0));
        permMode = PermissionMode.getByIdentifier(capabilityString.charAt(2));

        return CapabilityNode.get(capMode, permMode);
    }
    
}
