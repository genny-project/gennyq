package life.genny.kogito.custom.sendmessage.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import life.genny.qwandaq.message.QDataBaseEntityMessage;
import org.acme.messages.MessageCode;
import org.kie.kogito.internal.process.runtime.KogitoWorkItem;
import org.kie.kogito.internal.process.runtime.KogitoWorkItemHandler;
import org.kie.kogito.internal.process.runtime.KogitoWorkItemManager;

import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.message.QMessageGennyMSG;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.KeycloakUtils;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import org.jboss.logging.Logger;

public class SendMessageWorkItemHandler implements KogitoWorkItemHandler {

    private static final Logger log = Logger.getLogger(SendMessageWorkItemHandler.class);

    @Override
    public void executeWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager manager) {
        log.info("Hello from the custom work item definition2.");

        MessageCode mc = null;
        String realMessageCode = null;
        BaseEntity be = null;
        QDataBaseEntityMessage msg = null;
        log.info("Passed parameters:");

        // Printing task’s parameters, it will also print
        // our value we pass to the task from the process
        System.out.println("MessageCodes being checked");
        for (String parameter : workItem.getParameters().keySet()) {
            log.info("Parameter = "+parameter+"--->");
            if ("messageCode".equals(parameter)) {
                mc = (MessageCode) workItem.getParameters().get(parameter);
                realMessageCode = mc.getCode();
                System.out.println("MessageCode=" + mc);
            } else if ("messageCodeBE".equals(parameter)) {
                System.out.println("BaseEntity detected");
                be = (BaseEntity) workItem.getParameters().get(parameter);
                if (be != null) {
                    realMessageCode = be.getCode();
                    log.info("BaseENTITYCode=" + be.getCode());
                } else if ("messageCodeQ".equals(parameter)) {
                    log.info("QDataBaseEntity detected");
                    msg = (QDataBaseEntityMessage) workItem.getParameters().get(parameter);
                    if (msg != null) {
                        // Get BaseEntities
                        if (msg.getItems() != null) {
                            if (msg.getItems().size() > 0) {
                                be = msg.getItems().get(0);
                                realMessageCode = be.getCode();
                                log.info("BaseEntityCode=" + be.getCode() + " from QDAtabaseEntity:" + msg);
                            }
                        }
                    }
                }
            } else {
                log.info(parameter + " = " + workItem.getParameters().get(parameter));
            }
        }

        System.out.println("realMessageCode= " + realMessageCode);
        // Test sending message
        GennyToken serviceToken = new KeycloakUtils().getToken("https://keycloak.gada.io", "internmatch", "admin-cli",
                null,
                "service", System.getenv("GENNY_SERVICE_PASSWORD"), null);
        System.out.println("ServiceToken = " + serviceToken.getToken());
        log.info("realMessageCode = " + serviceToken.getToken());
        BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken, serviceToken);

        SearchEntity searchBE = new SearchEntity("SBE_TESTUSER", "test user Search")
                .addSort("PRI_CREATED", "Created", SearchEntity.Sort.DESC)
                .addFilter("PRI_CODE", SearchEntity.StringFilter.EQUAL, "PER_C9E55D68-20FE-4CDC-A1D3-EE36468461DB")
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

            Jsonb jsonb = JsonbBuilder.create();

            log.info(jsonb.toJson(recipient));

            // Now test QDataBaseEntityMessage
            // msg = new QDataBaseEntityMessage(recipient);
            // msg.setToken(serviceToken.getToken());
            log.info(jsonb.toJson(msg));
            // // BaseEntity recipient =
            // beUtils.getBaseEntityByCode(userToken.getUserCode());

            QMessageGennyMSG sendGridMsg = new QMessageGennyMSG.Builder(realMessageCode)
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
        // log.error("Error happened in the custom work item definition.");
    }
}