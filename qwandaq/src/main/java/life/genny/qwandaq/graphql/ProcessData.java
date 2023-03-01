package life.genny.qwandaq.graphql;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;
import javax.json.bind.annotation.JsonbTransient;
import java.util.Arrays;
import life.genny.qwandaq.Answer;

/**
 * TODO: We need to document this
 * 
 * @author Jasper Robison
 */
@RegisterForReflection
public class ProcessData extends ProcessInstanceVariables {

	private String sourceCode;
	private String targetCode;
	private String questionCode;

	private String pcmCode;
	private String parent;
	private String location;

	private String buttonEvents;

	private String definitionCode;
	private String processEntityCode;
	private List<String> attributeCodes;
	private List<String> searches;
	private List<Answer> answers;

	private boolean readonly;

	public ProcessData() {
		super();
	}

	// TODO: There is a danger here that the initial defCodes set in this object may
	// not reflect the current defCodes state.
	@JsonbTransient
	public List<String> getDefCodes() {
		// split the definition code into all the defCodes for this baseEntity
		String defCodesStr = definitionCode;
		if (defCodesStr.startsWith("DEF_DEF_")) { // TODO: Hack to fix prefix bug
			defCodesStr = defCodesStr.substring("DEF_".length());
		}
		defCodesStr = defCodesStr.replace("[", "");
		defCodesStr = defCodesStr.replace("]", "");
		defCodesStr = defCodesStr.replace("\"", "");
		defCodesStr = defCodesStr.replace("_DEF_", ",DEF_");
		return Arrays.asList(defCodesStr.split(","));
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

	public String getDefinitionCode() {
		return definitionCode;
	}

	public void setDefinitionCode(String definitionCode) {
		this.definitionCode = definitionCode;
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

	public List<String> getSearches() {
		return searches;
	}

	public void setSearches(List<String> searches) {
		this.searches = searches;
	}

	public List<Answer> getAnswers() {
		return answers;
	}

	public void setAnswers(List<Answer> answers) {
		this.answers = answers;
	}

	public boolean isReadonly() {
		return readonly;
	}

	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}

}
