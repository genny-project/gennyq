package life.genny.qwandaq.entity;

import java.lang.invoke.MethodHandles;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Definition
 */
@RegisterForReflection
public class Definition extends BaseEntity {

	static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());
	static Jsonb jsonb = JsonbBuilder.create();

	public Definition(String code, String name) {
		super(code, name);
	}

	public static Definition from(BaseEntity entity) {

		Definition definition = new Definition(entity.getCode(), entity.getName());
		definition.setRealm(entity.getRealm());
		definition.setRealm(entity.getRealm());
		definition.setBaseEntityAttributes(entity.getBaseEntityAttributes());

		return definition;
	}

}
