package life.genny.messages.live.factory;

import life.genny.messages.live.qualifer.MessageType;
import life.genny.messages.managers.QMessageProvider;
import life.genny.qwandaq.message.QBaseMSGMessageType;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static life.genny.qwandaq.message.QBaseMSGMessageType.*;

@ApplicationScoped
public class QMessageFactory {

    @Inject
    @MessageType(type = SMS)
    QMessageProvider smsManager;

    @Inject
    @MessageType(type = SENDGRID_RELAY)
    QMessageProvider sendGridRelayManager;

    @Inject
    @MessageType(type = TOAST)
    QMessageProvider toastManager;

    @Inject
    @MessageType(type = SENDGRID)
    QMessageProvider sendGridManager;

    @Inject
    @MessageType(type = SLACK)
    QMessageProvider slackManager;

    @Inject
    @MessageType(type = DEFAULT)
    QMessageProvider errorManager;

    public QMessageProvider getMessageProvider(QBaseMSGMessageType messageType) {
        return switch (messageType) {
            case SMS -> smsManager;
            case SENDGRID_RELAY -> sendGridRelayManager;
            case TOAST -> toastManager;
            case SENDGRID -> sendGridManager;
            case SLACK -> slackManager;
            default -> errorManager;
        };
    }
}
