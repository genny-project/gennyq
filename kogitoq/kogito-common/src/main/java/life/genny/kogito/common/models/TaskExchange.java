package life.genny.kogito.common.models;

import java.io.Serializable;

public class TaskExchange implements Serializable {

	private String productCode;

	private String sourceCode;
	private String targetCode;
	private String questionCode;

	private String pcmCode;
	private String parent;
	private String location;

	private String buttonEvents;
	private String token;

	private TimerData timerData;

	// completion information
	private ECompletion completion;
	private String completionCode;

	public TaskExchange() {
	}

	public void submit() {
		setCompletion(ECompletion.SUBMIT);
	}

	public void cancel() {
		setCompletion(ECompletion.CANCEL);
	}

	public void previous() {
		setCompletion(ECompletion.PREVIOUS);
	}

	public void timeout() {
		setCompletion(ECompletion.TIMEOUT);
	}

	public void custom() {
		setCompletion(ECompletion.CUSTOM);
	}

	public Boolean isSubmit() {
		return this.completion == ECompletion.SUBMIT;
	}

	public Boolean isCancel() {
		return this.completion == ECompletion.CANCEL;
	}

	public Boolean isPrevious() {
		return this.completion == ECompletion.PREVIOUS;
	}

	public Boolean isTimeout() {
		return this.completion == ECompletion.TIMEOUT;
	}

	public Boolean isCustom() {
		return this.completion == ECompletion.CUSTOM;
	}

	@Override
	public String toString() {
		return "TaskExchange (" + productCode + ") "
			+ "[completion=" + completion 
			+ ", completionCode=" + completionCode
			+ ", buttonEvents=" + buttonEvents
			+ ", pcmCode=" + pcmCode 
			+ ", questionCode=" + questionCode 
			+ ", sourceCode=" + sourceCode 
			+ ", targetCode=" + targetCode 
			+ ", timerData=" + timerData + "]";
	}

	public String getProductCode() {
		return productCode;
	}

	public void setProductCode(String productCode) {
		this.productCode = productCode;
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

	public String getQuestionCode() {
		return questionCode;
	}

	public void setQuestionCode(String questionCode) {
		this.questionCode = questionCode;
	}

	public String getPcmCode() {
		return pcmCode;
	}

	public void setPcmCode(String pcmCode) {
		this.pcmCode = pcmCode;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getButtonEvents() {
		return buttonEvents;
	}

	public void setButtonEvents(String buttonEvents) {
		this.buttonEvents = buttonEvents;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public TimerData getTimerData() {
		return timerData;
	}

	public void setTimerData(TimerData timerData) {
		this.timerData = timerData;
	}

	public ECompletion getCompletion() {
		return completion;
	}

	public void setCompletion(ECompletion completion) {
		this.completion = completion;
	}

	public String getCompletionCode() {
		return completionCode;
	}

	public void setCompletionCode(String completionCode) {
		this.completionCode = completionCode;
	}

}
