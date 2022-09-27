package life.genny.test.qwandaq.json;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.junit.jupiter.api.Test;

import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;

public class JsonTest {

	static Jsonb jsonb = JsonbBuilder.create();

	private <T> T serializeDeserialize(Object o, Class<T> c) {
		return jsonb.fromJson(jsonb.toJson(o), c);
	}

	@Test
	public void baseEntityTest()
	{
		BaseEntity baseEntity = new BaseEntity("TST_JSON", "Test Json");
		assert(baseEntity.equals(serializeDeserialize(baseEntity, BaseEntity.class)));
	}

	@Test
	public void searchEntityTest()
	{
		SearchEntity searchEntity = new SearchEntity("SBE_DEF", "DEF check")
		.addSort(Attribute.PRI_NAME, "Created", Sort.ASC)
		.add(new Filter(Attribute.PRI_CODE, Filter.LIKE, "DEF_%"))
		.add(new Column(Attribute.PRI_CODE, "Name"))
		.setPageStart(0).setPageSize(1000);

		searchEntity.setRealm("genny");
		assert(searchEntity.equals(serializeDeserialize(searchEntity, BaseEntity.class)));
	}
}
