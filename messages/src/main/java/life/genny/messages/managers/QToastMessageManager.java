package life.genny.messages.managers;

import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.message.QCmdMessage;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.MergeUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public final class QToastMessageManager extends QMessageProvider {

    private static final Logger log = Logger.getLogger(QToastMessageManager.class);

    @Override
    public void sendMessage(BaseEntity templateBe, Map<String, Object> contextMap) {

        log.info("About to send TOAST message!");

        BaseEntity target = (BaseEntity) contextMap.get("RECIPIENT");

        if (target == null) {
            log.error("Target is NULL");
            return;
        }

        // Check for Toast Body
        String body = null;
        if (contextMap.containsKey("BODY")) {
            body = (String) contextMap.get("BODY");
        } else {
            body = templateBe.getValue("PRI_BODY", null);
        }
        if (body == null) {
            log.error("body is NULL");
            return;
        }

        // Check for Toast Style
        String style = null;
        if (contextMap.containsKey("STYLE")) {
            style = (String) contextMap.get("STYLE");
        } else {
            style = templateBe.getValue("PRI_STYLE", "INFO");
        }
        if (style == null) {
            log.error("style is NULL");
            return;
        }

        // Mail Merging Data
        body = MergeUtils.merge(body, contextMap);

        // build toast command msg
        QCmdMessage msg = new QCmdMessage("TOAST", style);
        msg.setMessage(body);
        msg.setToken(userToken.getToken());
        msg.setSend(true);

        // send to frontend
        KafkaUtils.writeMsg(KafkaTopic.WEBCMDS, msg);
    }

}
