package life.genny.qwandaq.serialization.common;

public abstract class CoreEntityKey implements CoreEntityKeyIntf {

	protected String productCode;

    // no-arg constructor
    @Deprecated
    public CoreEntityKey() { }

    public CoreEntityKey(String productCode) {
        this.productCode = productCode;
    }

    @Override
    public String getProductCode() {
        return productCode;
    }
	
	@Override
	public String toString() {
		return getKeyString();
	}
    
}
