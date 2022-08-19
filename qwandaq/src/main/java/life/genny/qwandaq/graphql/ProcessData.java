package life.genny.qwandaq.graphql;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.entity.BaseEntity;

@RegisterForReflection
public class ProcessData extends ProcessInstanceVariables {

	private String questionCode;
	private String sourceCode;
	private String pcmCode;
	private String definitionCode;
	private String events;
    private BaseEntity processEntity;
	private List<String> attributeCodes;

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

    public String getEvents() {
        return events;
    }

    public void setEvents(String events) {
        this.events = events;
    }

    public BaseEntity getProcessEntity() {
        return processEntity;
    }

    public void setProcessEntity(BaseEntity processEntity) {
        this.processEntity = processEntity;
    }

	public List<String> getAttributeCodes() {
		return attributeCodes;
	}

	public void setAttributeCodes(List<String> attributeCodes) {
		this.attributeCodes = attributeCodes;
	}

}
