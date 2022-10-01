package life.genny.qwandaq.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public abstract class QMessage implements Serializable {

	private static final long serialVersionUID = 1L;

	private String type;
	private String token;

	private String sourceCode;
	private String targetCode;

	private String code;
	private String attributeCode;
	private String parentCode;
	private String processId;

	private String value;

	private List<String> recipientCodeArray = new ArrayList<>();
	private String tag; // Used for debugging and development testing.

	public QMessage() {
	}

	public QMessage(String type) {
		this.type = type;
	}

	public QMessage(String type, String code) {
		this.type = type;
		this.code = code;
	}

	/**
	 * @return String
	 */
	@Override
	public String toString() {
		return "QMessage[type=" + type + "]";
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
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

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getAttributeCode() {
		return attributeCode;
	}

	public void setAttributeCode(String attributeCode) {
		this.attributeCode = attributeCode;
	}

	public String getParentCode() {
		return parentCode;
	}

	public void setParentCode(String parentCode) {
		this.parentCode = parentCode;
	}

	public String getProcessId() {
		return processId;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
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
}
