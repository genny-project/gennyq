package life.genny.gadaq.utils;

import io.quarkus.arc.Arc;
import org.jboss.logging.Logger;
import org.kie.api.event.process.*;
import org.kie.kogito.internal.process.event.DefaultKogitoProcessEventListener;
import org.kie.kogito.internal.process.event.KogitoProcessEventListener;
import org.kie.kogito.internal.process.runtime.KogitoProcessInstance;

import java.lang.invoke.MethodHandles;

public class JtiEventListener extends DefaultProcessEventListener implements KogitoProcessEventListener {
    private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass());

    public JtiEventListener() {
    }

    public void beforeProcessStarted(ProcessStartedEvent event) {
        LOGGER.info("Starting workflow '{}' ({})");
    }

    public void afterProcessStarted(ProcessStartedEvent event) {
        if (event.getProcessInstance().getState() != 2) {
            LOGGER.info("Workflow '{}' ({}) was started, now '{}'", new Object[]{event.getProcessInstance().getProcessId(), ((KogitoProcessInstance)event.getProcessInstance()).getStringId(), getStatus(event.getProcessInstance().getState())});
        }
    }

    public void afterProcessCompleted(ProcessCompletedEvent event) {
        LOGGER.info("Workflow '{}' ({}) completed");
    }

    public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
        String nodeName = event.getNodeInstance().getNodeName();
        LOGGER.info("Triggered node '{}' for process '{}' ({})", new Object[]{nodeName, event.getProcessInstance().getProcessId(), ((KogitoProcessInstance)event.getProcessInstance()).getStringId()});
    }

    public void afterVariableChanged(ProcessVariableChangedEvent event) {
        LOGGER.info("Variable '{}' changed value from: '{}', to: '{}'", new Object[]{event.getVariableId(), event.getOldValue(), event.getNewValue()});
    }

    private static String getStatus(int status) {
        switch (status) {
            case 0:
                return "PENDING";
            case 1:
                return "ACTIVE";
            case 2:
                return "COMPLETED";
            case 3:
                return "ABORTED";
            case 4:
                return "SUSPENDED";
            default:
                return "UNKNOWN " + status;
        }
    }
}
