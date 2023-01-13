package life.genny.kogito.common.messages;

import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.message.QMessageGennyMSG;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import life.genny.qwandaq.message.QBaseMSGMessageType;

public class SendMessage extends MessageSendingStrategy {

    private String templateCode;
    private BaseEntity recipientBE;
    private Map<String, String> ctxMap = null;

    static final Logger log = Logger.getLogger(SendMessage.class);

    private QBaseMSGMessageType msgType;

    public SendMessage(String templateCode, String recipientBECode) {
        super();
        this.templateCode = templateCode;
        this.recipientBE = beUtils.getBaseEntityByCode(recipientBECode);
    }

    public SendMessage(String templateCode, String recipientBECode, Map<String, String> ctxMap) {
        super();
        this.templateCode = templateCode;
        if (beUtils == null) {
            log.warn("beUtils is NULL --> no userToken");
        }
        this.recipientBE = beUtils.getBaseEntityByCode(recipientBECode);
        this.ctxMap = ctxMap;
    }

    public SendMessage(String templateCode, BaseEntity recipientBE, Map<String, String> ctxMap) {
        super();
        this.templateCode = templateCode;
        this.recipientBE = recipientBE;
        this.ctxMap = ctxMap;
    }

    public SendMessage(String templateCode, BaseEntity recipientBE, String url) {
        super();
        this.templateCode = templateCode;
        this.recipientBE = recipientBE;
        this.ctxMap = (Map<String, String>) new HashMap<>().put("URL:ENCODE", url);
    }

    public SendMessage(String templateCode,String recipientBECode,QBaseMSGMessageType msgType) {
        super();
        this.templateCode = templateCode;
        this.recipientBE = beUtils.getBaseEntity(recipientBECode);
        this.msgType = msgType;
        this.ctxMap = ctxMap;
    }

    public SendMessage(String templateCode,String recipientBECode,QBaseMSGMessageType msgType,Map<String, String> ctxMap) {
        super();
        this.templateCode = templateCode;
        this.recipientBE = beUtils.getBaseEntity(recipientBECode);
        this.msgType = msgType;
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

        if (this.msgType != null) {
            msgBuilder.addMessageType(this.msgType);
        }

        msgBuilder.addRecipient(recipientBE)
                .setUtils(beUtils)
                .setToken(userToken)
                .send();
    }
}
