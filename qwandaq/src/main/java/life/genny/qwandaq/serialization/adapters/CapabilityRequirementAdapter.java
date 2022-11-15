package life.genny.qwandaq.serialization.adapters;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.adapter.JsonbAdapter;

import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.entity.search.trait.CapabilityRequirement;
import life.genny.qwandaq.utils.CommonUtils;

public class CapabilityRequirementAdapter implements JsonbAdapter<Set<CapabilityRequirement>, JsonArray> {

    static Jsonb jsonb = JsonbBuilder.create();

    @Override
    public JsonArray adaptToJson(Set<CapabilityRequirement> obj) throws Exception {
        JsonArrayBuilder array = Json.createArrayBuilder();
        for(CapabilityRequirement req : obj) {
            array.add(adaptOneToJson(req));
        }

        return array.build();
    }

    public static JsonObject adaptOneToJson(CapabilityRequirement obj) {
        return Json.createObjectBuilder()
            .add("code", obj.getCode())
            .add("nodes", CommonUtils.getArrayString(obj.getNodes()))
            .add("reqAll", obj.requiresAll())
            .build();
    }

    @Override
    public Set<CapabilityRequirement> adaptFromJson(JsonArray array) throws Exception {
        Set<CapabilityRequirement> reqs = new HashSet<>(array.size());
        for(int i = 0; i < array.size(); i++) {
            JsonObject object = array.getJsonObject(i);
            reqs.add(adaptOneFromJson(object));
        }

        return reqs;
    }

    private static CapabilityRequirement adaptOneFromJson(JsonObject obj) {
        String name = obj.getString("code");
        String code = name;
        String nodeString = obj.getString("nodes");
        boolean reqAll = obj.getBoolean("reqAll");
        
        List<CapabilityNode> nodes = CommonUtils.getListFromString(nodeString, CapabilityNode::parseCapability);
        return new CapabilityRequirement(code, reqAll, nodes.toArray(new CapabilityNode[0]));
    }
    
}
