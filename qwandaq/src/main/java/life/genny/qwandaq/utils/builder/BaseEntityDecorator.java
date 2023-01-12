package life.genny.qwandaq.utils.builder;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.entity.BaseEntity;

public class BaseEntityDecorator {
    
    private BaseEntity be;

    public BaseEntityDecorator(String code, String name) {
        be = new BaseEntity(code, name);
    }

    public EntityAttributeBuilder addEA(Attribute attribute) {
        EntityAttributeBuilder eaBuilder = new EntityAttributeBuilder(this);
        if(attribute != null) {
            eaBuilder.setAttribute(attribute);
        }

        return eaBuilder;
    }

    public EntityAttributeBuilder addEA() {
        return addEA(null);
    }

    public BaseEntity getBaseEntity() {
        return be;
    }

}
