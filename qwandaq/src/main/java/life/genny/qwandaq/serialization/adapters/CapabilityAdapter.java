package life.genny.qwandaq.serialization.adapters;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.bind.adapter.JsonbAdapter;

import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;

public class CapabilityAdapter implements JsonbAdapter<Capability, JsonObject> {
    
    @Override
    public JsonObject adaptToJson(Capability capability) {
        return toJson(capability);
    }

    public static JsonObject toJson(Capability capability) {
        System.out.println("Serialising: " + capability);
        JsonArrayBuilder nodeArray = Json.createArrayBuilder();
        for(CapabilityNode node : capability.nodes) {
            nodeArray.add(node.toString());
        }

        JsonObject obj = Json.createObjectBuilder()
            .add("code", capability.code)
            .add("nodes", nodeArray.build())
            .build();
        return obj;
    }

    @Override
    public Capability adaptFromJson(JsonObject capJson) {
        return fromJson(capJson);
    }

    public static Capability fromJson(JsonObject capJson) {
        System.out.println("Deserializing: " + capJson);
        String code = capJson.getString("code");
        JsonArray nodeArray = capJson.getJsonArray("nodes");
        Set<CapabilityNode> nodes = new LinkedHashSet<>(nodeArray.size());
        for(int i = 0; i < nodeArray.size(); i++) {
            nodes.add(CapabilityNode.parseCapability(nodeArray.getString(i)));
        }
        // java.lang.ClassCastException: class org.glassfish.json.JsonObjectBuilderImpl$JsonObjectImpl cannot be cast to class javax.json.JsonString:67
        return new Capability(code, nodes);
    }
    

}
