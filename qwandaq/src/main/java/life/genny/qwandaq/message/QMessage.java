package life.genny.qwandaq.message;

import java.io.Serializable;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import javax.json.bind.annotation.JsonbTransient;
import javax.xml.bind.annotation.XmlTransient;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.models.GennyToken;

@RegisterForReflection
public abstract class QMessage implements Serializable, QMessageIntf {

	private static final long serialVersionUID = 1L;

	private String msg_type;
	private String token;
	private List<String> targetCodes;
	private String sourceAddress;
	private String sourceCode;
	private String targetCode;
	private String attributeCode;
	private String questionCode;
	private String message;
	private List<String> recipientCodeArray = new ArrayList<>();

	public QMessage() { }

	public QMessage(String msg_type) {
		this.msg_type = msg_type;
	}

	/**
	 * @return String
	 */
	public String getMsg_type() {
		return msg_type;
	}

	/**
	 * @param msg_type the type of message to set
	 */
	public void setMsg_type(String msg_type) {
		this.msg_type = msg_type;
	}

	/**
	 * @return String
	 */
	public String getToken() {
		return token;
	}


	/**
	 * @param token the token to set
	 */
	public void setToken(GennyToken token) {
		this.token = token.getToken();
	}

	/**
	 * @param token the token to set
	 */
	@Deprecated
	public void setToken(String token) {
		this.token = token;
	}

	/**
	 * @return the targetCodes
	 */
	public List<String> getTargetCodes() {
		return targetCodes;
	}

	/**
	 * @param targetCodes the targetCodes to set
	 */
	public void setTargetCodes(List<String> targetCodes) {
		this.targetCodes = targetCodes;
	}

	/**
	 * @return String
	 */
	public String getSourceAddress() {
		return sourceAddress;
	}

	/**
	 * @param sourceAddress the source address to set
	 */
	public void setSourceAddress(String sourceAddress) {
		this.sourceAddress = sourceAddress;
	}

	/**
	 * @return String
	 */
	public String getSourceCode() {
		return sourceCode;
	}

	/**
	 * @param sourceCode the source code to set
	 */
	public void setSourceCode(String sourceCode) {
		this.sourceCode = sourceCode;
	}

	/**
	 * @return String
	 */
	public String getTargetCode() {
		return targetCode;
	}

	/**
	 * @param targetCode the target code to et
	 */
	public void setTargetCode(String targetCode) {
		this.targetCode = targetCode;
	}

	/**
	 * @return String
	 */
	public String getQuestionCode() {
		return questionCode;
	}

	/**
	 * @param questionCode the question code to set
	 */
	public void setQuestionCode(String questionCode) {
		this.questionCode = questionCode;
	}

	/**
	 * @return String
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * @return the recipientCodeArray
	 */
	public List<String> getRecipientCodeArray() {
		return recipientCodeArray;
	}

	/**
	 * @param recipientCodeArray the array of recipient codes to set
	 */
	@JsonbTransient
	@XmlTransient
	public void setRecipientCodeArray(String[] recipientCodeArray) {
		this.recipientCodeArray = Arrays.asList(recipientCodeArray);
	}

	/**
	 * @param recipientCodeArray the list of recipient codes to set
	 */
	public void setRecipientCodeArray(List<String> recipientCodeArray) {
		this.recipientCodeArray = recipientCodeArray;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	/**
	 * @return String
	 */
	public String getAttributeCode() {
		return attributeCode;
	}

	/**
	 * @param attributeCode the attribute code to set
	 */

	public void setAttributeCode(String attributeCode) {
		this.attributeCode = attributeCode;
	}

	/**
	 * @return String
	 */
	@Override
	public String toString() {
		return "QMessage [msg_type=" + msg_type + "],";
	}
}
