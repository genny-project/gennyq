package life.genny.fyodor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.entity.search.trait.Action;
import life.genny.qwandaq.entity.search.trait.AssociatedColumn;
import life.genny.qwandaq.entity.search.trait.Column;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Sort;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.entity.search.trait.Ord;

public class SearchEntitySerialisationTest {
	public static final String SBE_TABLE_APPLICATIONS = "SBE_TABLE_APPLICATIONS";
	public static final String DEF_APPLICATION = "DEF_APPLICATION";
    
    static Jsonb jsonb = JsonbBuilder.create();

    //TODO: Turn these into proper tests

    static SearchEntity entity = new SearchEntity(SBE_TABLE_APPLICATIONS, "Applications")
    .add(new Filter("LNK_TEST_LINK", Operator.CONTAINS, "DEF_TEST"))
    .add(new AssociatedColumn("LNK_TEST1", "PRI_DUMMY_ATTRIBUTE_NINE_THOUSAND", "Assc. column 1"))
    .add(new Column("PRI_ATTRIB_TEST_NO_CONSTANT", "Some value here"))
    .add(new AssociatedColumn("LNK_TEST2", "PRI_DUMMY_ATTRIBUTE", "Funny Attribute Name"))
    .add(new AssociatedColumn("LNK_TEST3", "PRI_GRRR", "Email"))
    .add(new AssociatedColumn("LNK_TEST4", "PRI_THIS_IS_A_TEST_ATTRIB", "Dan"))
    .add(new Sort("SRT_TESTSORT", Ord.ASC))
    .add(new Column("PRI_ATTRIB1", "Some attribute"))
    .add(new AssociatedColumn("LNK_TEST5", "PRI_JOE_ATTRIBUTE", "Joe"))
    .add(new Action("TEST_ACTION_THINGY", "ACTION THINGY"))
    .add(new Action("TEST_ACTION_THINGY2", "ACTION erefef"))
    .setPageSize(20)
    .setPageStart(0);

    // @Test
    public void serialiseTrait() {
        String json = jsonb.toJson(entity);
        SearchEntity entity2 = jsonb.fromJson(json, SearchEntity.class);
        json = jsonb.toJson(entity2);
        // System.out.println(json);
        assertEquals(entity, entity2);

        // entity.convertToSendable();
    }

    // @Test
    public void flipOrdTest() {
        Sort s = entity.getTrait(Sort.class, "SRT_TESTSORT").get();
        System.out.println(s);
        s.flipOrd();
        System.out.println(s);
        System.out.println(entity.getTraitMap()); 
    }
}
