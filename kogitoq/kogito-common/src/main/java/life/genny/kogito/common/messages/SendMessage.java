package life.genny.kogito.common.messages;

import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.message.QMessageGennyMSG;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.util.Map;

public class SendMessage extends MessageSendingStrategy{

    private String templateCode;
    private BaseEntity recipientBE;
    private Map<String, String> ctxMap = null;
    public SendMessage(String templateCode, String recipientBECode) {
        this.templateCode = templateCode;
        this.recipientBE = beUtils.getBaseEntityByCode(recipientBECode);
    }

    public SendMessage(String templateCode, String recipientBECode, Map<String, String> ctxMap) {
        this.templateCode = templateCode;
        this.recipientBE = beUtils.getBaseEntityByCode(recipientBECode);
        this.ctxMap = ctxMap;
    }

    public SendMessage(String templateCode, BaseEntity recipientBE, Map<String, String> ctxMap) {
        this.templateCode = templateCode;
        this.recipientBE = recipientBE;
        this.ctxMap = ctxMap;
    }

    @Override
    public void sendMessage() {
        log.info("templateCode : " + templateCode);
        log.info("recipientBE (found BaseEntity): " + (recipientBE != null ? recipientBE.getCode() : "null"));
        log.info("ctxMap : " + (ctxMap != null ? jsonb.toJson(ctxMap) : "null"));

        QMessageGennyMSG.Builder msgBuilder = new QMessageGennyMSG.Builder(templateCode);

        if (ctxMap != null) {
            msgBuilder.setMessageContextMap(ctxMap);
        }

        msgBuilder.addRecipient(recipientBE)
                .setUtils(beUtils)
                .setToken(userToken)
                .send();
    }
}
