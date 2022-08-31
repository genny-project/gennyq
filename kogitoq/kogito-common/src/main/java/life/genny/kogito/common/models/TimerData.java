package life.genny.kogito.common.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.time.ZoneId;
import java.util.PriorityQueue;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TimerData implements Serializable {

    static final Long DEFAULT_TIMER_INTERVAL_MIN = 1L;
    static final Long OFFSET_EXPIRY_SECONDS = 60L;// 24L * 60L * 60L; // Add a day
    static final Integer PRIORITY_QUEUE_INITIAL_SIZE = 3;

    static final Logger log = Logger.getLogger(TimerData.class);

    private Long intervalMin = DEFAULT_TIMER_INTERVAL_MIN; //
    private Long elapsedMin = 0L;
    private Long expiryMin = 3L;// 7L * 24L * 60L; // 7 days
    private Long expiryTimeStamp = 12448167224L; // This must be set during init, default to 20th June 2364

    // private PriorityQueue<TimerEvent> events = new
    // PriorityQueue<>(PRIORITY_QUEUE_INITIAL_SIZE,
    // new TimerEventComparator());

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
        return currentTimeStampUTC >= this.expiryTimeStamp;
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

    public TimerEvent nextMilestone() {
        if ((this.events != null) && (this.events.size() > 0)) {
            this.events.remove(0);
        }
        if ((this.events != null) && (this.events.size() > 0)) {
            return this.events.get(0);
        }
        return null;
    }

    public TimerEvent getNextTimerEvent() {
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

    public void setExpiryMin(Long expiryMin) {
        this.expiryMin = expiryMin;
    }

    public Long getExpiryTimeStamp() {
        return expiryTimeStamp;
    }

    public List<TimerEvent> getEvents() {
        return events;
    }

    public void setExpiryTimeStamp(Long expiryTimeStamp) {
        this.expiryTimeStamp = expiryTimeStamp;
    }

    public void setEvents(List<TimerEvent> events) {
        this.events = events;
    }

    public void add(final String timerEventString) {
        add(new TimerEvent(timerEventString));
    }

    public void add(TimerEvent timerEvent) {
        // check if the same eventCode is already in the queue. If so then replace
        Iterator<TimerEvent> itr = this.events.iterator();
        while (itr.hasNext()) {
            TimerEvent tv = (TimerEvent) itr.next();
            if (tv.getUniqueCode().equals(timerEvent.getUniqueCode())) {
                this.events.remove(tv);
            }
        }
        this.events.add(timerEvent);
        // Now push the expiryTime to always be later than the last event
        if (timerEvent.getTimeStamp() > this.expiryTimeStamp) {
            this.expiryTimeStamp = timerEvent.getTimeStamp() + OFFSET_EXPIRY_SECONDS;
        }

        Collections.sort(this.events, new TimerEventComparator());
    }

    public Long getNow() {
        return LocalDateTime.now().atZone(
                ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of("UTC")).toEpochSecond();
    }

    @Override
    public String toString() {
        return "TimerData [intervalStr=" + getIntervalStr() + ", elapsedMin=" + elapsedMin + ", expiryMin=" + expiryMin
                + ", intervalMin=" + intervalMin + ", hasExpired=" + this.hasExpired() + ", events=" + events + "]";
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
