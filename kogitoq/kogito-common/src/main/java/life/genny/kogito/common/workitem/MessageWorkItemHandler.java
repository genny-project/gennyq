package life.genny.kogito.common.workitem;

import static life.genny.qwandaq.attribute.Attribute.LNK_MESSAGE_TYPE;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.kie.kogito.internal.process.runtime.KogitoWorkItem;
import org.kie.kogito.internal.process.runtime.KogitoWorkItemHandler;
import org.kie.kogito.internal.process.runtime.KogitoWorkItemManager;

import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.message.QBaseMSGMessageType;
import life.genny.qwandaq.message.QMessageGennyMSG;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;

@ApplicationScoped
public class MessageWorkItemHandler implements KogitoWorkItemHandler {

    @Inject
    Logger log;

    @Inject
    UserToken userToken;

    @Inject
    BaseEntityUtils beUtils;

    public static final String CTX = "CTX.";

    public static final String RECIPIENT = "RECIPIENT";
    public static final String SOURCE = "SOURCE";
    public static final String TARGET = "TARGET";

    @PostConstruct
    public void init(){
        log.info("MessageWorkItemHandler initialized!");
    }

    @Override
    public void abortWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager manager) {
        log.error("MessageWorkItemHandler error!");
    }

    @Override
    public void executeWorkItem(KogitoWorkItem workItem, KogitoWorkItemManager manager) {
        String messageCode = (String) workItem.getParameter("messageCode");
        log.info("messageCode: " + messageCode);
        String recipientCode = (String) workItem.getParameter("recipientCode");
        log.info("recipientCode: " + recipientCode);

        // build the context map
        Map<String, String> ctxMap = new HashMap<>();
        for(String parameter : workItem.getParameters().keySet()) {
            if(parameter.startsWith(CTX)){
                String key = parameter.replace(CTX, "");
                ctxMap.put(key, (String) workItem.getParameters().get(parameter));
            }
        }

        log.info("contextMap: " + ctxMap);

        // find the template entity
        BaseEntity baseEntity;
        try {
            baseEntity = beUtils.getBaseEntity(messageCode);
        } catch (ItemNotFoundException e) {
            log.error("Message template not found: " + messageCode + "! Details: " + e.getMessage());
            return;
        }

        // find the message type selection
        BaseEntity messageType;
        try {
            messageType = beUtils.getBaseEntityFromLinkAttribute(baseEntity, LNK_MESSAGE_TYPE, true);
            log.debug("messageType: " + messageType.getCode());
        } catch (ItemNotFoundException e) {
            log.error("Message Type (" + LNK_MESSAGE_TYPE + ") not found in: " + messageCode + "! Details: " + e.getMessage());
            return;
        }

        // add valid message types
        QMessageGennyMSG.Builder msgBuilder = new QMessageGennyMSG.Builder(messageCode);
        for (QBaseMSGMessageType type : QBaseMSGMessageType.values()) {
            if (messageType.getCode().contains(type.name())) {
                msgBuilder.addMessageType(type);
            }
        }

        try {
            log.info("Sending message: " + messageCode);
            msgBuilder.setMessageContextMap(ctxMap);
            msgBuilder.addRecipient(recipientCode)
                    .setUtils(beUtils)
                    .setToken(userToken)
                    .setMessageContextMap(ctxMap)
                    .send();

            log.info("Triggered message!");
            log.info(ANSIColour.doColour("messageCode: " + messageCode, ANSIColour.GREEN));
            log.info(ANSIColour.doColour("recipientCode: " + recipientCode, ANSIColour.GREEN));
            log.info(ANSIColour.doColour("messageType: " + messageType, ANSIColour.GREEN));
            log.info(ANSIColour.doColour("ctxMap: " + ctxMap, ANSIColour.GREEN));

            Map<String, Object> results = new HashMap<String, Object>();
            manager.completeWorkItem(workItem.getStringId(), results);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
