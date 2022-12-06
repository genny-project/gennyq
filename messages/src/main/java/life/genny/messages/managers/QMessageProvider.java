package life.genny.messages.managers;


import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.util.Map;

@ApplicationScoped
public abstract sealed class QMessageProvider
        permits QEmailMessageManager, QErrorManager, QSlackMessageManager, QSMSMessageManager,
        QSendGridMessageManager, QToastMessageManager {

    @Inject
    BaseEntityUtils beUtils;

    @Inject
    protected UserToken userToken;

    protected static Jsonb jsonb = JsonbBuilder.create();

    public abstract void sendMessage(BaseEntity templateBe, Map<String, Object> contextMap);

}