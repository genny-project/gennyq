package life.genny.kogito.common.models;

import java.io.Serializable;

public class TimerData implements Serializable {

    static final Long DEFAULT_TIMER_INTERVAL_MIN = 1L;

    private Long intervalMin = DEFAULT_TIMER_INTERVAL_MIN; //
    private String delayStr = "R/PT" + DEFAULT_TIMER_INTERVAL_MIN + "M";
    private Long elapsedMin = 0L;
    private Long expiryMin = 3L;// 7L * 24L * 60L; // 7 days

    // kcontext.setVariable("timerMinutes",0);
    // kcontext.setVariable("delayMinutes",2);
    // kcontext.setVariable("delayMinutesStr","R/PT"+delayMinutes+"M");
    // /* Calculate first next minutes from timer data */
    // kcontext.setVariable("nextMinutes",4);
    // kcontext.setVariable("nextMilestoneCode",1);
    // kcontext.setVariable("nextMinutesMessageCode",questionCode+"-"+nextMilestoneCode);
    // kcontext.setVariable("timerParms","0M:TEST_0M:GREEN,5M:TEST_5M:ORANGE,7M:TEST_7M:RED");

    public TimerData() {
        // TODO document why this constructor is empty
    }

    public TimerData(final Long intervalMin) {
        this.intervalMin = intervalMin;
        this.delayStr = "R/PT" + intervalMin + "M";
    }

    public Long updateElapsed() {
        return this.elapsedMin += this.intervalMin;
    }

    public Boolean hasExpired() {
        return this.elapsedMin >= this.expiryMin;
    }

    public Boolean isMilestone() {
        return false;
    }

    public Long getIntervalMin() {
        return intervalMin;
    }

    public void setIntervalMin(Long intervalMin) {
        this.intervalMin = intervalMin;
    }

    public String getDelayStr() {
        return delayStr;
    }

    public void setDelayStr(String delayStr) {
        this.delayStr = delayStr;
    }

    public Long getElapsedMin() {
        return elapsedMin;
    }

    public void setElapsedMin(Long elapsedMin) {
        this.elapsedMin = elapsedMin;
    }

    public Long getExpiryMin() {
        return expiryMin;
    }

    public void setExpiryMin(Long expiryMin) {
        this.expiryMin = expiryMin;
    }

    @Override
    public String toString() {
        return "TimerData [delayStr=" + delayStr + ", elapsedMin=" + elapsedMin + ", expiryMin=" + expiryMin
                + ", intervalMin=" + intervalMin + "]";
    }

}
