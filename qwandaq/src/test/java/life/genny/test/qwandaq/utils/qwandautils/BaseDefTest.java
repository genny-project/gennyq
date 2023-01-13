package life.genny.test.qwandaq.utils.qwandautils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.Definition;
import life.genny.qwandaq.datatype.capability.core.CapabilityBuilder;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.builder.DefinitionDecorator;
import life.genny.test.qwandaq.utils.capabilities.requirements.BaseRequirementsTest;

import static life.genny.qwandaq.datatype.capability.core.node.PermissionMode.*;

public abstract class BaseDefTest extends BaseRequirementsTest {

    protected static Map<String, Definition> baseEntities = new HashMap<>();

    protected static void initDefaultDefs() {
        // Setup Grandchild
        addDefinition(new DefinitionDecorator("DEF_TEST", "Test Definition")
            .addEA()
                .setAttribute("ATT_PRI_PREFIX", "Prefix Attribute", new DataType(String.class))
                .addRequirement(new CapabilityBuilder("CAP_TEST:~:ATT_PRI_PREFIX").add(ALL).buildCap())
                .setValue("TST")
            .build()
            .addEA()
                .setAttribute(Attribute.LNK_INCLUDE, "Include Attribute", new DataType(String.class))
                .setValue("[\"DEF_TEST_FATHER\"]")
            .build()

            .getDefinition()
        );

        // Setup Father
        addDefinition(new DefinitionDecorator("DEF_TEST_FATHER", "Test Father Def")
            .addEA()
                .setAttribute("ATT_PRI_PREFIX", "Prefix Attribute", new DataType(String.class))
                .setValue("TST")
            .build()
            .addEA()
                .setAttribute(Attribute.LNK_INCLUDE, "Include Attribute", new DataType(String.class))
                .setValue("[\"DEF_TEST_GRANDFATHER\"]")
            .build()

            .getDefinition()
        );

        // Setup Grandfather
        Definition grandFather = addDefinition(new DefinitionDecorator("DEF_TEST_GRANDFATHER", "Test Grandfather Def")
            .addEA()
                .setAttribute("ATT_PRI_PREFIX", "Prefix Attribute", new DataType(String.class))
                .setValue("TST")
            .build()

            .getDefinition()
        );



        // Setup Ancient Parents
        Definition ancient1 = addDefinition(new DefinitionDecorator("DEF_TEST_ANCIENT1", "Test Ancient 1 Def")
            .addEA()
                .setAttribute("ATT_PRI_PREFIX", "Prefix Attribute", new DataType(String.class))
                .setValue("TST")
            .build()
            .addEA()
                .setAttribute(Attribute.LNK_INCLUDE, "Include Attribute", new DataType(String.class))
                .setValue("[]")
            .build()

            .getDefinition()
        );

        Definition ancient2 = addDefinition(new DefinitionDecorator("DEF_TEST_ANCIENT2", "Test Ancient 2 Def")
            .addEA()
                .setAttribute("ATT_PRI_PREFIX", "Prefix Attribute", new DataType(String.class))
                .setValue("TST")
            .build()

            .getDefinition()
        );

        linkEntities(grandFather, ancient1, ancient2);
        linkEntities(ancient1, ancient2);
    }

    protected static DefinitionDecorator DefinitionDecorator(String code, String name) {
        return new DefinitionDecorator(code, name);
    }

    protected static Definition addDefinition(Definition be) {
        baseEntities.put(be.getCode(), be);
        return be;
    }

    protected static Definition getDefinition(String code) {
        return baseEntities.get(code);
    }

    /**
     * Link two or more definitions with LNK_INCLUDE retroactively
     * @param child - definition with LNK_INCLUDE to modify
     * @param parents - parents to add to the LNK_INCLUDE
     */
    protected static void linkEntities(Definition child, Definition... parents) {
        Optional<EntityAttribute> optLnkInclude = child.findEntityAttribute(Attribute.LNK_INCLUDE);
        String previous = "";
        if(optLnkInclude.isPresent()) {
            previous = optLnkInclude.get().getValueString();
        }
        new DefinitionDecorator(child)
        .addEA()
        .setAttribute(Attribute.LNK_INCLUDE, "Include Attribute", new DataType(String.class))
        .setValue(CommonUtils.addToStringArray(previous, CommonUtils.mapArray(parents, String.class, parent -> parent.getCode())))
        .build();
    }

    /**
     * Unlink two or more definitions with LNK_INCLUDE retroactively
     * @param child - definition with LNK_INCLUDE to modify
     * @param parents - parents to remove from the LNK_INCLUDE
     */
    protected static void unlinkEntities(Definition child, Definition... parents) {
        Optional<EntityAttribute> optLnkInclude = child.findEntityAttribute(Attribute.LNK_INCLUDE);
        String previous = "";
        if(optLnkInclude.isPresent()) {
            previous = optLnkInclude.get().getValueString();
        }
        new DefinitionDecorator(child)
        .addEA()
        .setAttribute(Attribute.LNK_INCLUDE, "Include Attribute", new DataType(String.class))
        .setValue(CommonUtils.removeFromStringArray(previous, CommonUtils.mapArray(parents, String.class, parent -> parent.getCode())))
        .build();
    }
}
