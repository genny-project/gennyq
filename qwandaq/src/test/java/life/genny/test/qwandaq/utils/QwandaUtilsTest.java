package life.genny.test.qwandaq.utils;

import java.util.HashMap;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.utils.testsuite.JUnitTester;

public class QwandaUtilsTest extends BaseTestCase {

	@Inject
	QwandaUtils qwandaUtils;

    @Test
    public void testBasicAttributeCodeFilter() {

        new JUnitTester<String, Boolean>()
            .setTest((input) -> {
                return Expected(QwandaUtils.attributeCodeMeetsBasicRequirements(input.input));
            })
            
            .createTest("Exclude Submit")
            .setInput("EVT_SUBMIT")
            .setExpected(false)
            .build()

            .createTest("Include First Name")
            .setInput("PRI_FIRSTNAME")
            .setExpected(true)
            .build()

            .assertAll();
    }

    @Test
    public void testMandatoryFieldCheck() {

		String PER_SOURCE = "PER_SOURCE";
		String PER_TARGET = "PER_TARGET";

		Attribute grp = new Attribute("QQQ_QUESTION_GROUP", "Question Group", new DataType(String.class));
		Attribute firstName = new Attribute("PRI_FIRSTNAME", "First Name", new DataType(String.class));
		Attribute lastName = new Attribute("PRI_LASTNAME", "Last Name", new DataType(String.class));

		Question testQ = new Question("QUE_TEST", "Test", grp);
		Question firstNameQ = new Question("QUE_FIRSTNAME", "FirstName", firstName);
		Question lastNameQ = new Question("QUE_LASTNAME", "LastName", lastName);

		Ask testAsk = new Ask(testQ, PER_SOURCE, PER_TARGET);
		Ask firstNameAsk = new Ask(firstNameQ, PER_SOURCE, PER_TARGET);
		firstNameAsk.setMandatory(true);
		Ask lastNameAsk = new Ask(lastNameQ, PER_SOURCE, PER_TARGET);
		lastNameAsk.setMandatory(true);

		testAsk.add(firstNameAsk);
		testAsk.add(lastNameAsk);

		HashMap<String, Ask> map = new HashMap<>();
		map.put(grp.getCode(), testAsk);
		map.put(firstName.getCode(), firstNameAsk);
		map.put(lastName.getCode(), lastNameAsk);

		BaseEntity filled = new BaseEntity(PER_TARGET);
		filled.addAttribute(new EntityAttribute(filled, firstName, 1.0, "Boris"));
		filled.addAttribute(new EntityAttribute(filled, lastName, 1.0, "Yeltson"));

		BaseEntity nonFilled = new BaseEntity(PER_TARGET);
		nonFilled.addAttribute(new EntityAttribute(nonFilled, firstName, 1.0, "Boris"));
		nonFilled.addAttribute(new EntityAttribute(nonFilled, lastName, 1.0, null));

        new JUnitTester<BaseEntity, Boolean>()
            .setTest((input) -> {
                return Expected(QwandaUtils.mandatoryFieldsAreAnswered(map, input.input));
            })

            .createTest("Answered Case")
            .setInput(filled)
            .setExpected(true)
            .build()

            // .createTest("Not Answered Case")
            // .setInput(nonFilled)
            // .setExpected(false)
            // .build()

			.assertAll();

    }

}
