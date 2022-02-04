package life.genny.kogito.custom.sendmessage.config;

import java.util.HashMap;
import java.util.Map;

import org.kie.kogito.internal.process.runtime.KogitoWorkItem;
import org.kie.kogito.internal.process.runtime.KogitoWorkItemHandler;
import org.kie.kogito.internal.process.runtime.KogitoWorkItemManager;

public class SendMessageWorkItemHandler implements KogitoWorkItemHandler {

    @Override
    public void executeWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager manager) {
        System.out.println("Hello from the custom work item definition.");
        System.out.println("Passed parameters:");

        // Printing task’s parameters, it will also print
        // our value we pass to the task from the process
        for (String parameter : workItem.getParameters().keySet()) {
            // if ("messageCode".equals(parameter)) {
            //     MessageCode mc = (MessageCode) workItem.getParameters().get(parameter);
            //     System.out.println("MessageCode=" + mc);
            // } else {
            System.out.println(parameter + " = " + workItem.getParameters().get(parameter));
            // }
        }

        // Test sending message

        // GennyToken userToken = new GennyToken(token);
        // BaseEntityUtils beUtils = new BaseEntityUtils();
        // BaseEntity recipient = beUtils.getBaseEntityByCode(userToken.getUserCode())

        // QMessageGennyMSG sendGridMsg = new QMessageGennyMSG.Builder("MSG_IM_INTERN_LOGBOOK_REMINDER")
        //         .addRecipient(recipient)
        //         .setUtils(beUtils)
        //         .send();

        Map<String, Object> results = new HashMap<String, Object>();
        results.put("Result", "Message Returned from Work Item Handler");
        // Don’t forget to finish the work item otherwise the process
        // will be active infinitely and never will pass the flow
        // to the next node.
        manager.completeWorkItem(workItem.getStringId(), results);
    }

    @Override
    public void abortWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager manager) {
        System.err.println("Error happened in the custom work item definition.");
    }
}
