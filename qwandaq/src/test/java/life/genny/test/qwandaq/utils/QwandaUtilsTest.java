package life.genny.test.qwandaq.utils;

import static life.genny.qwandaq.entity.BaseEntity.PER_TARGET;
import static life.genny.qwandaq.entity.Definition.DEF_PERSON;
import static org.mockito.ArgumentMatchers.any;
import static life.genny.qwandaq.attribute.Attribute.LNK_DEF;
import static life.genny.qwandaq.entity.BaseEntity.PER_SOURCE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
import life.genny.qwandaq.entity.Definition;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.utils.testsuite.JUnitTester;

@QuarkusTest
public class QwandaUtilsTest extends BaseTestCase {

	private static Attribute QQQ_QUESTION_GRP = new Attribute(Attribute.QQQ_QUESTION_GROUP, "Question Group",
			new DataType(String.class));
	private static Attribute PRI_FIRSTNAME = new Attribute(Attribute.PRI_FIRSTNAME, "First Name",
			new DataType(String.class));
	private static Attribute PRI_LASTNAME = new Attribute(Attribute.PRI_LASTNAME, "Last Name",
			new DataType(String.class));

	@Inject
	QwandaUtils qwandaUtils;

	@Test
	public void testSaveAnswers() {

		// mock BaseEntityUtils
		BaseEntityUtils beUtilsMock = Mockito.mock(BaseEntityUtils.class);

		// mock get target entity
		BaseEntity target = new BaseEntity(PER_TARGET, "Target");
		Mockito.when(beUtilsMock.getBaseEntity(PER_TARGET)).thenReturn(target);

		// mock getting entity def codes
		Mockito.when(beUtilsMock.getBaseEntityCodeArrayFromLinkAttribute(target, LNK_DEF)).thenReturn(Arrays.asList(DEF_PERSON));

		// mock get def
		Definition defPerson = new Definition(DEF_PERSON, "Person");
		defPerson.addAttribute(new Attribute("ATT_PRI_FIRSTNAME", "FirstName", new DataType(Boolean.class)), 1.0, false);
		Mockito.when(beUtilsMock.getDefinition(DEF_PERSON)).thenReturn(defPerson);

		// install mock
		QuarkusMock.installMockForType(beUtilsMock, BaseEntityUtils.class);

		Answer a = new Answer(PER_SOURCE, PER_TARGET, Attribute.PRI_FIRSTNAME, "Boris");
		// Assertions.assertDoesNotThrow(() -> qwandaUtils.saveAnswer(a));
		Answer b = new Answer(PER_SOURCE, PER_TARGET, Attribute.PRI_CREATED, "NotADateTime");
		// Assertions.assertThrows(BadDataException.class, () -> qwandaUtils.saveAnswer(b));
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

		List<Ask> asks = new ArrayList<>();
		asks.add(buildAskGroup());

		new JUnitTester<List<Ask>, Boolean>()
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

		List<Ask> asks = new ArrayList<>();
		asks.add(buildAskGroup());
		Map<String, Ask> map = QwandaUtils.buildAskFlatMap(asks);

		BaseEntity filled = new BaseEntity(PER_TARGET);
		filled.addAttribute(new EntityAttribute(filled, PRI_FIRSTNAME, 1.0, "Boris"));
		filled.addAttribute(new EntityAttribute(filled, PRI_LASTNAME, 1.0, "Yeltson"));

		BaseEntity nonFilled = new BaseEntity(PER_TARGET);
		nonFilled.addAttribute(new EntityAttribute(nonFilled, PRI_FIRSTNAME, 1.0, "Boris"));

		new JUnitTester<BaseEntity, Boolean>()
				.setTest((input) -> {
					return Expected(QwandaUtils.mandatoryFieldsAreAnswered(map, input.input));
				})

				.createTest("Answered Case")
				.setInput(filled)
				.setExpected(true)
				.build()

				.createTest("Not Answered Case")
				.setInput(nonFilled)
				.setExpected(false)
				.build()

				.assertAll();
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
