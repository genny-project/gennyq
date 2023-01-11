package life.genny.bootq.models;

import java.util.HashMap;
import java.util.Map;

public class ModuleUnit extends GennySheet {

    protected Map<String, Map<String, String>> attributes = new HashMap<>();
    protected Map<String, Map<String, String>> questionQuestions = new HashMap<>();
    protected Map<String, Map<String, String>> validations = new HashMap<>();
    protected Map<String, Map<String, String>> dataTypes = new HashMap<>();
    protected Map<String, Map<String, String>> questions = new HashMap<>();
    protected Map<String, Map<String, String>> entityAttributes = new HashMap<>();
    protected Map<String, Map<String, String>> entityEntitys = new HashMap<>();
    protected Map<String, Map<String, String>> baseEntitys = new HashMap<>();
    protected Map<String, Map<String, String>> def_baseEntitys = new HashMap<>();
    protected Map<String, Map<String, String>> def_entityAttributes = new HashMap<>();

    public ModuleUnit(Map<String, String> map) {
		super(map);
	}

	public Map<String, Map<String, String>> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Map<String, String>> attributes) {
		this.attributes = attributes;
	}

	public Map<String, Map<String, String>> getQuestionQuestions() {
		return questionQuestions;
	}

	public void setQuestionQuestions(Map<String, Map<String, String>> questionQuestions) {
		this.questionQuestions = questionQuestions;
	}

	public Map<String, Map<String, String>> getValidations() {
		return validations;
	}

	public void setValidations(Map<String, Map<String, String>> validations) {
		this.validations = validations;
	}

	public Map<String, Map<String, String>> getDataTypes() {
		return dataTypes;
	}

	public void setDataTypes(Map<String, Map<String, String>> dataTypes) {
		this.dataTypes = dataTypes;
	}

	public Map<String, Map<String, String>> getQuestions() {
		return questions;
	}

	public void setQuestions(Map<String, Map<String, String>> questions) {
		this.questions = questions;
	}

	public Map<String, Map<String, String>> getEntityAttributes() {
		return entityAttributes;
	}

	public void setEntityAttributes(Map<String, Map<String, String>> entityAttributes) {
		this.entityAttributes = entityAttributes;
	}

	public Map<String, Map<String, String>> getEntityEntitys() {
		return entityEntitys;
	}

	public void setEntityEntitys(Map<String, Map<String, String>> entityEntitys) {
		this.entityEntitys = entityEntitys;
	}

	public Map<String, Map<String, String>> getBaseEntitys() {
		return baseEntitys;
	}

	public void setBaseEntitys(Map<String, Map<String, String>> baseEntitys) {
		this.baseEntitys = baseEntitys;
	}

	public Map<String, Map<String, String>> getDef_baseEntitys() {
		return def_baseEntitys;
	}

	public void setDef_baseEntitys(Map<String, Map<String, String>> def_baseEntitys) {
		this.def_baseEntitys = def_baseEntitys;
	}

	public Map<String, Map<String, String>> getDef_entityAttributes() {
		return def_entityAttributes;
	}

	public void setDef_entityAttributes(Map<String, Map<String, String>> def_entityAttributes) {
		this.def_entityAttributes = def_entityAttributes;
	}

}
