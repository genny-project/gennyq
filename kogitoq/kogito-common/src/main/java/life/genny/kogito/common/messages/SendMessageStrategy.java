package life.genny.kogito.common.messages;

import life.genny.qwandaq.entity.BaseEntity;

import java.util.Map;

public interface SendMessageStrategy {
    String templateCode = null;
    BaseEntity recipientBE = null;
    Map<String, String> ctxMap = null;
    public void sendMessage();
}
