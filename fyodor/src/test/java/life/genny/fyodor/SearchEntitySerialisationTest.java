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
        .add(new Filter("LNK_DEF", Operator.CONTAINS, DEF_APPLICATION))
        .add(new AssociatedColumn("LNK_PROPERTY", "PRI_NAME", "Property Heading"))
        .add(new Column("PRI_CREATED", "Date Submitted"))
        .add(new AssociatedColumn("LNK_TENANT", "PRI_NAME", "Tenant Name"))
        .add(new AssociatedColumn("LNK_TENANT", "PRI_EMAIL", "Tenant Email"))
        .add(new AssociatedColumn("LNK_TENANT", "PRI_MOBILE", "Tenant Mobile"))
        .add(new Column("PRI_APPROVED", "Approved"))
        .add(new AssociatedColumn("LNK_PROPERTY", "PRI_NAME", "Applied"))
        .add(new Action("VIEW", "View"))
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
