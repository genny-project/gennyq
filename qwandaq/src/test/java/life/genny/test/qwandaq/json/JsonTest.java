package life.genny.test.qwandaq.json;

import static org.junit.Assert.assertTrue;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.junit.jupiter.api.Test;

import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.entity.search.trait.Column;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.entity.search.trait.Ord;
import life.genny.qwandaq.entity.search.trait.Sort;

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
			.add(new Sort("PRI_NAME", Ord.ASC))
			.add(new Filter("PRI_CODE", Operator.LIKE, "DEF_%"))
			.add(new Column("PRI_CODE", "Name"))
			.setPageStart(0).setPageSize(1000);

		searchEntity.setRealm("genny");
		assert(searchEntity.equals(serializeDeserialize(searchEntity, BaseEntity.class)));
	}
}
