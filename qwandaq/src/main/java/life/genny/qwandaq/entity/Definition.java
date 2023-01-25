package life.genny.qwandaq.entity;

import java.lang.invoke.MethodHandles;
import java.util.List;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.annotation.JsonbTransient;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.Prefix;

/**
 * Definition
 */
@RegisterForReflection
public class Definition extends BaseEntity {

	static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());
	static Jsonb jsonb = JsonbBuilder.create();

	// core
	public static final String DEF_COMMON = "DEF_COMMON";
	public static final String DEF_PROJECT = "DEF_PROJECT";
	public static final String DEF_PERSON = "DEF_PERSON";
	public static final String DEF_COMPANY = "DEF_COMPANY";
	public static final String DEF_USER = "DEF_USER";
	public static final String DEF_MESSAGE = "DEF_MESSAGE";

	public Definition(String code, String name) {
		super(code, name);
	}

	public static Definition from(BaseEntity entity) {

		Definition definition = new Definition(entity.getCode(), entity.getName());
		entity.decorate(definition);
		return definition;
	}

	public void setAllowedAttribute(String attributeCode, Boolean mandatory) {
		setValue(Prefix.ATT_.concat(attributeCode), mandatory);
	}

	@JsonbTransient
	public List<EntityAttribute> getAllowedAttributes() {
		return findPrefixEntityAttributes(Prefix.ATT_);
	}

	public void setDefaultValue(String attributeCode, Object value) {
		setValue(Prefix.DFT_.concat(attributeCode), value);
	}

	@JsonbTransient
	public List<EntityAttribute> getDefaultValues() {
		return findPrefixEntityAttributes(Prefix.DFT_);
	}

}
