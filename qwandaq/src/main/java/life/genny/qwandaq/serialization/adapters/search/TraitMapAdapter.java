package life.genny.qwandaq.serialization.adapters.search;

import java.util.List;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.adapter.JsonbAdapter;

import life.genny.qwandaq.entity.search.TraitMap;
import life.genny.qwandaq.entity.search.trait.Action;
import life.genny.qwandaq.entity.search.trait.AssociatedColumn;
import life.genny.qwandaq.entity.search.trait.Column;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Sort;
import life.genny.qwandaq.entity.search.trait.Trait;
import life.genny.qwandaq.utils.collections.BiDirectionalHashMap;

public class TraitMapAdapter implements JsonbAdapter<TraitMap, JsonObject> {
    private static BiDirectionalHashMap<Integer, Class<? extends Trait>> traitMap = new BiDirectionalHashMap<>();
    // private static Map<Class<? extends Trait>, Character> inMap = new HashMap<>();
    // private static Map<Character, Class<? extends Trait>> outMap = new HashMap<>();

    static {
        traitMap.put(0, Action.class);
        traitMap.put(1, AssociatedColumn.class);
        traitMap.put(2, Column.class);
        traitMap.put(3, Filter.class);
        traitMap.put(4, Sort.class);

        // for(Entry<Class<? extends Trait>, Character> entry : inMap.entrySet()) {
        //     outMap.put(entry.getValue(), entry.getKey());
        // }
    }

    @Override
    public JsonObject adaptToJson(TraitMap obj) throws Exception {
        JsonObjectBuilder objectJson = Json.createObjectBuilder();
        for(Entry<Class<? extends Trait>, List<? extends Trait>> traits : obj.entrySet()) {
            int id = traitMap.getKey(traits.getKey());
            JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
            for(Trait trait : traits.getValue()) {
                System.out.println(trait);
                jsonArrayBuilder.add(trait.toString());
            }

            objectJson.add(Integer.toString(id), jsonArrayBuilder.build());
        }

        return objectJson.build();
    }

    @Override
    public TraitMap adaptFromJson(JsonObject obj) throws Exception {
        return null;
    }
    
}
