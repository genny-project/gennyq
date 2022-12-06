package life.genny.kogito.common.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jboss.logging.Logger;

import javax.json.bind.annotation.JsonbTransient;
import java.io.Serializable;
import java.time.*;

public class TimerEvent implements Serializable {

    static final Logger log = Logger.getLogger(TimerEvent.class);

    private Long timeStamp; // UTC timestamp in seconds that this event should trigger
    private String uniqueCode; // This is the unique milestone code for this event. Used primarily for
                               // messaging
    private String updatePairs; // These are the pairs of code and value updates that need to be set at this
                                // milestone

    public TimerEvent() {
        // TODO document why this constructor is empty
    }

    public TimerEvent(final String timerEventString) {
        // Split up into TimerEvents
        String[] timerEventStrArray = timerEventString.split(";");

        Long timerEventTimeStamp = Long.parseLong(timerEventStrArray[0]);
        if (timerEventTimeStamp < 1000000L) {
            // This is not an absolute timestamp, it is an interval in minutes, so provide
            // the current time and add it
            this.timeStamp = (timerEventTimeStamp * 60L)
                    + getNow();
        }
        this.timeStamp = timerEventTimeStamp;
        this.uniqueCode = timerEventStrArray[1];
        if (timerEventStrArray.length > 2) {
            this.updatePairs = timerEventStrArray[2];
        }

    }

    public TimerEvent(final Long timeStamp, final String uniqueCode, final String updatepairs) {
        // log.info("Init2 TimerEvent timeStamp = " + timeStamp + " , " +
        // getDateTime(timeStamp));
        if (timeStamp < 1000000L) {
            // This is not an absolute timestamp, it is an interval in minutes, so provide
            // the current time and add it
            this.timeStamp = (timeStamp * 60L) // convert to seconds
                    + getNow();

        } else {
            this.timeStamp = timeStamp;
        }
        log.info("2TimerEvent incoming timeStamp = " + timeStamp + " , final:" + this.timeStamp + " , "
                + getDateTime(this.timeStamp));
        this.uniqueCode = uniqueCode;
        this.updatePairs = updatepairs;
    }

    public TimerEvent(final LocalDateTime startDateTimeUTC, final Long minutes, final String uniqueCode,
            final String updatePairs) {
        this(startDateTimeUTC.atZone(
                ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of("UTC")).plusMinutes(minutes).toEpochSecond(),
                uniqueCode, updatePairs);
    }

    public TimerEvent(final ZonedDateTime eventZonedDateTimeUTC, final String uniqueCode, final String updatePairs) {
        // Convert to Zoned UTC
        this(eventZonedDateTimeUTC.withZoneSameInstant(ZoneId.of("UTC")).toEpochSecond(), uniqueCode, updatePairs);
    }

    public TimerEvent(final LocalDateTime eventDateTime, final String uniqueCode, final String updatePairs) {
        // Convert to Zoned UTC
        this(eventDateTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC")).toEpochSecond(),
                uniqueCode, updatePairs);
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getUniqueCode() {
        return uniqueCode;
    }

    public void setUniqueCode(String uniqueCode) {
        this.uniqueCode = uniqueCode;
    }

    public String getUpdatePairs() {
        return updatePairs;
    }

    public void setUpdatePairs(String updatePairs) {
        this.updatePairs = updatePairs;
    }

    @JsonbTransient
    @JsonIgnore
    public String getDateTimeUTC() {
        return ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(timeStamp),
                ZoneId.of("UTC")).toString();
    }

    @JsonbTransient
    @JsonIgnore
    public String getDateTime(final String timezoneId) {
        return ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(timeStamp),
                ZoneId.of(timezoneId)).toString();
    }

    @JsonbTransient
    @JsonIgnore
    public String getDateTime(final Long epochSeconds) {
        return LocalDateTime.ofEpochSecond(epochSeconds, 0, ZoneOffset.UTC).toString();
    }

    @JsonbTransient
    @JsonIgnore
    public Long getNow() {
        return LocalDateTime.now().atZone(
                ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of("UTC")).toEpochSecond();
    }

    @Override
    public String toString() {
        return "TimerEvent [updatePairs=" + updatePairs + ", timeStamp=" + timeStamp + " (UTC:" + getDateTimeUTC()
                + "), uniqueMilestoneCode=" + uniqueCode + "]";
    }

}
