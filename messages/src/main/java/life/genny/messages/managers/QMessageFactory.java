package life.genny.messages.managers;

import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.message.QBaseMSGMessageType;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.utils.CommonUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class QMessageFactory {
	
	@Inject
	Logger log;

	@Inject
	QSMSMessageManager smsManager;

	@Inject
	QEmailMessageManager emailManager;

	@Inject
	QToastMessageManager toastManager;

	@Inject
	QSendGridMessageManager sendGridManager;

	@Inject
	QSlackMessageManager slackManager;

	@Inject
	QErrorManager errorManager;

	public QMessageProvider getMessageProvider(QBaseMSGMessageType messageType) {

		QMessageProvider provider;

		log.info("message type::" + messageType.toString());

		provider = switch (messageType) {
			case SMS -> smsManager;
			case EMAIL -> emailManager;
			case TOAST -> toastManager;
			case SENDGRID -> sendGridManager;
			case SLACK -> slackManager;
			// Default to Error Manager if no proper message is found
//			case DEFAULT,
			default -> errorManager;
		};

		if (provider == null) {
			log.error(ANSIColour.doColour(">>>>>> Provider is NULL for entity: " + ", msgType: " + messageType + " <<<<<<<<<", ANSIColour.RED));
			throw new NullParameterException("Provider for message type: " + messageType);
		}

		return provider;

	}

}
