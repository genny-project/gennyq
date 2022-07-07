package life.genny.qwandaq.serialization.common.key.core;

import life.genny.qwandaq.serialization.common.key.cache.CacheKey;

public abstract class CoreEntityKey extends CacheKey {

    // no-arg constructor
    @Deprecated
    public CoreEntityKey() { }

    public CoreEntityKey(String productCode) {
        super(productCode);
    }

    public CoreEntityKey(String productCode, String key) {
        super(productCode, key);
    }

    public abstract String getEntityCode();
}
