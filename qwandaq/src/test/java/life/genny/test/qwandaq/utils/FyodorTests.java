package life.genny.test.qwandaq.utils;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.junit.jupiter.api.Test;

import life.genny.qwandaq.datatype.capability.core.CapabilityBuilder;
import life.genny.qwandaq.datatype.capability.core.node.PermissionMode;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.entity.search.trait.Action;
import life.genny.qwandaq.entity.search.trait.Trait;

public class FyodorTests extends BaseTestCase {
    
    static Jsonb jsonb = JsonbBuilder.create();

    @Test
    public void testCapabilityRequirements() {
        Trait action = new Action("EDIT", "Edit")
            .addCapabilityRequirement(new CapabilityBuilder("CAP_PROPERTY").add(PermissionMode.SELF).edit(PermissionMode.ALL).buildCap(), false)
            ;
        
        SearchEntity entity = new SearchEntity("SBE_FEATURED_APARTMENTS", "Featured Apartments")
        .add(action);
        String json = jsonb.toJson(entity);
        System.out.println(json);
        System.out.println("==================");
        System.out.println("Deserializing: " + json);
        entity = jsonb.fromJson(json, SearchEntity.class);
        System.out.println(entity);
    }        
}
