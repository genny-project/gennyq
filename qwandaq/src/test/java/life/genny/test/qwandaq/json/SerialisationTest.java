package life.genny.test.qwandaq.json;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import life.genny.qwandaq.utils.callbacks.testing.FITestCallback;
import life.genny.qwandaq.utils.testsuite.Expected;
import life.genny.qwandaq.utils.testsuite.Input;

public abstract class SerialisationTest<Serializable> {
    
    protected static Jsonb jsonb = JsonbBuilder.create();

    // public FITestCallback<Serializable, Serializable> test(Input<Serializable> input) {
    //     return (Serializable i) -> {
    //         String json = jsonb.toJson(i);
    //         return jsonb.fromJson(json, i.getClass());
    //     };
    //     String json = jsonb.toJson(input);
    //     Capability outputCap = jsonb.fromJson(json, Serializable.class);

    //     return new Expected<>(outputCap);
    // }

}
