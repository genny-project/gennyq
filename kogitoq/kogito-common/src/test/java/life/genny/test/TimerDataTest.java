package life.genny.test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import life.genny.kogito.common.models.TimerData;
import life.genny.kogito.common.models.TimerEvent;

//@QuarkusTest
public class TimerDataTest {
    private static final Logger log = Logger.getLogger(TimerDataTest.class);

    Jsonb jsonb = JsonbBuilder.create();

    @Test
    public void TimerEventTest() {
        System.out.println("This is the TimerEventTest");

        TimerEvent t1 = new TimerEvent(0L, "TEST_TIMER_1", "LNK_WARNING");
        System.out.println("TimerEvent1 = " + t1);

        TimerEvent t2 = new TimerEvent(1L, "TEST_TIMER_2", "LNK_DANGER");
        System.out.println("TimerEvent2 = " + t2);

        TimerEvent t3 = new TimerEvent(2L, "TEST_TIMER_3", "LNK_DEAD");
        System.out.println("TimerEvent3 = " + t3);

        TimerData timerData = new TimerData();
        // test sorting
        timerData.add(t1);
        timerData.add(t3);
        timerData.add(t2);

        // Force expiry time
        timerData.setExpiryMin(3L);

        System.out.println("Timer Data = " + timerData);

        for (int t = 0; t < 200; t++) {
            timerData.updateElapsed();

            if (timerData.hasExpired()) {
                System.out.println("TimerData is expired");
                break;
            }

            Long currentTimeStampUTC = LocalDateTime.now().atZone(
                    ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of("UTC")).toEpochSecond();
            if (timerData.isMilestone()) {
                timerData.updateMilestone();
                System.out.println(
                        "\n current epoch: " + currentTimeStampUTC + " , elapsed:" + timerData.getElapsedMin()
                                + " minutes, current Milestone is: " + timerData.getCurrentMilestone().getUniqueCode()
                                + ", isMilestone is "
                                + (timerData.isMilestone() ? "TRUE" : "FALSE")
                                + ",isExpired is " + (timerData.hasExpired() ? "TRUE" : "FALSE")
                                + ", next Milestone at : "
                                + timerData.getNextMilestone());

                System.out.println("");
                System.out.flush();
            } else {
                System.out.print(".");
                System.out.flush();
            }
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException e) {

            }

        }

    }
}
