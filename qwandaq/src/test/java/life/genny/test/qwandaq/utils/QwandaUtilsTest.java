package life.genny.test.qwandaq.utils;

import java.util.*;

import javax.inject.Inject;

import life.genny.qwandaq.utils.EntityAttributeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;

import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.utils.testsuite.JUnitTester;

// @QuarkusTest
public class QwandaUtilsTest extends BaseTestCase {

	private static String PER_SOURCE = "PER_SOURCE";
	private static String PER_TARGET = "PER_TARGET";

	private static Attribute QQQ_QUESTION_GRP = new Attribute(Attribute.QQQ_QUESTION_GROUP, "Question Group",
			new DataType(String.class));
	private static Attribute PRI_FIRSTNAME = new Attribute(Attribute.PRI_FIRSTNAME, "First Name",
			new DataType(String.class));
	private static Attribute PRI_LASTNAME = new Attribute(Attribute.PRI_LASTNAME, "Last Name",
			new DataType(String.class));

	static QwandaUtils qwandaUtils;

	@InjectMocks
	static EntityAttributeUtils beaUtilsMock;

	//@BeforeAll
	public static void setup() {
		BaseEntityUtils beUtilsMock = Mockito.mock(BaseEntityUtils.class);
		Mockito.when(beUtilsMock.getBaseEntity(PER_TARGET)).thenReturn(new BaseEntity(PER_TARGET, "Target"));
		QuarkusMock.installMockForType(beUtilsMock, BaseEntityUtils.class);

		qwandaUtils = Mockito.mock(QwandaUtils.class);
		QuarkusMock.installMockForType(qwandaUtils, QwandaUtils.class);

		QuarkusMock.installMockForType(beaUtilsMock, EntityAttributeUtils.class);
	}

	// @Test
	public void testSaveAnswers() {

		Answer a = new Answer(PER_SOURCE, PER_TARGET, Attribute.PRI_FIRSTNAME, "Boris");
		System.out.println("Asserting");
		Assertions.assertDoesNotThrow(() -> qwandaUtils.saveAnswer(a));
	}

	@Test
	public void testBasicAttributeCodeFilter() {

		new JUnitTester<String, Boolean>()
				.setTest((input) -> {
					return Expected(QwandaUtils.attributeCodeMeetsBasicRequirements(input.input));
				})

				.createTest("Exclude Submit")
				.setInput(Attribute.EVT_SUBMIT)
				.setExpected(false)
				.build()

				.createTest("Include First Name")
				.setInput(Attribute.PRI_FIRSTNAME)
				.setExpected(true)
				.build()

				.assertAll();
	}

	@Test
	public void testAskFlatMapBuilder() {

		Set<Ask> asks = new HashSet<>();
		asks.add(buildAskGroup());

		new JUnitTester<Set<Ask>, Boolean>()
				.setTest((input) -> {
					boolean found = true;
					Map<String, Ask> flatMap = QwandaUtils.buildAskFlatMap(input.input);
					for (Ask ask : asks) {
						if (!recursiveAskConfirmation(ask, flatMap)) {
							found = false;
						}
					}
					return Expected(found);
				})
				.createTest("Answered Case")
				.setInput(asks)
				.setExpected(true)
				.build()

				.assertAll();
	}

	@Test
	public void testMandatoryFieldCheck() {

		Set<Ask> asks = new HashSet<>();
		asks.add(buildAskGroup());
		Map<String, Ask> map = QwandaUtils.buildAskFlatMap(asks);

		BaseEntity filled = new BaseEntity(PER_TARGET, PER_TARGET);
		EntityAttribute firstNameEA = new EntityAttribute(filled, PRI_FIRSTNAME, 1.0, "Boris");
		filled.addAttribute(firstNameEA);
		EntityAttribute lastNameEA = new EntityAttribute(filled, PRI_LASTNAME, 1.0, "Yeltson");
		filled.addAttribute(lastNameEA);

		BaseEntity nonFilled = new BaseEntity(PER_TARGET, PER_TARGET);
		EntityAttribute nonFilledFirstNameEA = new EntityAttribute(nonFilled, PRI_FIRSTNAME, 1.0, "Boris");
		nonFilled.addAttribute(nonFilledFirstNameEA);

//		Mockito.when(beaUtilsMock.getEntityAttribute("genny", PER_TARGET, "PRI_FIRSTNAME", true, true)).thenReturn(firstNameEA);
//		Mockito.when(beaUtilsMock.getEntityAttribute("genny", PER_TARGET, "PRI_LASTNAME", true, true)).thenReturn(lastNameEA);
//		Mockito.when(beaUtilsMock.getEntityAttribute("genny", PER_TARGET, "PRI_FIRSTNAME", true, true)).thenReturn(nonFilledFirstNameEA);

		/*new JUnitTester<BaseEntity, Boolean>()
				.setTest((input) -> Expected(qwandaUtils.mandatoryFieldsAreAnswered(map, input.input)))

				.createTest("Answered Case")
				.setInput(filled)
				.setExpected(true)
				.build()

				.createTest("Not Answered Case")
				.setInput(nonFilled)
				.setExpected(false)
				.build()

				.assertAll();*/
	}

	public static Ask buildAskGroup() {

		Question QUE_TEST = new Question("QUE_TEST", "Test", QQQ_QUESTION_GRP);
		Question QUE_FIRSTNAME = new Question("QUE_FIRSTNAME", "FirstName", PRI_FIRSTNAME);
		Question QUE_LASTNAME = new Question("QUE_LASTNAME", "LastName", PRI_LASTNAME);

		Ask test = new Ask(QUE_TEST, PER_SOURCE, PER_TARGET);
		Ask firstName = new Ask(QUE_FIRSTNAME, PER_SOURCE, PER_TARGET);
		firstName.setMandatory(true);
		Ask lastName = new Ask(QUE_LASTNAME, PER_SOURCE, PER_TARGET);
		lastName.setMandatory(true);

		test.add(firstName);
		test.add(lastName);

		return test;
	}

	public static boolean recursiveAskConfirmation(Ask ask, Map<String, Ask> flatMap) {
		if (ask.hasChildren()) {
			for (Ask child : ask.getChildAsks()) {
				if (!recursiveAskConfirmation(child, flatMap)) {
					return false;
				}
			}
		} else if (flatMap.get(ask.getAttributeCode()) == null) {
			return false;
		}
		return true;
	}

}