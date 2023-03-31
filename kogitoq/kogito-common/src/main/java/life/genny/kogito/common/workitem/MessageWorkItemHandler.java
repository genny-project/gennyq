package life.genny.kogito.common.workitem;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.kie.kogito.internal.process.runtime.KogitoWorkItem;
import org.kie.kogito.internal.process.runtime.KogitoWorkItemHandler;
import org.kie.kogito.internal.process.runtime.KogitoWorkItemManager;

import life.genny.kogito.common.messages.SendMessageService;

@ApplicationScoped
public class MessageWorkItemHandler implements KogitoWorkItemHandler {

    @Inject
    Logger log;

    @Inject
    SendMessageService sendMessageService;

    public static final String CTX = "CTX.";

    @PostConstruct
    public void init(){
        log.info("MessageWorkItemHandler initialized!");
    }

    @Override
    public void abortWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager manager) {
        log.error("Message WorkItem Handler error!");
    }

    @Override
    public void executeWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager manager) {
        try {
            String messageCode = (String) workItem.getParameter("messageCode");
            log.info("messageCode: " + messageCode);
            String recipientCode = (String) workItem.getParameter("recipientCode");
            log.info("recipientCode: " + recipientCode);

            Map<String, String> ctxMap = new HashMap<>();

            for(String parameter : workItem.getParameters().keySet()) {
                if(parameter.startsWith(CTX)){
                    String key = parameter.replace(CTX, "");
                    ctxMap.put(key,  (String) workItem.getParameters().get(parameter));
                }
            }
            log.info("contextMap: "+ ctxMap);
            sendMessageService.send(messageCode, recipientCode, ctxMap);

            Map<String, Object> results = new HashMap<String, Object>();
            manager.completeWorkItem(workItem.getStringId(), results);
        } catch (Exception ex) {
            log.error("Exception: " + ex);
        }
    }
}
