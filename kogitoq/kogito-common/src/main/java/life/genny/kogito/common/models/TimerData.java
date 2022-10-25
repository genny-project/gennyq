package life.genny.kogito.common.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.time.ZoneId;
import java.util.PriorityQueue;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.annotation.JsonbTransient;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TimerData implements Serializable {

    private static final Logger log = Logger.getLogger(TimerData.class);

    static Jsonb jsonb = JsonbBuilder.create();

    static final Long DEFAULT_TIMER_INTERVAL_MIN = 1L;
    static final Long OFFSET_EXPIRY_SECONDS = 7L * 24L * 60L * 60L; // Add a week
    static final Integer PRIORITY_QUEUE_INITIAL_SIZE = 3;
    static final Long DEFAULT_TIMER_EXPIRY_SECONDS = 12448167224L; // This must be set during init, default to 20th June
                                                                   // 2364

    private Long intervalMin = DEFAULT_TIMER_INTERVAL_MIN; //
    private Long elapsedMin = 0L;
    private Long expiryMin = 3L;// 7L * 24L * 60L; // 7 days
    private Long expiryTimeStamp = DEFAULT_TIMER_EXPIRY_SECONDS; // This must be set during init, default to 20th June
                                                                 // 2364
    private Boolean expired = false;

    private TimerEvent currentMilestone = null;

    private TimerEvent[] timerEventsArray = new TimerEvent[0];

    // @JsonbTransient
    // @JsonIgnore
    // private List<TimerEvent> timerevents = new
    // ArrayList<>(PRIORITY_QUEUE_INITIAL_SIZE);

    public TimerData() {
        // TODO document why this constructor is empty
    }

    public TimerData(final Long intervalMin) {
        this.intervalMin = intervalMin;
    }

    public TimerData(final String timerDataString) {
        // Split up into TimerEvents
        String[] timerEventStrArray = timerDataString.split(",");
        for (String timerEventStr : timerEventStrArray) {
            add(new TimerEvent(timerEventStr));
        }
    }

    public Long updateElapsed() {
        this.elapsedMin = this.elapsedMin + this.intervalMin;
        return this.elapsedMin;
    }

    public Boolean hasExpired() {
        Long currentTimeStampUTC = getNow();
        expired = expired || (currentTimeStampUTC >= this.expiryTimeStamp);
        log.debug("hasExpired->" + expired + " ,currentTimeStampUTC=" + currentTimeStampUTC + " ,expiryTimeStamp="
                + this.expiryTimeStamp);

        return expired;
    }

    @JsonIgnore
    public Boolean isMilestone() {
        // Get current UTC date time
        Long currentTimeStampUTC = getNow();
        // Now grab the first TimerEvent in the queue
        if ((this.timerEventsArray != null) && (this.timerEventsArray.length > 0)) {
            TimerEvent firstEvent = this.timerEventsArray[0];
            if (firstEvent != null) {
                log.info("Current UTC is " + currentTimeStampUTC + ", First event is " +
                        firstEvent);
                return firstEvent.getTimeStamp() <= currentTimeStampUTC;
            }
        }
        return false;
    }

    public TimerEvent updateMilestone() {
        if ((this.timerEventsArray != null) && (this.timerEventsArray.length > 0)) {
            TimerEvent[] newTimerEventsArray = new TimerEvent[this.timerEventsArray.length - 1];
            this.currentMilestone = this.timerEventsArray[0];
            for (int j = 0; j < this.timerEventsArray.length - 1; j++) {
                // Shift element of array by one
                newTimerEventsArray[j] = this.timerEventsArray[j + 1];
            }
            this.timerEventsArray = newTimerEventsArray; // replace the array
        }
        if ((this.timerEventsArray != null) && (this.timerEventsArray.length > 0)) {
            return this.timerEventsArray[0];
        }
        return this.currentMilestone; // stay with this existing final milestone
    }

    @JsonIgnore
    public TimerEvent getCurrentMilestone() {
        return this.currentMilestone;
    }

    @JsonIgnore
    public void setCurrentMilestone(TimerEvent currentMilestone) {
        this.currentMilestone = currentMilestone;
    }

    @JsonIgnore
    public TimerEvent getNextMilestone() {
        if ((this.timerEventsArray != null) && (this.timerEventsArray.length > 0)) {
            return this.timerEventsArray[0];
        }
        return null;
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

    @JsonbTransient
    @JsonIgnore
    public void setExpiryMin(Long expiryMin) {
        this.expiryMin = expiryMin;
        this.expiryTimeStamp = getNow() + (expiryMin * 60L);
    }

    public Long getExpiryTimeStamp() {
        return expiryTimeStamp;
    }

    @JsonbTransient
    @JsonIgnore
    public List<TimerEvent> getTimerEvents() {
        return new ArrayList<>(Arrays.asList(timerEventsArray));

    }

    public void setExpiryTimeStamp(Long expiryTimeStamp) {
        this.expiryTimeStamp = expiryTimeStamp;
    }

    public void setTimerEvents(List<TimerEvent> events) {
        timerEventsArray = new TimerEvent[events.size()];
        this.timerEventsArray = events.toArray(timerEventsArray);
    }

    public void add(final String timerEventString) {
        add(new TimerEvent(timerEventString));
    }

    public Boolean getExpired() {
        return expired;
    }

    public Boolean isExpired() {
        return expired;
    }

    public void setExpired(Boolean expired) {
        this.expired = expired;
    }

    public void add(TimerEvent timerEvent) {
        // check if the same eventCode is already in the queue. If so then replace
        Boolean replaced = false;
        for (int j = 0; j < this.timerEventsArray.length; j++) {
            if (this.timerEventsArray[j].getUniqueCode().equals(timerEvent.getUniqueCode())) {
                this.timerEventsArray[j] = timerEvent;
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            // Add to the end of the array
            TimerEvent[] newTimerEventsArray = new TimerEvent[this.timerEventsArray.length + 1];
            for (int j = 0; j < this.timerEventsArray.length; j++) {
                newTimerEventsArray[j] = this.timerEventsArray[j];
            }
            newTimerEventsArray[this.timerEventsArray.length] = timerEvent;
            this.timerEventsArray = newTimerEventsArray; // replace the array
        }

        // Now push the expiryTime to always be later than the last event
        if (this.expiryTimeStamp.equals(DEFAULT_TIMER_EXPIRY_SECONDS)) {
            // This means that we need to seed the expiryTimestamp because a timer event has
            // been added
            this.expiryTimeStamp = timerEvent.getTimeStamp() + OFFSET_EXPIRY_SECONDS;
        }
        if (timerEvent.getTimeStamp() > this.expiryTimeStamp) {
            this.expiryTimeStamp = timerEvent.getTimeStamp() + OFFSET_EXPIRY_SECONDS;
        }

        Arrays.sort(this.timerEventsArray, new TimerEventComparator());
        this.currentMilestone = this.getNextMilestone();
    }

    @JsonbTransient
    @JsonIgnore
    public Long getNow() {
        return LocalDateTime.now().atZone(
                ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of("UTC")).toEpochSecond();
    }

    @JsonbTransient
    @JsonIgnore
    public String getNowUTC() {
        return LocalDateTime.now().atZone(
                ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of("UTC")).toString();
    }

    public TimerEvent[] getTimerEventsArray() {
        return timerEventsArray;
    }

    public void setTimerEventsArray(TimerEvent[] timerEventsArray) {
        this.timerEventsArray = timerEventsArray;
    }

    @Override
    public String toString() {
        return "TimerData [intervalStr=" + getIntervalStr() + ", elapsedMin=" + elapsedMin + ", expiryMin=" + expiryMin
                + ",timerExpiry=" + expiryTimeStamp
                + ", intervalMin=" + intervalMin + ", hasExpired=" + this.hasExpired() + ", currentMilestone="
                + getCurrentMilestone() + ",events=" + timerEventsArray + "]";
    }

    class TimerEventComparator implements Comparator<TimerEvent> {

        // Overriding compare()method of Comparator
        // for descending order of cgpa
        public int compare(TimerEvent t1, TimerEvent t2) {
            if (t1.getTimeStamp() > t2.getTimeStamp())
                return 1;
            else if (t1.getTimeStamp() < t2.getTimeStamp())
                return -1;
            return 0;
        }
    }

}
