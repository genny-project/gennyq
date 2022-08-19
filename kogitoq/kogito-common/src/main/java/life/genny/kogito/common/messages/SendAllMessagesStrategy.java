package life.genny.kogito.common.messages;

import life.genny.qwandaq.entity.BaseEntity;

public interface SendAllMessagesStrategy {
    String productCode = null;
    String milestoneCode = null;
    BaseEntity coreBE = null;

    public void sendAllMessages();
}
