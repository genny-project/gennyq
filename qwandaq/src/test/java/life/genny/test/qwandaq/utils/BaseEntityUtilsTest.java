package life.genny.test.qwandaq.utils;

import io.quarkus.test.junit.mockito.InjectMock;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.testsuite.JUnitTester;

// @RunWith(MockitoJUnitRunner.class)
public class BaseEntityUtilsTest extends BaseTestCase {

	static Jsonb jsonb = JsonbBuilder.create();

	private static final String PRODUCT = "genny";
	private static final String ENTITY_CODE = "TST_ENTITY";
	private static final String DUMMY_CODE = "DUMMY";

	@InjectMock
	UserToken userToken;

	@InjectMocks
	BaseEntityUtils beUtils;

	@InjectMocks
	DatabaseUtils dbUtils;

	// @Test
	public void nullInputTest() {
		// TODO
	}

	// @Test
	public void getBaseEntityTest() {
		
        DatabaseUtils dbUtils = Mockito.mock(DatabaseUtils.class);
        BaseEntityUtils beUtils = Mockito.mock(BaseEntityUtils.class);

		BaseEntity baseEntity = new BaseEntity(ENTITY_CODE, "Test Entity");
		baseEntity.setRealm(PRODUCT);
		
		Mockito.when(beUtils.getBaseEntity(PRODUCT, ENTITY_CODE)).thenReturn(baseEntity);
		Mockito.when(beUtils.getBaseEntity(PRODUCT, DUMMY_CODE)).thenReturn(null);

		assert(baseEntity.equals(beUtils.getBaseEntity(PRODUCT, ENTITY_CODE)));
		Assertions.assertThrows(ItemNotFoundException.class, () -> beUtils.getBaseEntity(PRODUCT, DUMMY_CODE));
	}

	public void cleanAttributeValueTest() {

        new JUnitTester<String, String>()
            .setTest((input) -> {
                return Expected(CommonUtils.cleanUpAttributeValue(input.input));
            })
            
            .createTest("Clean Value 1")
            .setInput("[\"SEL_ONE\"]")
            .setExpected("SEL_ONE")
            .build()

            .createTest("Clean Value 2")
            .setInput("[\"SEL_ONE\", \"SEL_TWO\"]")
            .setExpected("SEL_ONE,SEL_TWO")
            .build()

            .createTest("Clean Value 3")
            .setInput("[\"SEL_ONE\", \"SEL_TWO\", \"SEL_THREE\"]")
            .setExpected("SEL_ONE,SEL_TWO,SEL_THREE")
            .build()

            .assertAll();
	}

	// TODO: Get Code Array

	// TODO: Create BaseEntity Test

	// TODO: Update BaseEntity Test

	// TODO: Privacy Filter Test
}
