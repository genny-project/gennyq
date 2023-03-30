package life.genny.qwandaq.utils.javax;

import java.util.Map.Entry;

import life.genny.qwandaq.utils.CommonUtils;

import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;


public class JsonUtils {


    /**
     * Filter a {@link JsonArray javax json array}
     * @param value json array to filter
     * @param criteria one or more keys that should be filtered out
     * @return a new {@link JsonArray JsonArray} object without the keys that were supplied in criteria
     */
    public static JsonArray filter(JsonArray value, String... criteria) {
        if(criteria == null || criteria.length == 0)
            return value;

        JsonArrayBuilder builder = Json.createArrayBuilder();
        for(int i = 0; i < value.size(); i++) {
            JsonValue childValue = value.get(i);
            switch(childValue.getValueType()) {
                case ARRAY:
                    JsonArray array = filter(childValue.asJsonArray(), criteria);
                    builder.add(array);
                    break;
                case OBJECT:
                    JsonObject object = filter(childValue.asJsonObject(), criteria);
                    builder.add(object);
                    break;
                case NULL:
                    System.err.println("Null detected in JSON. Unsure what to do at this point so letting it through");
                case FALSE:
                case NUMBER:
                case STRING:
                case TRUE:
                    builder.add(childValue);
                    break;
                default:
                    System.err.println("Unsupported JsonValue type: " + childValue.getValueType());
                    break;
            }
        }

        return builder.build();
    }

    /**
     * Filter a {@link JsonObject javax json object}
     * @param value json array to filter
     * @param criteria one or more keys that should be filtered out
     * @return a new {@link JsonObject JsonObject} object without the keys that were supplied in criteria
     */
    public static JsonObject filter(JsonObject object, String... criteria) {
        if(criteria == null || criteria.length == 0)
            return object;
        JsonObjectBuilder builder = Json.createObjectBuilder();
        Set<Entry<String, JsonValue>> values = object.entrySet();
        for(Entry<String, JsonValue> mapping : values) {
            switch(mapping.getValue().getValueType()) {
                case ARRAY:
                    if(!CommonUtils.isInArray(criteria, mapping.getKey())) {
                        JsonArray array = filter(mapping.getValue().asJsonArray(), criteria);
                        builder.add(mapping.getKey(), array);
                    }
                    break;
                case OBJECT:
                    if(!CommonUtils.isInArray(criteria, mapping.getKey())) {
                        JsonObject obj = filter(mapping.getValue().asJsonObject(), criteria);
                        builder.add(mapping.getKey(), obj);
                    }
                    break;
                case FALSE:
                case NULL:
                case NUMBER:
                case STRING:
                case TRUE:
                    builder.add(mapping.getKey(), mapping.getValue());
                    break;
                default:
                    break;
            }
        }

        return builder.build();
    }
}
