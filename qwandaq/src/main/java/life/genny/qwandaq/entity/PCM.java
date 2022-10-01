package life.genny.qwandaq.entity;

import life.genny.qwandaq.attribute.Attribute;

/**
 * PCM
 */
public class PCM extends BaseEntity {

	public static final String PCM_VERT = "PCM_VERT";
	public static final String PCM_HORI = "PCM_HORI";

	public static final String PCM_ROOT = "PCM_ROOT";
	public static final String PCM_CONTENT = "PCM_CONTENT";
	public static final String PCM_HEADER = "PCM_HEADER";
	public static final String PCM_SIDEBAR = "PCM_SIDEBAR";
	public static final String PCM_TABLE = "PCM_TABLE";
	public static final String PCM_DETAIL_VIEW = "PCM_DETAIL_VIEW";

	public PCM(String code, String name) {
		super(code, name);
	}

	public void setLocation(Integer index, String value) {
		setValue("PRI_LOC"+index, value);
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
