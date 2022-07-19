package life.genny.qwandaq.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class QMessage implements Serializable {

	private static final long serialVersionUID = 1L;

	private String msgType;
    private String token;

	private String sourceCode;
	private String targetCode;
	private String attributeCode;
	private String questionCode;

    private String parentCode;
	private String rootCode;
	private String processId;

    private List<String> recipientCodeArray = new ArrayList<>();
	private String tag;
	private String destination;

	public QMessage() { }

	public QMessage(String msgType) {
		this.msgType = msgType;
	}
	public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getTargetCode() {
        return targetCode;
    }

    public void setTargetCode(String targetCode) {
        this.targetCode = targetCode;
    }

    public String getAttributeCode() {
        return attributeCode;
    }

    public void setAttributeCode(String attributeCode) {
        this.attributeCode = attributeCode;
    }

    public String getQuestionCode() {
        return questionCode;
    }

    public void setQuestionCode(String questionCode) {
        this.questionCode = questionCode;
    }

	public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    public String getRootCode() {
        return rootCode;
    }

    public void setRootCode(String rootCode) {
        this.rootCode = rootCode;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public List<String> getRecipientCodeArray() {
        return recipientCodeArray;
    }

    public void setRecipientCodeArray(List<String> recipientCodeArray) {
        this.recipientCodeArray = recipientCodeArray;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

}
