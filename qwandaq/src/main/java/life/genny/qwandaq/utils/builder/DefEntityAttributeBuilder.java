package life.genny.qwandaq.utils.builder;

import java.util.HashSet;
import java.util.Set;

import org.jboss.logging.Logger;

import life.genny.qwandaq.Answer;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.Definition;
import life.genny.qwandaq.exception.runtime.NullParameterException;

public class DefEntityAttributeBuilder {
    private static final Logger log = Logger.getLogger(DefEntityAttributeBuilder.class);

    private static final BaseEntity TESTING_BASE_ENTITY = new BaseEntity("TST_UNIT", "Unit Test BE");

    private DefinitionDecorator defDecorator;
    private Definition def;
    private String value;
    private Attribute attribute;

    private Set<Capability> capabilityRequirements = new HashSet<Capability>();

    public DefEntityAttributeBuilder(Definition be) {
        this.def = be;
    }

    public DefEntityAttributeBuilder(DefinitionDecorator defDecorator) {
        this(defDecorator.getDefinition());
        this.defDecorator = defDecorator;
    }

    public DefEntityAttributeBuilder setValue(String value) {
        this.value = value;
        return this;
    }

    public DefEntityAttributeBuilder setDefinition(Definition be) {
        this.def = be;
        return this;
    }

    public DefEntityAttributeBuilder setAttribute(Attribute attribute) {
        this.attribute = attribute;
        return this;
    }

    public DefEntityAttributeBuilder addRequirement(Capability capability) {
        this.capabilityRequirements.add(capability);
        return this;
    }

    public DefEntityAttributeBuilder setAttribute(String code, String name, DataType dataType) {
        this.attribute = new Attribute(code, name, dataType);
        return this;
    }

    public EntityAttribute buildEA() {
        if(def == null)
            throw new NullParameterException("BaseEntity in EntityAttributeBuilder");
        return buildEA(def);
    }

    public EntityAttribute buildEA(Definition be) {
        if(attribute == null)
            throw new NullParameterException("Attribute in EntityAttributeBuilder");
        if(value == null)
            throw new NullParameterException("value assigned to EntityAttribute in EntityAttributeBuilder");

        // Conforming to answer mechanism
        Answer answer = new Answer(TESTING_BASE_ENTITY, be, attribute, (String)value);
        be.addAnswer(answer);

        // Set requirements
        EntityAttribute ea = be.findEntityAttribute(attribute).get();
        ea.setCapabilityRequirements(capabilityRequirements);
        return ea;
    }


    public DefinitionDecorator build() {
        if(defDecorator == null) {
            log.error("Not inside a DefinitionDecoratora. call DefEntityAttributeBuilder.buildEA() here instead");
            throw new NullParameterException("DefinitionDecoratora");
        }

        if(defDecorator.getDefinition() == null)
            throw new NullParameterException("BaseEntity in DefinitionDecoratora");
        if(attribute == null)
            throw new NullParameterException("Attribute in DefEntityAttributeBuilder");
        if(value == null)
            throw new NullParameterException("value assigned to EntityAttribute in DefEntityAttributeBuilder");

        buildEA(defDecorator.getDefinition());
        return defDecorator;
    }
    
}
