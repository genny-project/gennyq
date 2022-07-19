package life.genny.kogito.common.service;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.message.QCommsMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;

@ApplicationScoped
public class SendMessageService {

	private static final Logger log = Logger.getLogger(SendMessageService.class);

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	BaseEntityUtils beUtils;

	/**
	* Send a genny message.
	*
	* @param templateCode The template to use
	* @param recipientBEJson The recipient BaseEntity json
	 */
	public void sendMessage(String templateCode, String recipientBECode) {
		sendMessage(templateCode, recipientBECode, null);
	}

	/**
	* Send a genny message.
	*
	* @param templateCode The template to use
	* @param recipientBEJson The recipient BaseEntity json
	* @param ctxMap The map of contexts to use
	 */
	public void sendMessage(String templateCode, String recipientBECode, Map<String, String> ctxMap) {
		log.info("recipientBECode : " + recipientBECode);
		BaseEntity recipientBe = beUtils.getBaseEntity(recipientBECode);
		sendMessage(templateCode, recipientBe, ctxMap);
	}


	/**
	* Send a genny message.
	*
	* @param templateCode The template to use
	* @param recipientBEJson The recipient BaseEntity json
	* @param ctxMap The map of contexts to use
	 */
	public void sendMessage(String templateCode, BaseEntity recipientBE, Map<String, String> ctxMap) {

		log.info("templateCode : " + templateCode);
		log.info("recipientBE (found BaseEntity): " + (recipientBE != null ? recipientBE.getCode() : "null"));
		log.info("ctxMap : " + (ctxMap != null ? jsonb.toJson(ctxMap) : "null"));

		QCommsMessage.Builder msgBuilder = new QCommsMessage.Builder(templateCode);
		if (ctxMap != null) {
			msgBuilder.setMessageContextMap(ctxMap);
		}
		msgBuilder.addRecipient(recipientBE)
			.setToken(userToken)
			.send();
	}
}
