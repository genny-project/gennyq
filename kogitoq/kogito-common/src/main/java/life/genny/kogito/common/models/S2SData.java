package life.genny.kogito.common.models;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class S2SData implements Serializable {

    public enum EAbortReason {
        NONE,
        CANCEL,
        TIMEOUT,
    }

    private String productCode;
    private String questionCode;
    private String targetCode;
    private String sourceCode;
    private String pcmCode;
    private String events;
    private String token;
    private TimerData timerData;
    private EAbortReason abortReason = EAbortReason.NONE;

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

    public TimerData getTimerData() {
        return timerData;
    }

    public void setTimerData(TimerData timerData) {
        this.timerData = timerData;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    @Override
    public String toString() {
        return "S2SData (" + getProductCode() + ") [abortReason=" + abortReason + ", events=" + events + ", pcmCode="
                + pcmCode + ", questionCode="
                + questionCode + ", sourceCode=" + sourceCode + ", targetCode=" + targetCode + ", timerData="
                + timerData + "]";
    }

    public EAbortReason getAbortReason() {
        return abortReason;
    }

    public void setAbortReason(EAbortReason abortReason) {
        this.abortReason = abortReason;
    }

    @JsonIgnore
    public Boolean isAborted() {
        return !abortReason.equals(EAbortReason.NONE);
    }

    @JsonIgnore
    public Boolean isCanceled() {
        return abortReason.equals(EAbortReason.CANCEL);
    }

    @JsonIgnore
    public Boolean isExpired() {
        return abortReason.equals(EAbortReason.TIMEOUT);
    }

    public Boolean setCancel() {
        // This method makes it easier within kogito to set a state to avoid enum
        Boolean oldState = getAbortReason().equals(EAbortReason.CANCEL);
        setAbortReason(EAbortReason.CANCEL);
        return oldState;
    }

    public Boolean setExpired() {
        // This method makes it easier within kogito to set a state to avoid enum
        Boolean oldState = getAbortReason().equals(EAbortReason.TIMEOUT);
        setAbortReason(EAbortReason.TIMEOUT);
        return oldState;
    }

    public Boolean setNone() {
        // This method makes it easier within kogito to set a state to avoid enum
        Boolean oldState = getAbortReason().equals(EAbortReason.NONE);
        setAbortReason(EAbortReason.NONE);
        return oldState;
    }

}
