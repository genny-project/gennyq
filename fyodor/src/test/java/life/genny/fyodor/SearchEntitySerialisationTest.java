package life.genny.fyodor;

import org.junit.jupiter.api.Test;

import life.genny.qwandaq.datatype.capability.core.CapabilityBuilder;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.entity.search.trait.Action;
import life.genny.qwandaq.entity.search.trait.Trait;
import life.genny.qwandaq.utils.CommonUtils;

import static life.genny.qwandaq.datatype.capability.core.node.PermissionMode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

public class SearchEntitySerialisationTest {
    
    static Jsonb jsonb = JsonbBuilder.create();

    @Test
    public void serialiseTrait() {
        Trait action = new Action("EDIT", "EDIT")
            .addCapabilityRequirement(CapabilityBuilder.code("CAP_PROPERTY").edit(ALL).buildCap())
            .addCapabilityRequirement(CapabilityBuilder.code("CAP_IRVAN").add(SELF).edit(NONE).view(ALL).buildCap());

        SearchEntity entity = new SearchEntity("PROP_SEARCH", "Property")
        .add(action);
        String json = jsonb.toJson(entity);
        System.out.println("JSOON: " + json);

        SearchEntity entity2 = jsonb.fromJson(json, SearchEntity.class);
        System.out.println(entity);
        System.out.println(CommonUtils.getArrayString(action.getCapabilityRequirements()));
        assertEquals(entity, entity2);
    }
}
