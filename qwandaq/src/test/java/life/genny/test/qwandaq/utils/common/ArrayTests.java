package life.genny.test.qwandaq.utils.common;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import life.genny.qwandaq.utils.CommonUtils;
import life.genny.test.qwandaq.utils.BaseTestCase;

public class ArrayTests extends BaseTestCase {
    
    @Test
    public void mapArray() {
        String[] nums = {"0", "1", "2", "3"};
        Integer[] numInts = CommonUtils.mapArray(nums, Integer.class, Integer::parseInt);
        assertArrayEquals(new Integer[] {0, 1, 2, 3}, numInts);
    }
    
}
