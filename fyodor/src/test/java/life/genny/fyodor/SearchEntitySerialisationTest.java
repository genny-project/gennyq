package life.genny.fyodor;

import org.junit.jupiter.api.Test;

import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.entity.search.trait.Action;
import life.genny.qwandaq.entity.search.trait.AssociatedColumn;
import life.genny.qwandaq.entity.search.trait.Column;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.utils.CommonUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

public class SearchEntitySerialisationTest {
	public static final String SBE_TABLE_APPLICATIONS = "SBE_TABLE_APPLICATIONS";
	public static final String DEF_APPLICATION = "DEF_APPLICATION";
    
    static Jsonb jsonb = JsonbBuilder.create();

    @Test
    public void serialiseTrait() {

        SearchEntity entity = new SearchEntity(SBE_TABLE_APPLICATIONS, "Applications")
        .add(new Filter("LNK_DEF", Operator.CONTAINS, "DEF_TEST"))
        .add(new AssociatedColumn("LNK_TEST1", "PRI_NAME", "Assc. column 1"))
        .add(new Column("PRI_CREATED", "Some date here"))
        .add(new AssociatedColumn("LNK_TEST2", "PRI_NAME", "Name"))
        .add(new AssociatedColumn("LNK_TEST3", "PRI_EMAIL", "Email"))
        .add(new AssociatedColumn("LNK_TEST4", "PRI_MOBILE", "Mobile"))
        .add(new Column("PRI_ATTRIB1", "Some attribute"))
        .add(new AssociatedColumn("LNK_TEST5", "PRI_NAME", "Some name"))
        .add(new Action("VIEW", "View Button"))
        .setPageSize(20)
        .setPageStart(0);
        System.out.println("Entity AC: " + entity.getTraitMap().getList(AssociatedColumn.class).size());
        System.out.println("Entity Attributes: " + entity.getBaseEntityAttributes().size());
        entity = entity.convertToSendable();
        System.out.println("Post sendable Entity AC: " + entity.getTraitMap().getList(AssociatedColumn.class).size());
        System.out.println("Entity Attributes: " + entity.getBaseEntityAttributes().size());
        System.out.println("===============================================");
        CommonUtils.printCollection(entity.getBaseEntityAttributes());

        String json = jsonb.toJson(entity);
        // System.out.println("JSOON: " + json);

        SearchEntity entity2 = jsonb.fromJson(json, SearchEntity.class);
        System.out.println("Post Serializable Entity AC: " + entity.getTraitMap().getList(AssociatedColumn.class).size());

        System.out.println("Entity Attributes: " + entity2.getBaseEntityAttributes().size());
        System.out.println("===============================================");
        CommonUtils.printCollection(entity2.getBaseEntityAttributes());

        assertEquals(entity, entity2);
    }
}
