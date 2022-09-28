package life.genny.kogito.common.messages;

import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.SearchUtils;
import org.jboss.logging.Logger;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import io.quarkus.arc.Arc;

public abstract class MessageSendingStrategy {
    static final Logger log = Logger.getLogger(MessageSendingStrategy.class);

    Jsonb jsonb = JsonbBuilder.create();

    UserToken userToken;
    SearchUtils searchUtils;
    BaseEntityUtils beUtils;

    public abstract void sendMessage();

    public MessageSendingStrategy() {
        beUtils = Arc.container().select(BaseEntityUtils.class).get();
        userToken = Arc.container().select(UserToken.class).get();
        searchUtils = Arc.container().select(SearchUtils.class).get();

        log.info("Init beUtils: " + beUtils);
        log.info("Init UserToken: " + userToken);
        log.info("Init Search utils: " + searchUtils);
    }
}
