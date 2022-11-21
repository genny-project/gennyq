package life.genny.qwandaq.serialization.adapters.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class TraitMapAdapter implements JsonbAdapter<TraitMap, JsonObject> {

    private static Map<Class<? extends Trait>, Character> inMap = new HashMap<>();
    private static Map<Character, Class<? extends Trait>> outMap = new HashMap<>();
    static {
        inMap.put(Action.class, '0');
        inMap.put(AssociatedColumn.class, '1');
        inMap.put(Column.class, '2');
        inMap.put(Filter.class, '3');
        inMap.put(Sort.class, '4');

        for(Entry<Class<? extends Trait>, Character> entry : inMap.entrySet()) {
            outMap.put(entry.getValue(), entry.getKey());
        }
    }

    @Override
    public JsonObject adaptToJson(TraitMap obj) throws Exception {
        JsonObjectBuilder objectJson = Json.createObjectBuilder();
        for(Entry<Class<? extends Trait>, List<? extends Trait>> traits : obj.entrySet()) {
            int id = inMap.get(traits.getKey());
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
