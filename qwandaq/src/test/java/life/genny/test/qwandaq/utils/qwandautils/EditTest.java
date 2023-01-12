package life.genny.test.qwandaq.utils.qwandautils;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.Definition;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.utils.collections.MapDecorator;
import life.genny.qwandaq.utils.testsuite.JUnitTester;

public class EditTest extends BaseDefTest {
    

    @Test
    public void testCanEdit() {
        /**
         * Test this definition for editable Entity Attributes
         * 
         */
        new JUnitTester<Definition, Map<String, Boolean>>()
        .setTest((input) -> {
            Map<String, Boolean> attributeResults = new HashMap<String,Boolean>();
            for(EntityAttribute ea : input.input.getBaseEntityAttributes()) {
                attributeResults.put(ea.getAttributeCode(), QwandaUtils.checkCanEditEntityAttribute(USER_TEST_CAPS, input.input, ea));
            }
            return Expected(attributeResults);
        })
        .setAssertion((result, expected) -> {
            if(result.size() != expected.size()) {
                throw new AssertionError("Result size does not equal expected size");
            }

            // Map assert
            for(Entry<String, Boolean> actualResult : result.entrySet()) {
                Boolean expectedBoolean = expected.get(actualResult.getKey());
                assertEquals(expectedBoolean, actualResult.getValue());
            }
        })

        .createTest("DEF_TEST Edit Capability Check")
        .setInput(getBaseEntity("DEF_TEST"))
        .setExpected(new MapDecorator<String, Boolean>()
                .put("ATT_PRI_PREFIX", false)
                .get()
        )
        .build()

        .assertAll();
    }
}
