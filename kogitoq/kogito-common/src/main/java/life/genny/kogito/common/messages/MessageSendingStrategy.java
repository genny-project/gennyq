package life.genny.kogito.common.messages;

import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

public abstract class MessageSendingStrategy {
    static final Logger log = Logger.getLogger(SendMessageService.class);

    Jsonb jsonb = JsonbBuilder.create();

    @Inject
    UserToken userToken;

    @Inject
    BaseEntityUtils beUtils;

    public abstract void sendMessage();
}
