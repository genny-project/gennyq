package life.genny.fyodor;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.junit.jupiter.api.Test;

import life.genny.qwandaq.datatype.capability.core.CapabilityBuilder;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.entity.search.TraitMap;
import life.genny.qwandaq.entity.search.trait.Action;
import life.genny.qwandaq.entity.search.trait.AssociatedColumn;
import life.genny.qwandaq.entity.search.trait.Column;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.entity.search.trait.Trait;
import life.genny.qwandaq.utils.CommonUtils;

import static life.genny.qwandaq.datatype.capability.core.node.PermissionMode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

public class TraitMapSerialisationTest {
	public static final String SBE_TABLE_APPLICATIONS = "SBE_TABLE_APPLICATIONS";
	public static final String DEF_APPLICATION = "DEF_APPLICATION";
    
    static Jsonb jsonb = JsonbBuilder.create();

    @Test
    public void test() {

        SearchEntity entity = new SearchEntity(SBE_TABLE_APPLICATIONS, "Applications")
        .add(new Filter("LNK_DEF", Operator.CONTAINS, "DEF_TEST")
            .addCapabilityRequirement(CapabilityBuilder.code("CAP_TEST").add(ALL).buildCap()))
        .add(new AssociatedColumn("LNK_TEST1", "PRI_NAME", "Assc. column 1"))
        .add(new Column("PRI_CREATED", "Some date here"))
        .add(new AssociatedColumn("LNK_TEST2", "PRI_NAME", "Name"))
        .add(new AssociatedColumn("LNK_TEST3", "PRI_EMAIL", "Email"))
        .add(new AssociatedColumn("LNK_TEST4", "PRI_MOBILE", "Mobile"))
        .add(new Column("PRI_ATTRIB1", "Some attribute"))
        .add(new AssociatedColumn("LNK_TEST5", "PRI_NAME", "Some name"))
        .add(new Action("VIEW", "View Button"));

        TraitMap map = entity.getTraitMap();

        CommonUtils.printMap(map);

        String json = jsonb.toJson(map);
        System.out.println(json);
        
        TraitMap map2 = jsonb.fromJson(json, TraitMap.class);
        List<Action> acts = map2.get(Action.class);
        CommonUtils.printMap(map2);
        // assertEquals(map, map2);
    }

    @Test
    public void testTrait() {

        Trait t = new Filter("LNK_DEF", Operator.CONTAINS, "DEF_TEST")
            .addCapabilityRequirement(CapabilityBuilder.code("CAP_TEST").add(ALL).buildCap());

        String json = jsonb.toJson(t);
        System.out.println(json);        
    }
}
