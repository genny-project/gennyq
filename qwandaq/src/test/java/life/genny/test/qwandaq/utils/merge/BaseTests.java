package life.genny.test.qwandaq.utils.merge;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.utils.MergeUtils;
import life.genny.qwandaq.utils.collections.MapBuilder;
import life.genny.qwandaq.utils.testsuite.JUnitTester;
import life.genny.test.qwandaq.utils.BaseTestCase;

import javax.inject.Inject;

@QuarkusTest
public class BaseTests extends BaseTestCase {

    @InjectMocks
    MergeUtils mergeUtils;
    
    @Test
    public void wordMergeTest() {
        new JUnitTester<Map<String, Object>, Object>()
        .setTest((input) -> {
            Filter filter = new Filter("LNK_TENANT", Operator.CONTAINS, "USER");
            return Expected(mergeUtils.wordMerge((String)filter.getValue(), input.input));
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
