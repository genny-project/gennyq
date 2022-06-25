package life.genny.kogito.common.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;

import org.jboss.logging.Logger;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.serviceq.Service;

@ApplicationScoped
public class TaskService {

    private static final Logger log = Logger.getLogger(TaskService.class);

    Jsonb jsonb = JsonbBuilder.create();

    @Inject
    DatabaseUtils databaseUtils;

    @Inject
    QwandaUtils qwandaUtils;

	@Inject
	UserToken userToken;

    @Inject
    EntityManager entityManager;

    @Inject
    Service service;

    public void enableTaskQuestion(Ask ask, Boolean enabled) {

        ask.setDisabled(!enabled);

        QDataAskMessage askMsg = new QDataAskMessage(ask);
        askMsg.setToken(userToken.getToken());
        askMsg.setReplace(true);
        KafkaUtils.writeMsg("webcmds", askMsg);
    }

    public void hideTaskQuestion(Ask ask, Boolean hidden) {

        // Hide and Disable
        ask.setHidden(!hidden);
        ask.setDisabled(!hidden);

        QDataAskMessage askMsg = new QDataAskMessage(ask);
        askMsg.setToken(userToken.getToken());
        askMsg.setReplace(true);
        KafkaUtils.writeMsg("webcmds", askMsg);
    }
}
