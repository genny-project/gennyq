package life.genny.qwandaq.message;

import java.io.Serializable;

public class MessageData implements Serializable {

	private static final long serialVersionUID = 1L;

	private String code;
	private String parentCode;
	private String rootCode;
	private String targetCode;
	private String sourceCode;
	private Long id;
	private String value;
	private String processId;
	private String content;

	public MessageData() { }

	public MessageData(String code) {
		this.code = code;
	}
	
	/** 
	 * @return String
	 */
	public String getCode() {
		return code;
	}
	
	/** 
	 * @param code the code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}
	
	/** 
	 * @return Long
	 */
	public Long getId() {
		return id;
	}
	
	/** 
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/** 
	 * @return String
	 */
	public String getValue() {
		return value;
	}

	/** 
	 * @param value the valu to set
	 */
	public void setValue(String value) {
		this.value = value;
	}
	
	/** 
	 * @return String
	 */
	public String getContent() {
		return content;
	}
	
	/** 
	 * @param content the content to set
	 */
	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * @return the parentCode
	 */
	public String getParentCode() {
		return parentCode;
	}

	/**
	 * @param parentCode the parentCode to set
	 */
	public void setParentCode(String parentCode) {
		this.parentCode = parentCode;
	}

	/**
	 * @return the rootCode
	 */
	public String getRootCode() {
		return rootCode;
	}

	/**
	 * @param rootCode the rootCode to set
	 */
	public void setRootCode(String rootCode) {
		this.rootCode = rootCode;
	}

	/**
	 * @return the targetCode
	 */
	public String getTargetCode() {
		return targetCode;
	}

	/**
	 * @param targetCode the targetCode to set
	 */
	public void setTargetCode(String targetCode) {
		this.targetCode = targetCode;
	}
	
	/**
	 * @return the targetCode
	 */
	public String getSourceCode() {
		return sourceCode;
	}

	/**
	 * @param sourceCode the sourceCode to set
	 */
	public void setSourceCode(String sourceCode) {
		this.sourceCode = sourceCode;
	}

	public String getProcessId() {
		return processId;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
	}


	/** 
	 * @return String
	 */
	@Override
	public String toString() {
		return " MessageData [code=" + code + "   " + id + "]";
	}
}
