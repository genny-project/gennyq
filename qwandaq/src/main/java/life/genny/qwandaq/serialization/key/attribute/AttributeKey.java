package life.genny.qwandaq.serialization.key.attribute;

import life.genny.qwandaq.serialization.common.key.core.CoreEntityKey;

public class AttributeKey extends CoreEntityKey {

    private String attributeCode;

    // no-arg constructor
    @Deprecated
    public AttributeKey() {}

    public AttributeKey(String productCode, String attributeCode) {
        super(productCode);
        this.attributeCode = attributeCode;
    }

    @Override
    public AttributeKey fromKey(String key) {
        String[] components = getComponents(key);
        return new AttributeKey(components[0], components[1]);
    }

    @Override
    public String getEntityCode() {
        return attributeCode;
    }
    
}
