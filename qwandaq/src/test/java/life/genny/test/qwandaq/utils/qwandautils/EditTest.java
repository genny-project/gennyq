package life.genny.test.qwandaq.utils.qwandautils;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;

import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.Definition;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.utils.collections.MapDecorator;
import life.genny.qwandaq.utils.testsuite.JUnitTester;
import life.genny.test.qwandaq.utils.BaseTestCase;
import life.genny.qwandaq.datatype.capability.core.CapabilityBuilder;

import static life.genny.qwandaq.datatype.capability.core.node.PermissionMode.*;

public class EditTest extends BaseDefTest {

    @Before
    public void defInit() {
        initDefaultDefs();
    }

    // Reusable JUnitTester
    static JUnitTester<Definition, Map<String, Boolean>> unitTester = new JUnitTester<Definition, Map<String, Boolean>>()
        .setTest((input) -> {
            log("========= USER CAPS ==========");
            CommonUtils.printCollection(USER_TEST_CAPS, BaseTestCase::log, (cap) -> "      " + cap);
            log("==============================\n");
            Map<String, Boolean> attributeResults = new HashMap<String, Boolean>();
            log("Testing def: " + input.input);
            for (EntityAttribute ea : input.input.getBaseEntityAttributes()) {
                log("Entity Attribute: " + ea.getAttributeCode());
                attributeResults.put(ea.getAttributeCode(),
                        QwandaUtils.checkCanEditEntityAttribute(baseEntities, USER_TEST_CAPS, input.input, ea.getAttributeCode()));
            }
            return Expected(attributeResults);
        })
        .setAssertion((result, expected) -> {
            if (result.size() != expected.size()) {
                throw new AssertionError("Result size does not equal expected size");
            }

            // Map assert
            for (Entry<String, Boolean> actualResult : result.entrySet()) {
                Boolean expectedBoolean = expected.get(actualResult.getKey());
                assertEquals(expectedBoolean, actualResult.getValue());
            }
        });

    @Test
    public void blockSurfaceLevelTest() {
        Definition testDef = addDefinition(DefinitionDecorator("DEF_TEST_ENTITY", "Test Entity")
                                    .addStringEA("ATT_PRI_UNIQUE_ID", "ID Attr").setValue("SOMETHING")
                                    .addRequirement(new CapabilityBuilder("CAP_TEST_ENTITY:~:ATT_PRI_UNIQUE_ID").edit(ALL).buildCap())
                                    .build()
                                .getDefinition());
        // Clear user caps
        setTestUserCaps();

        printDef(testDef);

        unitTester
        .createTest("Test surface level fail")
        .setInput(testDef)
        .setExpected(new MapDecorator<String, Boolean>()
                    .put("ATT_PRI_UNIQUE_ID", false)
                    .get())
        .build()
        .assertAll();
    }
    
    @Test
    public void testMultipleInheritancePassOnFather() {
        Definition child = addDefinition(DefinitionDecorator("DEF_CHILD", "Child")
                                .addStringEA("ATT_PRI_PREFIX", "Prefix Attribute").setValue("CHD")
                                .build()
                            .getDefinition());
        
        Definition father = addDefinition(DefinitionDecorator("DEF_FATHER", "Father")
                                .addStringEA("ATT_PRI_PREFIX", "Prefix Attribute").setValue("FTH")
                                .addRequirement(new CapabilityBuilder("CAP_FATHER:~:ATT_PRI_PREFIX").edit(ALL).buildCap())
                                .build()
                            .getDefinition());
        
        Definition mother = addDefinition(DefinitionDecorator("DEF_MOTHER", "Mother")
                                .addStringEA("ATT_PRI_PREFIX", "Prefix Attribute").setValue("MTH")
                                .build()
                            .getDefinition());
        
        Definition grandParent = addDefinition(DefinitionDecorator("DFE_GRANDPARENT", "Grandparent")
                                .addStringEA("ATT_PRI_PREFIX", "Prefix Attribute").setValue("GPT")
                                .build()
                            .getDefinition());

        setTestUserCaps(
            new CapabilityBuilder("CAP_FATHER:~:ATT_PRI_PREFIX").edit(SELF).buildCap()
        );

        linkEntities(child, father, mother);
        linkEntities(father, grandParent);

        printDef(father);

        unitTester
                .createTest("Test Multiple Inheritance and pass on child father")
                .setInput(child)
                .setExpected(new MapDecorator<String, Boolean>()
                        .put("ATT_PRI_PREFIX", true)
                        .put("LNK_INCLUDE", true)
                        .get())
                .build()

                .assertAll();
    }

    @Test
    public void testMultipleInheritanceFail() {
        Definition child = addDefinition(DefinitionDecorator("DEF_CHILD2", "Child")
                                .addStringEA("ATT_PRI_PREFIX", "Prefix Attribute").setValue("CHD")
                                .build()
                            .getDefinition());
        
        Definition father = addDefinition(DefinitionDecorator("DEF_FATHER2", "Father")
                                .addStringEA("ATT_PRI_PREFIX", "Prefix Attribute").setValue("FTH")
                                .build()
                            .getDefinition());
        
        Definition mother = addDefinition(DefinitionDecorator("DEF_MOTHER2", "Mother")
                                .addStringEA("ATT_PRI_PREFIX", "Prefix Attribute").setValue("MTH")
                                .addRequirement(new CapabilityBuilder("CAP_MOTHER2:~:ATT_PRI_PREFIX").edit(ALL).buildCap())
                                .build()
                            .getDefinition());
        
        Definition grandParent = addDefinition(DefinitionDecorator("DFE_GRANDPARENT2", "Grandparent")
                                .addStringEA("ATT_PRI_PREFIX", "Prefix Attribute").setValue("GPT")
                                .build()
                            .getDefinition());

        setTestUserCaps(
            // new CapabilityBuilder("CAP_MOTHER2:~:ATT_PRI_PREFIX").edit(ALL).buildCap()
        );

        linkEntities(child, father, mother);
        linkEntities(father, grandParent);

        printDef(father);

        unitTester
                .createTest("Test Multiple Inheritance and pass on child father")
                .setInput(child)
                .setExpected(new MapDecorator<String, Boolean>()
                        .put("ATT_PRI_PREFIX", false)
                        .put("LNK_INCLUDE", true)
                        .get())
                .build()

                .assertAll();
    }

    @Test
    public void testMultipleInheritancePassAtTop() {
        
    }

    @Test
    public void testSingle() {
        Definition entity = DefinitionDecorator("DEF_WEIRD", "Weird entity")
                    .addStringEA("ATT_PRI_PREFIX", "Prefix Attribute")
                    .setValue("PPP")
                    .addRequirement(new CapabilityBuilder("CAP_WEIRD:~:ATT_PRI_PREFIX").edit(ALL).buildCap())
                    .build()

                .getDefinition();

        unitTester
                .createTest("Single Entity no inheritance Check")
                .setInput(entity)
                .setExpected(new MapDecorator<String, Boolean>()
                        .put("ATT_PRI_PREFIX", false)
                        .get())
                .build()

                .assertAll();
    }

    @Test
    public void testNoInheritance() {
        Definition entity = addDefinition(DefinitionDecorator("DEF_WEIRD", "Weird entity")
                    .addStringEA("ATT_PRI_PREFIX", "Prefix Attribute")
                    .setValue("PPP")
                    .addRequirement(new CapabilityBuilder("CAP_WEIRD:~:ATT_PRI_PREFIX").edit(ALL).buildCap())
                    .build()

                .getDefinition());

        unitTester
                .createTest("Single Entity no inheritance Check")
                .setInput(entity)
                .setExpected(new MapDecorator<String, Boolean>()
                        .put("ATT_PRI_PREFIX", false)
                        .get())
                .build()

                .assertAll();
    }

    @Test
    public void testCanEdit() {
        /**
         * Test this definition for editable Entity Attributes
         * 
         */
        setTestUserCaps(
                new CapabilityBuilder("CAP_TEST:~:ATT_PRI_PREFIX").edit(ALL).buildCap());

        unitTester
                .createTest("DEF_TEST Edit Capability Check")
                .setInput(getDefinition("DEF_TEST"))
                .setExpected(new MapDecorator<String, Boolean>()
                        .put("ATT_PRI_PREFIX", true)
                        .put("LNK_INCLUDE", true)
                        .get())
                .build()

                .assertAll();
    }
}
