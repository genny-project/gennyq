package life.genny.qwandaq.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public abstract class QMessage implements Serializable {

	private static final long serialVersionUID = 1L;

	private String msgType;
	private String token;

	private List<String> recipientCodeArray = new ArrayList<>();
	private String tag;

	private String sourceCode;
	private String targetCode;
	private String code;
	private String processId;

	public QMessage() { }

	public QMessage(String msgType) {
		this.msgType = msgType;
	}

	/**
	 * @return String
	 */
	@Override
	public String toString() {
		return "QMessage [msgType=" + msgType + "]";
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

	public String getProcessId() {
		return processId;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
	}

}
