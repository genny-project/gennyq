package life.genny.test.qwandaq.utils.qwandautils;

import org.junit.jupiter.api.Test;

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
            BaseEntity be = getBaseEntity(input.input);
            if(be == null)
                return Expected("null be for ".concat(input.input));

            log("Found BaseEntity: " + be.getCode());
            CommonUtils.printCollection(be.getBaseEntityAttributes(), BaseTestCase::log, (ea) -> {
                return GAP + ea.getAttributeCode() + " = " + ea.getValueString();
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
}
