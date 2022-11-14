package life.genny.test.qwandaq.utils;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.Table;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import life.genny.qwandaq.entity.BaseEntity;

@RunWith(MockitoJUnitRunner.class)
public class DatabaseUtilsTest {

	static Jsonb jsonb = JsonbBuilder.create();

	@Test
	public void baseEntityTest()
	{
		Object filterable = new BaseEntity("blah", "blah");
        Table tableAnnotation = filterable.getClass().getAnnotation(Table.class);
        System.out.println("Table name: " + tableAnnotation.name());
	}

}
