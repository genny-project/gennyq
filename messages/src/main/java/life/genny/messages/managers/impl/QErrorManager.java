package life.genny.messages.managers.impl;

import life.genny.messages.live.qualifer.MessageType;
import life.genny.messages.managers.QMessageProvider;
import life.genny.qwandaq.models.ANSIColour;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;

import static life.genny.qwandaq.message.QBaseMSGMessageType.DEFAULT;

@ApplicationScoped
@MessageType(type=DEFAULT)
public final class QErrorManager extends QMessageProvider {

	@Inject
	Logger log;

	@Override
	public void sendMessage(Map<String, Object> contextMap) {

		/*
		 * If a message makes it to this point, then something is probably
		 * wrong with the message or the template.
		 */
		log.error(ANSIColour.doColour("Message Type Supplied was bad. Please check the Message and Template Code!!!!!", ANSIColour.RED));

	}
}
