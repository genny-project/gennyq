package life.genny.test.utils;

import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusMock;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;

public class GennyTest {

	public static String PRODUCT = "genny";
	public static String ENTITY_CODE = "TST_ENTITY";
	public static String DUMMY_CODE = "DUMMY";

	public static void mockUserToken()
	{
		UserToken mock = Mockito.mock(UserToken.class);
		Mockito.when(mock.getProductCode()).thenReturn(PRODUCT);
		QuarkusMock.installMockForType(mock, UserToken.class);
	}

	public static void mockDatabaseUtils()
	{
		System.out.println("1");
		DatabaseUtils mock = Mockito.mock(DatabaseUtils.class);

		System.out.println("2");
		BaseEntity baseEntity = new BaseEntity(ENTITY_CODE, "Test Entity");
		baseEntity.setRealm(PRODUCT);

		System.out.println("3");

		Mockito.when(mock.findBaseEntityByCode(PRODUCT, ENTITY_CODE)).thenReturn(baseEntity);
		Mockito.when(mock.findBaseEntityByCode(PRODUCT, DUMMY_CODE)).thenReturn(null);

		System.out.println("4");
		QuarkusMock.installMockForType(mock, DatabaseUtils.class);
		System.out.println("5");
	}

	public static void mockBaseEntityUtils()
	{
		BaseEntityUtils mock = Mockito.mock(BaseEntityUtils.class);
		QuarkusMock.installMockForType(mock, BaseEntityUtils.class);
	}

}
