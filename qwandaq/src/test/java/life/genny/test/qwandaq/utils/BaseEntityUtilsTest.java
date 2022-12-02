package life.genny.test.qwandaq.utils;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;

import io.quarkus.test.junit.mockito.InjectMock;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;

// @RunWith(MockitoJUnitRunner.class)
public class BaseEntityUtilsTest extends BaseTestCase {

	static Jsonb jsonb = JsonbBuilder.create();

	private static String PRODUCT = "genny";
	private static String ENTITY_CODE = "TST_ENTITY";
	private static String DUMMY_CODE = "DUMMY";

	@InjectMock
	UserToken userToken;

	@InjectMocks
	BaseEntityUtils beUtils;

	@InjectMocks
	DatabaseUtils dbUtils;

	@Test
	public void nullInputTest() {
		// TODO
	}

	// @Test
	public void getBaseEntityTest() {
		
        DatabaseUtils dbUtils = Mockito.mock(DatabaseUtils.class);
        BaseEntityUtils beUtils = Mockito.mock(BaseEntityUtils.class);

		BaseEntity baseEntity = new BaseEntity(ENTITY_CODE, "Test Entity");
		baseEntity.setRealm(PRODUCT);
		
		Mockito.when(dbUtils.findBaseEntityByCode(PRODUCT, ENTITY_CODE)).thenReturn(baseEntity);
		Mockito.when(dbUtils.findBaseEntityByCode(PRODUCT, DUMMY_CODE)).thenReturn(null);

		assert(baseEntity.equals(beUtils.getBaseEntity(PRODUCT, ENTITY_CODE)));
		Assertions.assertThrows(ItemNotFoundException.class, () -> beUtils.getBaseEntity(PRODUCT, DUMMY_CODE));
	}

	public void cleanAttributeValueTest() {

		assert(beUtils.cleanUpAttributeValue("[\"SEL_ONE\"]").equals("SEL_ONE"));
		assert(beUtils.cleanUpAttributeValue("[\"SEL_ONE\", \"SEL_TWO\"]").equals("SEL_ONE,SEL_TWO"));
		assert(beUtils.cleanUpAttributeValue("[\"SEL_ONE\", \"SEL_TWO\", \"SEL_THREE\"]").equals("SEL_ONE,SEL_TWO,SEL_THREE"));
	}

	// TODO: Get Code Array


	// TODO: Create BaseEntity Test

	// TODO: Update BaseEntity Test

	// TODO: Privacy Filter Test
}
