package life.genny.test.qwandaq.json;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.junit.jupiter.api.Test;

import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;

public class JsonTest {

	@Test
	public void pojoSerDeserTest()
	{
		Jsonb jsonb = JsonbBuilder.create();

		// BaseEntity
		BaseEntity baseEntity = new BaseEntity("TST_JSON", "Test Json");
		baseEntity = jsonb.fromJson(jsonb.toJson(baseEntity), BaseEntity.class);

		// SearchEntity
		SearchEntity searchEntity = new SearchEntity("SBE_DEF", "DEF check")
		.addSort("PRI_NAME", "Created", SearchEntity.Sort.ASC)
		.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "DEF_%")
		.addColumn("PRI_CODE", "Name")
		.setPageStart(0).setPageSize(1000);

		searchEntity.setRealm("genny");

		searchEntity = jsonb.fromJson(jsonb.toJson(searchEntity), SearchEntity.class);

		System.out.println("Sucessfully completed POJO SerDeser Test!");
	}
}
