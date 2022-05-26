package life.genny.gadaq.service;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.message.QMessageGennyMSG;
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
	public void sendMessage(String templateCode, String recipientBEJson) {
		sendMessage(templateCode, recipientBEJson, null);
	}

	/**
	* Send a genny message.
	*
	* @param templateCode The template to use
	* @param recipientBEJson The recipient BaseEntity json
	* @param ctxMap The map of contexts to use
	 */
	public void sendMessage(String templateCode, String recipientBEJson, Map<String, String> ctxMap) {

		log.info("templateCode : " + templateCode);
		log.info("recipientBEJson : " + recipientBEJson);
		log.info("ctxMap : " + jsonb.toJson(ctxMap));

		BaseEntity recipient = jsonb.fromJson(recipientBEJson, BaseEntity.class);

		new QMessageGennyMSG.Builder(templateCode)
			.setMessageContextMap(ctxMap)
			.addRecipient(recipient)
			.setUtils(beUtils)
			.send();
	}
}
