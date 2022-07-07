package life.genny.qwandaq.serialization.common.key.cache;

import life.genny.qwandaq.serialization.common.CacheKeyIntf;

public class CacheKey implements CacheKeyIntf {

	private String productCode;
    protected String key;

    // no-arg constructor
    @Deprecated
    public CacheKey() { }

    public CacheKey(String productCode) {
        this.productCode = productCode;
    }

    public CacheKey(String productCode, String key) {
        this.productCode = productCode;
        this.key = key;
    }

    @Override
    public CacheKeyIntf fromKey(String key) {
        String[] components = getComponents();
        return new CacheKey(components[0], components[1]);
    }

    @Override
    public String getProductCode() {
        return productCode;
    }
    
	@Override
	public final String toString() {
		return getFullKeyString();
	}
    
    @Override
    public final String getFullKeyString() {
        return getBaseKeyString() + key;
    }

}
