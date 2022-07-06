package life.genny.qwandaq.serialization.key.baseentityattribute;

import life.genny.qwandaq.serialization.common.CoreEntityKey;

public class BaseEntityAttributeKey extends CoreEntityKey {

    private String baseEntityCode;
    private String attributeCode;

    // no-arg constructor
    @Deprecated
    public BaseEntityAttributeKey() { }

    public BaseEntityAttributeKey(String productCode, String baseEntityCode, String attributeCode) {
        super(productCode);
        this.baseEntityCode = baseEntityCode;
        this.attributeCode = attributeCode;
    }

    @Override
    public String getKeyString() {
        return productCode + getDelimiter() + getBaseEntityCode() + getDelimiter() + getAttributeCode();
    }

    @Override
    public BaseEntityAttributeKey fromKey(String key) {
        String[] components = key.split(getDelimiter());
        return new BaseEntityAttributeKey(components[0], components[1], components[2]);
    }

    public String getEntityCode() {
        return baseEntityCode;
    }

    @Override
    public String toString() {
        return getKeyString();
    }
    
    // Getters and Setters
    public String getBaseEntityCode() {
        return this.baseEntityCode;
    }
    public String getAttributeCode() {
        return this.attributeCode;
    }
}
