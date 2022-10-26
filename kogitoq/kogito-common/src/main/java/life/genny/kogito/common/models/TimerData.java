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

    private TimerEvent[] timereventsArray = new TimerEvent[0];

    @JsonbTransient
    @JsonIgnore
    private List<TimerEvent> events = new ArrayList<>(PRIORITY_QUEUE_INITIAL_SIZE);

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
        if ((this.events != null) && (!this.events.isEmpty())) {
            TimerEvent firstEvent = this.events.get(0);
            if (firstEvent != null) {
                // log.info("Current UTC is " + currentTimeStampUTC + ", First event is " +
                // firstEvent);
                return firstEvent.getTimeStamp() <= currentTimeStampUTC;
            }
        }
        return false;
    }

    public TimerEvent updateMilestone() {
        if ((this.events != null) && (this.events.size() > 0)) {
            this.currentMilestone = this.events.get(0);
            this.events.remove(0);
        }
        if ((this.events != null) && (this.events.size() > 0)) {
            return this.events.get(0);
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
        if ((this.events != null) && (this.events.size() > 0)) {
            return this.events.get(0);
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
    }

    public Long getExpiryTimeStamp() {
        return expiryTimeStamp;
    }

    @JsonbTransient
    @JsonIgnore
    public List<TimerEvent> getEvents() {
        // events = new ArrayList<>(Arrays.asList(timereventsArray));
        return events;
    }

    public void setExpiryTimeStamp(Long expiryTimeStamp) {
        this.expiryTimeStamp = expiryTimeStamp;
    }

    public void setEvents(List<TimerEvent> events) {
        timereventsArray = new TimerEvent[events.size()];
        this.timereventsArray = events.toArray(timereventsArray);
        this.events = events;
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
        for (TimerEvent tv : this.events) {
            if (tv.getUniqueCode().equals(timerEvent.getUniqueCode())) {
                this.events.remove(tv);
            }
        }
        this.events.add(timerEvent);
        // Now push the expiryTime to always be later than the last event
        if (this.expiryTimeStamp.equals(DEFAULT_TIMER_EXPIRY_SECONDS)) {
            // This means that we need to seed the expiryTimestamp because a timer event has
            // been added
            this.expiryTimeStamp = timerEvent.getTimeStamp() + OFFSET_EXPIRY_SECONDS;
        }
        if (timerEvent.getTimeStamp() > this.expiryTimeStamp) {
            this.expiryTimeStamp = timerEvent.getTimeStamp() + OFFSET_EXPIRY_SECONDS;
        }

        Collections.sort(this.events, new TimerEventComparator());
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

    public TimerEvent[] getTimereventsArray() {
        return timereventsArray;
    }

    public void setTimereventsArray(TimerEvent[] timereventsArray) {
        this.timereventsArray = timereventsArray;
    }

    @Override
    public String toString() {
        return "TimerData [intervalStr=" + getIntervalStr() + ", elapsedMin=" + elapsedMin + ", expiryMin=" + expiryMin
                + ",timerExpiry=" + expiryTimeStamp
                + ", intervalMin=" + intervalMin + ", hasExpired=" + this.hasExpired() + ", currentMilestone="
                + getCurrentMilestone() + ",events=" + events + "]";
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
