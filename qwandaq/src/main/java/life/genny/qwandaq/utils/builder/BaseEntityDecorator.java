package life.genny.qwandaq.utils.builder;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.entity.BaseEntity;

public class BaseEntityDecorator {
    
    protected BaseEntity be;

    public BaseEntityDecorator(String code, String name) {
        this(new BaseEntity(code, name));
    }

    public BaseEntityDecorator(BaseEntity be) {
        this.be = be;
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
