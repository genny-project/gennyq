package life.genny.kogito.custom.askquestions.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.acme.messages.MessageCode;
import org.kie.kogito.internal.process.runtime.KogitoWorkItem;
import org.kie.kogito.internal.process.runtime.KogitoWorkItemHandler;
import org.kie.kogito.internal.process.runtime.KogitoWorkItemManager;

import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.KeycloakUtils;

public class AskQuestionsWorkItemHandler implements KogitoWorkItemHandler {

    // private static final Logger log =
    // Logger.getLogger(SendMessageWorkItemHandler.class);

    Jsonb jsonb = JsonbBuilder.create();

    @Override
    public void executeWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager manager) {
        System.out.println("Hello from the custom AskQuestions work item .");

        MessageCode mc = null;
        // log.info("Passed parameters:");

        // Printing task’s parameters, it will also print
        // our value we pass to the task from the process
        for (String parameter : workItem.getParameters().keySet()) {
            if ("messageCode".equals(parameter)) {
                mc = (MessageCode) workItem.getParameters().get(parameter);
                System.out.println("MessageCode=" + mc);
            } else {
                System.out.println(parameter + " = " + workItem.getParameters().get(parameter));
            }
        }

        // Test sending Questions
        GennyToken serviceToken = new KeycloakUtils().getToken("https://keycloak.gada.io", "internmatch", "admin-cli",
                null,
                "service", System.getenv("GENNY_SERVICE_PASSWORD"), null);
        System.out.println("ServiceToken = " + serviceToken.getToken());
        BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken, serviceToken);

        // Get the remote alyson token
        // Test sending message

        SearchEntity searchBE = new SearchEntity("SBE_TESTUSER", "test user Search")
                .addSort("PRI_CREATED", "Created", SearchEntity.Sort.DESC)
                .addFilter("PRI_CODE", SearchEntity.StringFilter.EQUAL, "PER_086CDF1F-A98F-4E73-9825-0A4CFE2BB943")
                .addColumn("PRI_CODE", "Code");

        searchBE.setRealm("internmatch");

        searchBE.setPageStart(0);
        Integer pageSize = 1;
        searchBE.setPageSize(pageSize);
        BaseEntity recipient = null;
        List<BaseEntity> recipients = beUtils.getBaseEntitys(searchBE); // load 100 at a time
        if (recipients != null) {

            if (recipients.size() > 0) {

                recipient = recipients.get(0);
                System.out.println("Recipient = " + recipient.getCode() + " with size = " + recipients.size());
            }
        }

        String userCode = recipient.getCode();
        String username = "testuser@gada.io";

        String userTokenStr = (String) CacheUtils.readCache("internmatch", "TOKEN:" + userCode);
        System.out.println("usercode = " + userCode + " CacheJsonStr=[" + userTokenStr + "]");
        GennyToken userToken = new GennyToken("userToken", userTokenStr);
        System.out.println(
                "User " + username + " is logged in! " + userToken.getAdecodedTokenMap().get("session_state"));

        // Fetch the Questions

        // Create the Ask

        // Send the Questions to the source user

        // Set up a UserTask

        Map<String, Object> results = new HashMap<String, Object>();
        results.put("Result", "Message Returned from Work Item Handler");
        // Don’t forget to finish the work item otherwise the process
        // will be active infinitely and never will pass the flow
        // to the next node.
        manager.completeWorkItem(workItem.getStringId(), results);
    }

    @Override
    public void abortWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager manager) {
        // log.error("Error happened in the custom work item definition.");
    }
}
