package life.genny.qwandaq.serialization.key.baseentityattribute;

import life.genny.qwandaq.serialization.common.key.core.CoreEntityKey;

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
    public BaseEntityAttributeKey fromKey(String key) {
        String[] components = key.split(getDelimiter());
        return new BaseEntityAttributeKey(components[0], components[1], components[2]);
    }
    
    // Getters and Setters
    public String getAttributeCode() {
        return this.attributeCode;
    }

    public String getBaseEntityCode() {
        return this.baseEntityCode;
    }

    @Override
    //TODO: Think about this some more
    public String getEntityCode() {
        return baseEntityCode + getDelimiter() + attributeCode;
    }
}
