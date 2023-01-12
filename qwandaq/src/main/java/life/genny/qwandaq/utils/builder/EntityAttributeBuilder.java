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
import life.genny.qwandaq.exception.runtime.NullParameterException;

/**
 * Class to assist with testing
 * 
 * @author Bryn Meachem
 */
public class EntityAttributeBuilder {
    private static final Logger log = Logger.getLogger(EntityAttributeBuilder.class);

    private static final BaseEntity TESTING_BASE_ENTITY;
    static {
        TESTING_BASE_ENTITY = new BaseEntity("TST_UNIT", "Unit Test BE");
    }
    
    private BaseEntityDecorator beBuilder;
    private BaseEntity be;
    private String value;
    private Attribute attribute;

    private Set<Capability> capabilityRequirements = new HashSet<Capability>();

    public EntityAttributeBuilder(BaseEntity be) {
        this.be = be;
    }

    public EntityAttributeBuilder(BaseEntityDecorator beBuilder) {
        this.beBuilder = beBuilder;
        this.be = beBuilder.getBaseEntity();
    }

    public EntityAttributeBuilder setValue(String value) {
        this.value = value;
        return this;
    }

    public EntityAttributeBuilder setBaseEntity(BaseEntity be) {
        this.be = be;
        return this;
    }

    public EntityAttributeBuilder setAttribute(Attribute attribute) {
        this.attribute = attribute;
        return this;
    }

    public EntityAttributeBuilder addRequirement(Capability capability) {
        this.capabilityRequirements.add(capability);
        return this;
    }

    public EntityAttributeBuilder setAttribute(String code, String name, DataType dataType) {
        this.attribute = new Attribute(code, name, dataType);
        return this;
    }

    public EntityAttribute buildEA() {
        if(be == null)
            throw new NullParameterException("BaseEntity in EntityAttributeBuilder");
        return buildEA(be);
    }

    public EntityAttribute buildEA(BaseEntity be) {
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

    public BaseEntityDecorator build() {
        if(beBuilder == null) {
            log.error("Not inside a BaseEntityBuilder. call EntityAttributeBuilder.buildEA() here instead");
            throw new NullParameterException("BaseEntityBuilder");
        }

        if(beBuilder.getBaseEntity() == null)
            throw new NullParameterException("BaseEntity in BaseEntityBuilder");
        if(attribute == null)
            throw new NullParameterException("Attribute in EntityAttributeBuilder");
        if(value == null)
            throw new NullParameterException("value assigned to EntityAttribute in EntityAttributeBuilder");

        buildEA(beBuilder.getBaseEntity());
        return beBuilder;
    }
}
