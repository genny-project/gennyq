package life.genny.bootq.models;

import java.util.HashMap;
import java.util.Map;

public abstract class DataUnit {

    public DataUnit() {
    }

    protected Map<String, Map<String, String>> attributes = new HashMap<>();
    protected Map<String, Map<String, String>> attributeLinks = new HashMap<>();
    protected Map<String, Map<String, String>> questionQuestions = new HashMap<>();
    protected Map<String, Map<String, String>> validations = new HashMap<>();
    protected Map<String, Map<String, String>> dataTypes = new HashMap<>();
    protected Map<String, Map<String, String>> questions = new HashMap<>();
    protected Map<String, Map<String, String>> asks = new HashMap<>();
    protected Map<String, Map<String, String>> notifications = new HashMap<>();
    protected Map<String, Map<String, String>> entityAttributes = new HashMap<>();
    protected Map<String, Map<String, String>> entityEntitys = new HashMap<>();
    protected Map<String, Map<String, String>> baseEntitys = new HashMap<>();
    protected Map<String, Map<String, String>> messages = new HashMap<>();
    protected Map<String, Map<String, String>> def_baseEntitys = new HashMap<>();
    protected Map<String, Map<String, String>> def_entityAttributes = new HashMap<>();

    public void setAttributes(Map<String, Map<String, String>> attributes) {
        this.attributes = attributes;
    }

    public void setAttributeLinks(Map<String, Map<String, String>> attributeLinks) {
        this.attributeLinks = attributeLinks;
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

    public void setAsks(Map<String, Map<String, String>> asks) {
        this.asks = asks;
    }

    public void setNotifications(Map<String, Map<String, String>> notifications) {
        this.notifications = notifications;
    }

    public void setEntityAttributes(Map<String, Map<String, String>> entityAttributes) {
        this.entityAttributes = entityAttributes;
    }

    public void setEntityEntitys(Map<String, Map<String, String>> entityEntitys) {
        this.entityEntitys = entityEntitys;
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

    public Map<String, Map<String, String>> getAttributeLinks() {
        return attributeLinks;
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

    public Map<String, Map<String, String>> getAsks() {
        return asks;
    }

    public Map<String, Map<String, String>> getNotifications() {
        return notifications;
    }

    public Map<String, Map<String, String>> getEntityAttributes() {
        return entityAttributes;
    }

    public Map<String, Map<String, String>> getEntityEntitys() {
        return entityEntitys;
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

}
