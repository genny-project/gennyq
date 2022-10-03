package life.genny.qwandaq.entity;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.DataType;

/**
 * PCM
 */
@RegisterForReflection
public class PCM extends BaseEntity {

	static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());
	static Jsonb jsonb = JsonbBuilder.create();

	public static final String PCM_VERT = "PCM_VERT";
	public static final String PCM_HORI = "PCM_HORI";

	public static final String PCM_TREE = "PCM_TREE";
	public static final String PCM_ROOT = "PCM_ROOT";
	public static final String PCM_CONTENT = "PCM_CONTENT";
	public static final String PCM_HEADER = "PCM_HEADER";
	public static final String PCM_SIDEBAR = "PCM_SIDEBAR";
	public static final String PCM_TABLE = "PCM_TABLE";
	public static final String PCM_DETAIL_VIEW = "PCM_DETAIL_VIEW";

	public PCM(String code, String name) {
		super(code, name);
	}

	public static PCM from(BaseEntity entity) {

		PCM pcm = new PCM(entity.getCode(), entity.getName());
		pcm.setRealm(entity.getRealm());
		pcm.setRealm(entity.getRealm());
		pcm.setBaseEntityAttributes(entity.getBaseEntityAttributes());

		return pcm;
	}

	public void setLocation(Integer index, String value) {

		String code = Attribute.PRI_LOC + index;
		Optional<EntityAttribute> location = getValue(code);

		if (location.isEmpty()) {
			Attribute attribute = new Attribute(code, "Location " + index, new DataType(String.class));
			EntityAttribute ea = new EntityAttribute();
			ea.setAttribute(attribute);
			ea.setWeight(Double.valueOf(index));
			ea.setValue(value);
			addAttribute(ea);
		} else
			setValue(code, value);
	}

	public String getLocation(Integer index) {
		return getValueAsString("PRI_LOC"+index);
	}

	public void setTemplateCode(String code) {
		setValue(Attribute.PRI_TEMPLATE_CODE, code);
	}

	public String getTemplateCode() {
		return getValueAsString(Attribute.PRI_TEMPLATE_CODE);
	}

	public void setQuestionCode(String code) {
		setValue(Attribute.PRI_QUESTION_CODE, code);
	}

	public String getQuestionCode() {
		return getValueAsString(Attribute.PRI_QUESTION_CODE);
	}

	public void setTargetCode(String code) {
		setValue(Attribute.PRI_TARGET_CODE, code);
	}

	public String getTargetCode() {
		return getValueAsString(Attribute.PRI_TARGET_CODE);
	}

}
