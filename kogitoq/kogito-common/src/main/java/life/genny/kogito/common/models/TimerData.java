package life.genny.kogito.common.models;

import java.io.Serializable;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TimerData implements Serializable {

    static final Long DEFAULT_TIMER_INTERVAL_MIN = 1L;

    static final Logger log = Logger.getLogger(TimerData.class);

    private Long intervalMin = DEFAULT_TIMER_INTERVAL_MIN; //
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
    }

    public Long updateElapsed() {
        this.elapsedMin = this.elapsedMin + this.intervalMin;
        return this.elapsedMin;
    }

    public Boolean hasExpired() {
        // log.info("hasExpired: " + this.elapsedMin + " >= " + this.expiryMin);
        return this.elapsedMin >= this.expiryMin;
    }

	@JsonIgnore
    public Boolean isMilestone() {
        return false;
    }

    public Long getIntervalMin() {
        return intervalMin;
    }

    public void setIntervalMin(Long intervalMin) {
        this.intervalMin = intervalMin;
    }

	@JsonIgnore
    public String getIntervalStr() {
        return "R/PT" + getIntervalMin() + "M";
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
        return "TimerData [intervalStr=" + getIntervalStr() + ", elapsedMin=" + elapsedMin + ", expiryMin=" + expiryMin
                + ", intervalMin=" + intervalMin + ", hasExpired=" + this.hasExpired() + "]";
    }

}
