package life.genny.qwandaq.graphql;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.message.QDataAskMessage;

@RegisterForReflection
public class ProcessQuestions extends ProcessInstanceVariables {

	private String questionCode;
	private String sourceCode;
	private String targetCode;
	private String pcmCode;
	private String definitionCode;
	private String events;
    private BaseEntity processEntity;
    private QDataAskMessage askMessage;

	public ProcessQuestions() {
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

    public String getTargetCode() {
        return targetCode;
    }

    public void setTargetCode(String targetCode) {
        this.targetCode = targetCode;
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

	public QDataAskMessage getAskMessage() {
        return askMessage;
    }

    public void setAskMessage(QDataAskMessage askMessage) {
        this.askMessage = askMessage;
    }

}
