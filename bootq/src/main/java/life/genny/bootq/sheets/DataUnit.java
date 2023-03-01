package life.genny.bootq.sheets;

import java.util.HashMap;
import java.util.Map;

/**
 * A POJO representation of a standard "Data Sheet" containing all the raw data needed to 
 * bootstrap into a project
 */
public abstract class DataUnit {

    public DataUnit() {
    }

    protected String name;

    // TODO: Reduce the complexity of these Maps by making better use of POJOs
    protected Map<String, Map<String, String>> attributes = new HashMap<>();
    protected Map<String, Map<String, String>> questionQuestions = new HashMap<>();
    protected Map<String, Map<String, String>> validations = new HashMap<>();
    protected Map<String, Map<String, String>> dataTypes = new HashMap<>();
    protected Map<String, Map<String, String>> questions = new HashMap<>();
    protected Map<String, Map<String, String>> entityAttributes = new HashMap<>();
    protected Map<String, Map<String, String>> baseEntitys = new HashMap<>();
    protected Map<String, Map<String, String>> messages = new HashMap<>();
    protected Map<String, Map<String, String>> def_baseEntitys = new HashMap<>();
    protected Map<String, Map<String, String>> def_entityAttributes = new HashMap<>();

    public void setAttributes(Map<String, Map<String, String>> attributes) {
        this.attributes = attributes;
    }
    
    public void setQuestionQuestions(Map<String, Map<String, String>> questionQuestions) {
        this.questionQuestions = questionQuestions;
    }

    public void setValidations(Map<String, Map<String, String>> validations) {
        this.validations = validations;
    }

    public void setDataTypes(Map<String, Map<String, String>> dataTypes) {
        this.dataTypes = dataTypes;
    }

    public void setQuestions(Map<String, Map<String, String>> questions) {
        this.questions = questions;
    }

    public void setEntityAttributes(Map<String, Map<String, String>> entityAttributes) {
        this.entityAttributes = entityAttributes;
    }

    public void setBaseEntitys(Map<String, Map<String, String>> baseEntitys) {
        this.baseEntitys = baseEntitys;
    }

    public void setMessages(Map<String, Map<String, String>> messages) {
        this.messages = messages;
    }

    public Map<String, Map<String, String>> getAttributes() {
        return attributes;
    }

    public Map<String, Map<String, String>> getQuestionQuestions() {
        return questionQuestions;
    }

    public Map<String, Map<String, String>> getValidations() {
        return validations;
    }

    public Map<String, Map<String, String>> getDataTypes() {
        return dataTypes;
    }

    public Map<String, Map<String, String>> getQuestions() {
        return questions;
    }

    public Map<String, Map<String, String>> getEntityAttributes() {
        return entityAttributes;
    }

    public Map<String, Map<String, String>> getBaseEntitys() {
        return baseEntitys;
    }

    public Map<String, Map<String, String>> getDef_baseEntitys() {
        return def_baseEntitys;
    }

    public Map<String, Map<String, String>> getDef_entityAttributes() {
        return def_entityAttributes;
    }

    public Map<String, Map<String, String>> getMessages() {
        return messages;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}
