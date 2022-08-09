package life.genny.messages.managers;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.message.QBaseMSGMessageType;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.utils.CommonUtils;

@ApplicationScoped
public class QMessageFactory {

	private static final Logger log = Logger.getLogger(QMessageFactory.class);

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

		log.info("message type::"+messageType.toString());

		switch(messageType) {

			case SMS:
				provider = smsManager;
				break;
			case EMAIL:
				provider = emailManager;
				break;
			case TOAST:
				provider = toastManager;
				break;
			case SENDGRID:
				provider = sendGridManager;
				break;
			case SLACK:
				provider = slackManager;
				break;
			// Default to Error Manager if no proper message is found
			case DEFAULT:
			default:
				provider = errorManager;    
		}

        if(provider == null) {
            CommonUtils.logAndReturn(log::error, ANSIColour.RED + ">>>>>> Provider is NULL for entity: " + ", msgType: " + messageType.toString() + " <<<<<<<<<" + ANSIColour.RESET);
            throw new NullParameterException("Provider for message type: " + messageType.toString());
        }

		return provider;

	}

}
