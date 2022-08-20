package life.genny.kogito.common.messages;

import java.util.Map;
import java.util.List;
import java.util.HashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.message.QMessageGennyMSG;
import life.genny.qwandaq.models.UserToken;

import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.DefUtils;
import life.genny.qwandaq.utils.QwandaUtils;

import life.genny.qwandaq.utils.SearchUtils;
import life.genny.qwandaq.entity.SearchEntity;

@ApplicationScoped
public class SendMessageService {

	private static final Logger log = Logger.getLogger(SendMessageService.class);

	Jsonb jsonb = JsonbBuilder.create();

	/**
	 * Send a genny message.
	 *
	 * @param templateCode    The template to use
	 * @param recipientBEJson The recipient BaseEntity json
	 */
	public void sendMessage(String templateCode, String recipientBECode) {
		new SendMessage(templateCode, recipientBECode).sendMessage();
	}

	/**
	 * Send a genny message.
	 *
	 * @param templateCode    The template to use
	 * @param recipientBEJson The recipient BaseEntity json
	 * @param ctxMap          The map of contexts to use
	 */
	public void sendMessage(String templateCode, String recipientBECode, Map<String, String> ctxMap) {
		new SendMessage(templateCode, recipientBECode, ctxMap).sendMessage();
	}

	/**
	 * Send a genny message.
	 *
	 * @param templateCode    The template to use
	 * @param recipientBEJson The recipient BaseEntity json
	 * @param ctxMap          The map of contexts to use
	 */
	public void sendMessage(String templateCode, BaseEntity recipientBE, Map<String, String> ctxMap) {
		new SendMessage(templateCode, recipientBE, ctxMap).sendMessage();
	}

	/**
	 * Send all genny messages for a given milestone code and coreBE code.
	 * 
	 * @param milestoneCode The workflow location to send messages for
	 *
	 * @param coreBEcode    The core BaseEntity code for which all Contexts can be
	 *                      derived.
	 */
	public void sendAllMessagesCode(String milestoneCode, String coreBeCode) {
		new SendAllMessages(milestoneCode, coreBeCode).sendAllMessages();
	}

	/**
	 * Send all genny messages for a given milestone code.
	 * 
	 * @param milestoneCode The workflow location to send messages for
	 *
	 * @param coreBEJson    The core BaseEntity json for which all Contexts can be
	 *                      derived.
	 */
	public void sendAllMessagesJson(String milestoneCode, String coreBEJson) {
		// This is ugly. I Need to change this bit later.
		log.info("For milestoneCode : " + milestoneCode + " with the coreBEJson:" + coreBEJson);
		BaseEntity coreBE = jsonb.fromJson(coreBEJson, BaseEntity.class);
		String productCode = coreBE.getRealm();
		log.info("productCode is " + productCode);

		new SendAllMessages(productCode, milestoneCode, coreBE).sendAllMessages();
	}

	/**
	 * Send all genny messages for a given milestone code.
	 * 
	 * @param productCode.  The productCode to use.
	 * @param milestoneCode The workflow location to send messages for
	 *
	 * @param coreBE        The core BaseEntity for which all Contexts can be
	 *                      derived.
	 */
	public void sendAllMessages(String productCode, String milestoneCode, BaseEntity coreBE) {
		new SendAllMessages(productCode, milestoneCode, coreBE).sendAllMessages();
	}
}