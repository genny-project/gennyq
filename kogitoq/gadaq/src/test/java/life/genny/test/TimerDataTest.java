package life.genny.test;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import life.genny.kogito.common.models.TimerData;
import life.genny.kogito.common.models.TimerEvent;

public class TimerDataTest {

    private static final Logger log = Logger.getLogger(TimerDataTest.class);

    Jsonb jsonb = JsonbBuilder.create();

    @Test
    public void TimerEventTest() {

        TimerEvent t1 = new TimerEvent(0L, "TEST_TIMER_1", "LNK_WARNING");
        TimerData timerData = new TimerData();
        timerData.add(t1);

        log.info("Timer Data = " + timerData);

        timerData.updateElapsed();

		if (timerData.isMilestone())
			timerData.updateMilestone();

		timerData.updateElapsed();

    }
}
