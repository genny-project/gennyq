package life.genny.qwandaq.utils.filter;

import life.genny.qwandaq.utils.SearchUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;


@SessionScoped
public class FilterParamsUtils {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    SearchUtils searchUtils;

    private Map<String, String> filterParams = new HashMap<>();
    private Map<String,Map<String, String>> listFilterParams = new HashMap<>();

    /**
     *
     * @return Filter Parameters in the application scope
     */
    public Map<String, String> getFilterParams() {
        return filterParams;
    }

    /**
     * Set Filter Pamameters in application scope
     * @param filterParams
     */
    public void setFilterParams(Map<String, String> filterParams) {
        this.filterParams = filterParams;
    }

    /**
     * Set parameter value by key
     * @param key Parameter Key
     * @param value Parameter Value
     */
    public void setFilterParamValByKey(String key, String value) {
        filterParams.put(key, value);
    }

    /**
     * Get parameter value by key
     * @param key Parameter Key
     */
    public String getFilterParamValByKey(String key) {
        return searchUtils.getFilterParamValByKey(filterParams,key);
    }

    /**
     * Clone filter parameter
     * @return Clone of filter parameter
     */
    public Map<String, String> getCloneFilterParams() {
        Map<String, String> newMap = new HashMap<>();

        filterParams.entrySet().stream().forEach( e-> {
            newMap.put(e.getKey(),e.getValue());
        });
        return newMap;
    }




}
