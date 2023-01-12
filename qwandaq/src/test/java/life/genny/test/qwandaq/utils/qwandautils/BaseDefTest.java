package life.genny.test.qwandaq.utils.qwandautils;

import java.util.HashMap;
import java.util.Map;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.datatype.capability.core.CapabilityBuilder;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.Definition;
import life.genny.qwandaq.utils.builder.BaseEntityDecorator;
import life.genny.test.qwandaq.utils.capabilities.requirements.BaseRequirementsTest;

import static life.genny.qwandaq.datatype.capability.core.node.PermissionMode.*;

public abstract class BaseDefTest extends BaseRequirementsTest {

    private static Map<String, Definition> baseEntities = new HashMap<>();
    static {

        // Setup Grandchild
        addBaseEntity(new BaseEntityDecorator("DEF_TEST", "Test Definition")
            .addEA()
                .setAttribute("ATT_PRI_PREFIX", "Prefix Attribute", new DataType(String.class))
                .addRequirement(new CapabilityBuilder("TEST CAP").add(ALL).buildCap())
                .setValue("TST")
            .build()
            .addEA()
                .setAttribute(Attribute.LNK_INCLUDE, "Include Attribute", new DataType(String.class))
                .setValue("[\"DEF_TEST_FATHER\"]")
            .build()

            .getBaseEntity()
        );

        // Setup Father
        addBaseEntity(new BaseEntityDecorator("DEF_TEST_FATHER", "Test Father Def")
            .addEA()
                .setAttribute("ATT_PRI_PREFIX", "Prefix Attribute", new DataType(String.class))
                .setValue("TST")
            .build()
            .addEA()
                .setAttribute(Attribute.LNK_INCLUDE, "Include Attribute", new DataType(String.class))
                .setValue("[\"DEF_TEST_GRANDFATHER\"]")
            .build()

            .getBaseEntity()
        );

        // Setup Grandfather
        addBaseEntity(new BaseEntityDecorator("DEF_TEST_GRANDFATHER", "Test Grandfather Def")
            .addEA()
                .setAttribute("ATT_PRI_PREFIX", "Prefix Attribute", new DataType(String.class))
                .setValue("TST")
            .build()
            .addEA()
                .setAttribute(Attribute.LNK_INCLUDE, "Include Attribute", new DataType(String.class))
                .setValue("[\"DEF_TEST_ANCIENT1\",\"DEF_TEST_ANCIENT2\"]")
            .build()

            .getBaseEntity()
        );

        // Setup Ancient Parents
        addBaseEntity(new BaseEntityDecorator("DEF_TEST_ANCIENT1", "Test Ancient 1 Def")
            .addEA()
                .setAttribute("ATT_PRI_PREFIX", "Prefix Attribute", new DataType(String.class))
                .setValue("TST")
            .build()
            .addEA()
                .setAttribute(Attribute.LNK_INCLUDE, "Include Attribute", new DataType(String.class))
                .setValue("[]")
            .build()

            .getBaseEntity()
        );

        addBaseEntity(new BaseEntityDecorator("DEF_TEST_ANCIENT2", "Test Ancient 2 Def")
            .addEA()
                .setAttribute("ATT_PRI_PREFIX", "Prefix Attribute", new DataType(String.class))
                .setValue("TST")
            .build()

            .getBaseEntity()
        );
    }

    protected static void addBaseEntity(BaseEntity be) {
        baseEntities.put(be.getCode(), (Definition)be);
    }

    public static Definition getBaseEntity(String code) {
        return baseEntities.get(code);
    }
}
