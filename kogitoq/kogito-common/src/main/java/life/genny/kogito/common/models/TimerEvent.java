package life.genny.kogito.common.models;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;

import org.jboss.logging.Logger;

public class TimerEvent implements Serializable {

    static final Logger log = Logger.getLogger(TimerEvent.class);

    private Long timeStamp; // UTC timestamp in seconds that this event should trigger
    private String uniqueCode; // This is the unique code for this event. Used for messaging
    private String statusCode; // This is the status code for this event. Saved to the associated BaseEntity
                               // PRI_TIMER_STATUS

    public TimerEvent() {
        // TODO document why this constructor is empty
    }

    public TimerEvent(final String timerEventString) {
        // Split up into TimerEvents
        String[] timerEventStrArray = timerEventString.split(":");

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
            this.statusCode = timerEventStrArray[2];
        }

    }

    public TimerEvent(final Long timeStamp, final String uniqueCode, final String statusCode) {
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
        this.statusCode = statusCode;
    }

    public TimerEvent(final LocalDateTime startDateTimeUTC, final Long minutes, final String uniqueCode,
            final String statusCode) {
        this(startDateTimeUTC.atZone(
                ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of("UTC")).plusMinutes(minutes).toEpochSecond(),
                uniqueCode, statusCode);
    }

    public TimerEvent(final ZonedDateTime eventZonedDateTimeUTC, final String uniqueCode, final String statusCode) {
        // Convert to Zoned UTC
        this(eventZonedDateTimeUTC.withZoneSameInstant(ZoneId.of("UTC")).toEpochSecond(), uniqueCode, statusCode);
    }

    public TimerEvent(final LocalDateTime eventDateTime, final String uniqueCode, final String statusCode) {
        // Convert to Zoned UTC
        this(eventDateTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC")).toEpochSecond(),
                uniqueCode, statusCode);
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

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getDateTimeUTC() {
        return ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(timeStamp),
                ZoneId.of("UTC")).toString();
    }

    public String getDateTime(final String timezoneId) {
        return ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(timeStamp),
                ZoneId.of(timezoneId)).toString();
    }

    public String getDateTime(final Long epochSeconds) {
        return LocalDateTime.ofEpochSecond(epochSeconds, 0, ZoneOffset.UTC).toString();
    }

    public Long getNow() {
        return LocalDateTime.now().atZone(
                ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of("UTC")).toEpochSecond();
    }

    @Override
    public String toString() {
        return "TimerEvent [statusCode=" + statusCode + ", timeStamp=" + timeStamp + " (UTC:" + getDateTimeUTC()
                + "), uniqueCode=" + uniqueCode + "]";
    }

}
