package life.genny.qwandaq.constants;

import java.util.HashMap;
import java.util.Map;

public enum ECacheRef {
    BASEENTITY("baseentity"),
    BASEENTITY_ATTRIBUTE("baseentity_attribute", false),
    ATTRIBUTE("attribute"),
    QUESTION("question"),
    QUESTIONQUESTION("questionquestion"),
    DATATYPE("datatype"),
    VALIDATION("validation"),
    ENTITY_LAST_UPDATED_AT("table_last_updated_at"),
    USERSTORE("userstore");

    private static final Map<String, ECacheRef> nameLookup = new HashMap<>();
    static {
        for(ECacheRef cache : values()) {
            nameLookup.put(cache.cacheName, cache);
        }
    }

    public final String cacheName;
    public final boolean reindexable;

    private ECacheRef(String cacheName) {
        this(cacheName, true);
    }

    private ECacheRef(String cacheName, boolean reindexable) {
        this.cacheName = cacheName;
        this.reindexable = reindexable;
    }

    public static ECacheRef getByCacheName(String cacheName) {
        return nameLookup.get(cacheName);
    }
}
