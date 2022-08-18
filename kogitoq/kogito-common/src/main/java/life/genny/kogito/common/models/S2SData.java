package life.genny.kogito.common.models;

import java.io.Serializable;

public class S2SData implements Serializable {

    private String questionCode;
    private String targetCode;
    private String sourceCode;
    private String pcmCode;
    private String events;
    private String token;
    private Boolean cancel = false;

    public S2SData() {
    }

    public String getQuestionCode() {
        return questionCode;
    }

    public void setQuestionCode(String questionCode) {
        this.questionCode = questionCode;
    }

    public String getTargetCode() {
        return targetCode;
    }

    public void setTargetCode(String targetCode) {
        this.targetCode = targetCode;
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

    public String getEvents() {
        return events;
    }

    public void setEvents(String events) {
        this.events = events;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    /**
     * @return String
     */
    @Override
    public String toString() {
        return "S2SData [questionCode=" + questionCode + "]";
    }

    public Boolean getCancel() {
        return cancel;
    }

    public void setCancel(Boolean cancel) {
        this.cancel = cancel;
    }

    public Boolean isCancel() {
        return cancel;
    }
}
