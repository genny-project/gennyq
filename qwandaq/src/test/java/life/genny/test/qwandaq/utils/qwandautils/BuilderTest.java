package life.genny.test.qwandaq.utils.qwandautils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.testsuite.JUnitTester;
import life.genny.test.qwandaq.utils.BaseTestCase;

public class BuilderTest extends BaseDefTest {
    private static final String GAP = "        ";

    @Test
    public void verifyDefs() {
        new JUnitTester<String, String>()
        .setTest((input) -> {
            BaseEntity be = getDefinition(input.input);
            if(be == null)
                return Expected("null be for ".concat(input.input));

            log("Found BaseEntity: " + be.getCode());
            CommonUtils.printCollection(be.getBaseEntityAttributes(), BaseTestCase::log, (ea) -> {
                StringBuilder sb = new StringBuilder(GAP)
                    .append(ea.getAttributeCode())
                    .append(" = ")
                    .append(ea.getValueString())
                    .append("\n")
                    .append(GAP)
                    .append(GAP)
                    .append("Requirements: ")
                    .append(CommonUtils.getArrayString(ea.getCapabilityRequirements()));
                return sb.toString();
            });

            log("=============================");
            return Expected(be.getCode());
        })

        .createTest("Verify DEF_TEST")
        .setInput("DEF_TEST")
        .setExpected("DEF_TEST")
        .build()

        .createTest("Verify DEF_TEST_FATHER")
        .setInput("DEF_TEST_FATHER")
        .setExpected("DEF_TEST_FATHER")
        .build()

        .createTest("Verify DEF_TEST_GRANDFATHER")
        .setInput("DEF_TEST_GRANDFATHER")
        .setExpected("DEF_TEST_GRANDFATHER")
        .build()

        .createTest("Verify DEF_TEST_ANCIENT1")
        .setInput("DEF_TEST_ANCIENT1")
        .setExpected("DEF_TEST_ANCIENT1")
        .build()

        .createTest("Verify DEF_TEST_ANCIENT2")
        .setInput("DEF_TEST_ANCIENT2")
        .setExpected("DEF_TEST_ANCIENT2")
        .build()
        
        .assertAll();
    }

    @Test
    public void verifyLinks() {
        new JUnitTester<String, Object[]>()
        .setTest((input) -> {
            // input be code
            // output array of parent codes if lnk include exists
            BaseEntity be = getDefinition(input.input);
            Optional<EntityAttribute> optLnk = be.findEntityAttribute(Attribute.LNK_INCLUDE);
            if(optLnk.isPresent()) {
                return Expected(CommonUtils.getArrayFromString(optLnk.get().getValueString(), String.class, (str) -> str));
            } else {
                return Expected(new Object[] {"nolnk"});
            }
        })
        .setAssertion((result, expected) -> assertArrayEquals(expected, result))

        .createTest("Test DEF_TEST Parents")
        .setInput("DEF_TEST")
        .setExpected(new Object[] {"DEF_TEST_FATHER"})
        .build()

        .createTest("Test DEF_TEST_FATHER Parents")
        .setInput("DEF_TEST_FATHER")
        .setExpected(new Object[] {"DEF_TEST_GRANDFATHER"})
        .build()

        .createTest("Test DEF_TEST_GRANDFATHER Parents")
        .setInput("DEF_TEST_GRANDFATHER")
        .setExpected(new Object[] {"DEF_TEST_ANCIENT1", "DEF_TEST_ANCIENT2"})
        .build()

        .createTest("Test DEF_TEST_ANCIENT1 Parents")
        .setInput("DEF_TEST_ANCIENT1")
        .setExpected(new Object[] {})
        .build()

        .createTest("Test DEF_TEST_ANCIENT2 Parents")
        .setInput("DEF_TEST_ANCIENT2")
        .setExpected(new Object[] {"nolnk"})
        .build()

        .assertAll();
    }
}
