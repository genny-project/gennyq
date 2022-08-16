package life.genny.test.qwandaq.utils;

import static life.genny.test.utils.GennyTest.PRODUCT;
import static life.genny.test.utils.GennyTest.ENTITY_CODE;
import static life.genny.test.utils.GennyTest.DUMMY_CODE;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.test.utils.GennyTest;

// @QuarkusTest
public class BaseEntityUtilsTest {

	static Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	BaseEntityUtils beUtils;

	// @BeforeEach
	public void setup()
	{
		GennyTest.mockUserToken();
		GennyTest.mockDatabaseUtils();
		GennyTest.mockBaseEntityUtils();
	}

	// @Test
	public void getBaseEntityTest() 
	{
		BaseEntity baseEntity = new BaseEntity(ENTITY_CODE, "Test Entity");
		baseEntity.setRealm(PRODUCT);
		assert(beUtils.getBaseEntity(ENTITY_CODE).getCode().equals(ENTITY_CODE));
		Assertions.assertThrows(ItemNotFoundException.class, () -> beUtils.getBaseEntity(DUMMY_CODE));
		Assertions.assertThrows(NullParameterException.class, () -> beUtils.getBaseEntity(null));
	}

	// @Test
	public void cleanAttributeValueTest() 
	{
		assert(BaseEntityUtils.cleanUpAttributeValue("[\"SEL_ONE\"]").equals("SEL_ONE"));
		assert(BaseEntityUtils.cleanUpAttributeValue("[\"SEL_ONE\", \"SEL_TWO\"]").equals("SEL_ONE,SEL_TWO"));
		assert(BaseEntityUtils.cleanUpAttributeValue("[\"SEL_ONE\", \"SEL_TWO\", \"SEL_THREE\"]").equals("SEL_ONE,SEL_TWO,SEL_THREE"));
	}

	// TODO: Get Code Array

	// TODO: Create BaseEntity Test

	// TODO: Update BaseEntity Test

	// TODO: Privacy Filter Test
}
