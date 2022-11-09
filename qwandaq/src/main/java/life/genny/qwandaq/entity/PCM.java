package life.genny.qwandaq.entity;

import java.lang.invoke.MethodHandles;
import java.util.List;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.datatype.DataType;

/**
 * PCM
 */
@RegisterForReflection
public class PCM extends BaseEntity {

	static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());
	static Jsonb jsonb = JsonbBuilder.create();

	public static final String PREFIX = Prefix.PCM;

	public static final String TPL_VERT = "TPL_VERT";
	public static final String TPL_HORI = "TPL_HORI";

	public static final String PCM_TREE = "PCM_TREE";
	public static final String PCM_ROOT = "PCM_ROOT";
	public static final String PCM_CONTENT = "PCM_CONTENT";
	public static final String PCM_HEADER = "PCM_HEADER";
	public static final String PCM_SIDEBAR = "PCM_SIDEBAR";
	public static final String PCM_TABLE = "PCM_TABLE";
	public static final String PCM_DETAIL_VIEW = "PCM_DETAIL_VIEW";
	public static final String PCM_PROCESS = "PCM_PROCESS";

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

	public static Integer findLocation(String location) {
		return Integer.parseInt(StringUtils.removeStart(location, Attribute.PRI_LOC));
	}

	public static String location(int index) {
		return Attribute.PRI_LOC.concat(Integer.toString(index));
	}

	public void addStringAttribute(String code, String name, String value) {
		addStringAttribute(code, name, 1.0, value);
	}

	public void addStringAttribute(String code, String name, Double weight, String value) {

		if (findEntityAttribute(code).isPresent()) {
			log.info("[/] Attribute Present for " + code);
			setValue(code, value);
			return;
		}

		log.info("[X] Attribute NOT Present for " + code);
		Attribute attribute = new Attribute(code, name, new DataType(String.class));
		EntityAttribute ea = new EntityAttribute();
		ea.setAttribute(attribute);
		ea.setWeight(weight);
		ea.setValue(value);
		addAttribute(ea);
	}

	public void setLocation(Integer index, String value) {
		String code = location(index);
		addStringAttribute(code, "Location " + index, Double.valueOf(index), value);
	}

	public String getLocation(Integer index) {
		return getValueAsString(Attribute.PRI_LOC+index);
	}

	public List<EntityAttribute> getLocations() {
		return findPrefixEntityAttributes(Attribute.PRI_LOC);
	}

	public void setTemplateCode(String code) {
		addStringAttribute(Attribute.PRI_TEMPLATE_CODE, "Template Code", code);
	}

	public String getTemplateCode() {
		return getValueAsString(Attribute.PRI_TEMPLATE_CODE);
	}

	public void setQuestionCode(String code) {
		addStringAttribute(Attribute.PRI_QUESTION_CODE, "Question Code", code);
	}

	public String getQuestionCode() {
		return getValueAsString(Attribute.PRI_QUESTION_CODE);
	}

	public void setTargetCode(String code) {
		addStringAttribute(Attribute.PRI_TARGET_CODE, "Target Code", code);
	}

	public String getTargetCode() {
		return getValueAsString(Attribute.PRI_TARGET_CODE);
	}

}
