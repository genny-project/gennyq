package life.genny.test.qwandaq.json;

import org.junit.jupiter.api.Test;

import life.genny.qwandaq.datatype.capability.core.CapabilityBuilder;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.entity.search.trait.Action;
import life.genny.qwandaq.entity.search.trait.Trait;
import life.genny.qwandaq.utils.testsuite.Expected;
import life.genny.qwandaq.utils.testsuite.JUnitTester;

import static life.genny.qwandaq.datatype.capability.core.node.PermissionScope.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import static life.genny.qwandaq.datatype.capability.core.node.CapabilityMode.*;

public class SearchEntityTest extends SerialisationTest<SearchEntity> {
    
    // @Test
    // public void serializationTest() {

    //     new JUnitTester<SearchEntity, SearchEntity>()
    //     .setTest((input) -> {
    //         SearchEntity inputSE = input.input;
    //         String json = jsonb.toJson(inputSE);
    //         SearchEntity outputSE = jsonb.fromJson(json, SearchEntity.class);

    //         return new Expected<>(outputSE);
    //     })
    //     .setVerification((result, expected) -> {
    //         Set<Entry<Class<? extends Trait>, List<? extends Trait>>>  resultTraits = result.getTraitMap().entrySet();
    //         Set<Entry<Class<? extends Trait>, List<? extends Trait>>>  expectedTraits = expected.getTraitMap().entrySet();
            
    //         assertEquals(result, expected);
    //         assertEquals(resultTraits, expectedTraits);            
    //     })

    //     .createTest("SBE_TEST 1")
    //     .setInput(new SearchEntity("SBE_TEST", "Test")
    //         .add(new Action("EDIT", "EDIT")
    //             .addCapabilityRequirement(CapabilityBuilder.code("CAP_PROPERTY").add(ALL).buildCap())
    //         )
    //     )
    //     .setExpected(new SearchEntity("SBE_TEST", "Test")
    //         .add(new Action("EDIT", "EDIT")
    //             .addCapabilityRequirement(CapabilityBuilder.code("CAP_PROPERTY").add(ALL).buildCap())
    //         )
    //     )
    //     .build()

    //     .assertAll();
    // }
}
