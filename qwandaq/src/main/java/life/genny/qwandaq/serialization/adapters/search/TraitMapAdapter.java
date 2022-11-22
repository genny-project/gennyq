package life.genny.qwandaq.serialization.adapters.search;

import java.util.List;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.adapter.JsonbAdapter;

import life.genny.qwandaq.entity.search.TraitMap;
import life.genny.qwandaq.entity.search.trait.Trait;

public class TraitMapAdapter implements JsonbAdapter<TraitMap, JsonObject> {

    static Jsonb jsonb = JsonbBuilder.create();

    @Override
    public JsonObject adaptToJson(TraitMap obj) throws Exception {
        // for each id in map serialise the list and add it as a key
        JsonObjectBuilder object = Json.createObjectBuilder();
        for(Entry<Integer, List<? extends Trait>> trait : obj.entrySet()) {
            JsonArrayBuilder traitList = Json.createArrayBuilder();

            for(Trait t : trait.getValue()) {
                traitList.add(jsonb.toJson(t));
            }

            object.add(Integer.toString(trait.getKey()), traitList.build());
        }
        return object.build();
    }

    @Override
    public TraitMap adaptFromJson(JsonObject obj) throws Exception {
        TraitMap map = new TraitMap();
        System.out.println("DESERIALISING: " + obj.toString());
        for(Entry<String, JsonValue> entry : obj.entrySet()) {
            int id = Integer.parseInt(entry.getKey());
            Class<? extends Trait> deserialiseClass = TraitMap.TRAIT_MAP_IDS.get(id);
            System.out.println("Deserialising: " + deserialiseClass.getName()  + ": " + entry.getValue().toString());
        }

        return map;
    }
    
}
