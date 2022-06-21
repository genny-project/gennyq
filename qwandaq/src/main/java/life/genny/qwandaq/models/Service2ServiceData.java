package life.genny.qwandaq.models;

import javax.json.JsonObject;

public class Service2ServiceData {

	private String processId;
	private String questionCode;
	private String targetCode;
	private String sourceCode;
	private String pcmCode;

	public Service2ServiceData() { }

	public Service2ServiceData(String processId) {
		this.processId = processId;
	}

	public String getProcessId() {
		return this.processId;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
	}

	public String getQuestionCode() {
		return this.questionCode;
	}

	public String getSourceCode() {
		return this.sourceCode;
	}

	public String getTargetCode() {
		return this.targetCode;
	}

	public String getPcmCode() {
		return this.pcmCode;
	}

	public void setQuestionCode(String questionCode) {
		this.questionCode = questionCode;
	}

	public void setSourceCode(String sourceCode) {
		this.sourceCode = sourceCode;
	}

	public void setTargetCode(String targetCode) {
		this.targetCode = targetCode;
	}

	public void setPcmCode(String pcmCode) {
		this.pcmCode = pcmCode;
	}
}
