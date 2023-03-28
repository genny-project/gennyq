package life.genny.messages.managers.impl;

import life.genny.messages.live.qualifer.MessageType;
import life.genny.messages.managers.QMessageProvider;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.message.QCmdMessage;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.MergeUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;

import static life.genny.messages.util.FailureHandler.optional;
import static life.genny.messages.util.FailureHandler.required;
import static life.genny.qwandaq.message.QBaseMSGMessageType.TOAST;

@ApplicationScoped
@MessageType(type = TOAST)
public final class QToastMessageManager extends QMessageProvider {

    @Inject
    Logger log;

    @Inject
    MergeUtils mergeUtils;

    @Override
    public void sendMessage(Map<String, Object> contextMap) {
        log.info(ANSIColour.doColour(">>>>>>>>>>> Triggering Toast <<<<<<<<<<<<<<", ANSIColour.GREEN));

        String messageCode = (String) contextMap.get("MESSAGE");
        String realm = userToken.getRealm();

        if (messageCode == null) {
            log.error(ANSIColour.RED + "message code is NULL" + ANSIColour.RESET);
            return;
        }

        if (realm == null) {
            log.error(ANSIColour.RED + "realm is NULL" + ANSIColour.RESET);
            return;
        }

        String body = null;
        if (contextMap.containsKey("BODY")) {
            body = (String) contextMap.get("BODY");
        } else {
            body = required(() -> beaUtils.getEntityAttribute(realm, messageCode, "PRI_BODY").getValueString());
        }

        String style = null;
        if (contextMap.containsKey("STYLE")) {
            style = (String) contextMap.get("STYLE");
        } else {
            style = optional(() -> beaUtils.getEntityAttribute(realm, messageCode, "PRI_STYLE").getValueString(), "INFO");
        }

        // Merging Data
        body = mergeUtils.merge(body, contextMap);

        // build toast command msg
        QCmdMessage msg = new QCmdMessage("TOAST", style);
        msg.setMessage(body);
        msg.setToken(userToken.getToken());
        msg.setSend(true);

        // send to frontend
        KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
    }

}
