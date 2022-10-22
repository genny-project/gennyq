package life.genny.qwandaq.graphql;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.Answer;

/**
 * TODO: We need to document this
 * 
 * @author Jasper Robison
 */
@RegisterForReflection
public class ProcessData extends ProcessInstanceVariables {

	private String questionCode;
	private String sourceCode;
	private String pcmCode;
	private String definitionCode;
	private String buttonEvents;
	private String processEntityCode;
	private List<String> attributeCodes;
	private List<Answer> answers;

	public ProcessData() {
		super();
	}

	public String getQuestionCode() {
		return questionCode;
	}

	public void setQuestionCode(String questionCode) {
		this.questionCode = questionCode;
	}

	public String getSourceCode() {
		return sourceCode;
	}

	public void setSourceCode(String sourceCode) {
		this.sourceCode = sourceCode;
	}

	public String getPcmCode() {
		return pcmCode;
	}

	public void setPcmCode(String pcmCode) {
		this.pcmCode = pcmCode;
	}

	public String getDefinitionCode() {
		return definitionCode;
	}

	public void setDefinitionCode(String definitionCode) {
		this.definitionCode = definitionCode;
	}

	public String getButtonEvents() {
		return buttonEvents;
	}

	public void setButtonEvents(String buttonEvents) {
		this.buttonEvents = buttonEvents;
	}

	public String getProcessEntityCode() {
		return processEntityCode;
	}

	public void setProcessEntityCode(String processEntityCode) {
		this.processEntityCode = processEntityCode;
	}

	public List<String> getAttributeCodes() {
		return attributeCodes;
	}

	public void setAttributeCodes(List<String> attributeCodes) {
		this.attributeCodes = attributeCodes;
	}

	public List<Answer> getAnswers() {
		return answers;
	}

	public void setAnswers(List<Answer> answers) {
		this.answers = answers;
	}

}
