package life.genny.qwandaq.serialization.common.key.cache;

import life.genny.qwandaq.serialization.common.CacheEntityKeyIntf;

public class CacheEntityKey implements CacheEntityKeyIntf {

	private String productCode;
    protected String key;

    // no-arg constructor
    @Deprecated
    public CacheEntityKey() { }

    public CacheEntityKey(String productCode) {
        this.productCode = productCode;
    }

    public CacheEntityKey(String productCode, String key) {
        this.productCode = productCode;
        this.key = key;
    }

    @Override
    public CacheEntityKeyIntf fromKey(String key) {
        String[] components = getComponents();
        return new CacheEntityKey(components[0], components[1]);
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
