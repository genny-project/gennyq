package life.genny.qwandaq.managers.capabilities;

import life.genny.qwandaq.attribute.Attribute;

public class AttributeProductDecorator {
    private final Attribute attribute;
    
    public AttributeProductDecorator(Attribute attribute) {
        this.attribute = attribute;
    }

    public Attribute get(String productCode) {
        attribute.setRealm(productCode);
        return attribute;
    }
}
