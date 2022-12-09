package life.genny.test.qwandaq.utils.merge;

import java.util.Map;

import org.junit.jupiter.api.Test;

import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.utils.MergeUtils;
import life.genny.qwandaq.utils.collections.MapBuilder;
import life.genny.qwandaq.utils.testsuite.JUnitTester;
import life.genny.test.qwandaq.utils.BaseTestCase;

public class BaseTests extends BaseTestCase {
    
    @Test
    public void wordMergeTest() {
        new JUnitTester<Map<String, Object>, Object>()
        .setTest((input) -> {
            Filter filter = new Filter("LNK_TENANT", Operator.CONTAINS, "USER");
            return Expected(MergeUtils.wordMerge((String)filter.getValue(), input.input));
        })
        
        .createTest("Word Merge test")
        .setInput(new MapBuilder<String,Object>()
                .add("USER", "PER_TEST")
                .build())
        .setExpected("PER_TEST")
        .build()

        .assertAll();
        

    }
}
