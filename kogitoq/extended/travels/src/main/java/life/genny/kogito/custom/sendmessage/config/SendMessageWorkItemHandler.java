package life.genny.kogito.custom.sendmessage.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kie.kogito.internal.process.runtime.KogitoWorkItem;
import org.kie.kogito.internal.process.runtime.KogitoWorkItemHandler;
import org.kie.kogito.internal.process.runtime.KogitoWorkItemManager;

import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.message.QMessageGennyMSG;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.KeycloakUtils;

public class SendMessageWorkItemHandler implements KogitoWorkItemHandler {

    //   private static final Logger log = Logger.getLogger(SendMessageWorkItemHandler.class);

    @Override
    public void executeWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager manager) {
        System.out.println("Hello from the custom work item definition2.");
        //log.info("Passed parameters:");

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
        GennyToken serviceToken = new KeycloakUtils().getToken("https://keycloak.gada.io", "internmatch", "admin-cli", null,
                "service", System.getenv("GENNY_SERVICE_PASSWORD"), null);
        System.out.println("ServiceToken = " + serviceToken.getToken());
        BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken, serviceToken);

        SearchEntity searchBE = new SearchEntity("SBE_TESTUSER", "test user Search")
                .addSort("PRI_CREATED", "Created", SearchEntity.Sort.DESC)
                .addFilter("PRI_EMAIL", SearchEntity.StringFilter.EQUAL, "cto@gada.io")
                .addColumn("PRI_CODE", "Code");

        searchBE.setRealm("internmatch");

        searchBE.setPageStart(0);
        Integer pageSize = 1;
        searchBE.setPageSize(pageSize);

        List<BaseEntity> recipients = beUtils.getBaseEntitys(searchBE); // load 100 at a time
        if (recipients != null) {
            BaseEntity recipient = null;
            if (recipients.size() > 0) {

                recipient = recipients.get(0);
                System.out.println("Recipient = " + recipient.getCode());
            }

            // //  BaseEntity recipient = beUtils.getBaseEntityByCode(userToken.getUserCode());

            QMessageGennyMSG sendGridMsg = new QMessageGennyMSG.Builder("MSG_IM_INTERN_LOGBOOK_REMINDER")
                    .addRecipient(recipient)
                    .setUtils(beUtils)
                    .send();
        } else {
            System.out.println("No recipients matched search");
        }

        Map<String, Object> results = new HashMap<String, Object>();
        results.put("Result", "Message Returned from Work Item Handler");
        // Don’t forget to finish the work item otherwise the process
        // will be active infinitely and never will pass the flow
        // to the next node.
        manager.completeWorkItem(workItem.getStringId(), results);
    }

    @Override
    public void abortWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager manager) {
        //log.error("Error happened in the custom work item definition.");
    }
}
