package life.genny.qwandaq.utils.builder;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.entity.Definition;

public class DefinitionDecorator {
    
    private Definition be;

    public DefinitionDecorator(String code, String name) {
        this(new Definition(code, name));
    }

    public DefinitionDecorator(Definition be) {
        this.be = be;
    }


    public DefEntityAttributeBuilder addEA(Attribute attribute) {
        DefEntityAttributeBuilder eaBuilder = new DefEntityAttributeBuilder(this);
        if(attribute != null) {
            eaBuilder.setAttribute(attribute);
        }

        return eaBuilder;
    }

    public DefEntityAttributeBuilder addEA() {
        return addEA(null);
    }

    public Definition getDefinition() {
        return (Definition)be;
    }

}
