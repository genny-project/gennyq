package life.genny.qwandaq.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import javax.persistence.Embedded;
import javax.validation.Valid;

import life.genny.qwandaq.ContextList;

public class QCmdMessage extends QMessage {

	private static final String MESSAGE_TYPE = "CMD_MSG";
	private String cmd_type;
	private String code;
	
	private Boolean exec;
	
	private Boolean send;
	
	private String cache;

	@Embedded
	@Valid
	@JsonInclude(Include.NON_NULL)
	private ContextList contextList;

	public QCmdMessage(String cmd_type, String code) {

		super(MESSAGE_TYPE);
		this.code = code;
		this.cmd_type = cmd_type;
		this.send = true;
		this.exec=true;
		this.cache = null;
	}
	
	/** 
	 * @return String
	 */
	public String getCmd_type() {
		return cmd_type;
	}

	
	/** 
	 * @param cmd_type the cmd type to set
	 */
	public void setCmd_type(String cmd_type) {
		this.cmd_type = cmd_type;
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
	 * @return Boolean
	 */
	public Boolean getExec() {
		return exec;
	}

	/**
	 * @param exec the exec to set
	 */
	public void setExec(Boolean exec) {
		this.exec = exec;
	}

	/**
	 * @return Boolean
	 */
	public Boolean getSend() {
		return send;
	}

	/**
	 * @param send the send to set
	 */
	public void setSend(Boolean send) {
		this.send = send;
	}

	/**
	 * @return String
	 */
	public String getCache() {
		return cache;
	}

	/**
	 * @param cache the cache to set
	 */
	public void setCache(String cache) {
		this.cache = cache;
	}

	/**
	 * @return ContextList
	 */
	public ContextList getContextList() {
		return contextList;
	}

	/**
	 * @param contextList the contextList to set
	 */
	public void setContextList(final ContextList contextList) {
		this.contextList = contextList;
	}

}
