package life.genny.kogito.common.service;

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
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.DefUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;

import life.genny.qwandaq.utils.SearchUtils;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.message.QSearchBeResult;

@ApplicationScoped
public class SendMessageService {

	private static final Logger log = Logger.getLogger(SendMessageService.class);

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	SearchUtils searchUtils;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	DefUtils defUtils;

	/**
	 * Send a genny message.
	 *
	 * @param templateCode    The template to use
	 * @param recipientBEJson The recipient BaseEntity json
	 */
	public void sendMessage(String templateCode, String recipientBECode) {
		sendMessage(templateCode, recipientBECode, null);
	}

	/**
	 * Send a genny message.
	 *
	 * @param templateCode    The template to use
	 * @param recipientBEJson The recipient BaseEntity json
	 * @param ctxMap          The map of contexts to use
	 */
	public void sendMessage(String templateCode, String recipientBECode, Map<String, String> ctxMap) {
		log.info("recipientBECode : " + recipientBECode);
		BaseEntity recipientBe = beUtils.getBaseEntityByCode(recipientBECode);
		sendMessage(templateCode, recipientBe, ctxMap);
	}

	/**
	 * Send a genny message.
	 *
	 * @param templateCode    The template to use
	 * @param recipientBEJson The recipient BaseEntity json
	 * @param ctxMap          The map of contexts to use
	 */
	public void sendMessage(String templateCode, BaseEntity recipientBE, Map<String, String> ctxMap) {

		log.info("templateCode : " + templateCode);
		log.info("recipientBE (found BaseEntity): " + (recipientBE != null ? recipientBE.getCode() : "null"));
		log.info("ctxMap : " + (ctxMap != null ? jsonb.toJson(ctxMap) : "null"));

		QMessageGennyMSG.Builder msgBuilder = new QMessageGennyMSG.Builder(templateCode);
		if (ctxMap != null) {
			msgBuilder.setMessageContextMap(ctxMap);
		}
		msgBuilder.addRecipient(recipientBE)
				.setUtils(beUtils)
				.setToken(userToken)
				.send();
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
		log.info("For milestoneCode : " + milestoneCode + " with the coreBeCode:" + coreBeCode);
		BaseEntity coreBE = beUtils.getBaseEntity(userToken.getProductCode(), coreBeCode);
		String productCode = coreBE.getRealm();
		log.info("productCode is " + productCode);
		sendAllMessages(productCode, milestoneCode, coreBE);
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
		log.info("For milestoneCode : " + milestoneCode + " with the coreBEJson:" + coreBEJson);
		BaseEntity coreBE = jsonb.fromJson(coreBEJson, BaseEntity.class);
		String productCode = coreBE.getRealm();
		log.info("productCode is " + productCode);
		sendAllMessages(productCode, milestoneCode, coreBE);
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
		SearchEntity searchEntity = new SearchEntity("SBE_MILESTONE_MESSAGES",
				"Fetch All Messages associated with milestone Code")
				.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "MSG_%")
				.addFilter("PRI_MILESTONE", SearchEntity.StringFilter.LIKE, "%\"" + milestoneCode.toUpperCase() + "\"%")
				.setPageStart(0)
				.setPageSize(100);

		searchEntity.setRealm(productCode);

		List<String> messageCodes = searchUtils.searchBaseEntityCodes(searchEntity);
		if (messageCodes != null) {
			log.info("messages : " + messageCodes.size());
			for (String messageCode : messageCodes) {
				log.info("messageCode : " + messageCode);
				BaseEntity message = beUtils.getBaseEntityByCode(messageCode);
				// Construct a contextMap
				Map<String, String> ctxMap = new HashMap<>();
				String recipientBECode = null;

				// Determine the recipientBECode
				String recipientLnkValue = message.getValueAsString("PRI_RECIPIENT_LNK");
				if (recipientLnkValue != null) {
					// check the various formats to get the recipientBECode
					if (recipientLnkValue.startsWith("SELF")) { // The coreBE is the recipient
						ctxMap.put("RECIPIENT", coreBE.getCode());
						recipientBECode = coreBE.getCode();
					} else if (recipientLnkValue.startsWith("PER_")) {
						ctxMap.put("RECIPIENT", recipientLnkValue);
						recipientBECode = recipientLnkValue;
					} else {
						// check if it is a LNK path (of the coreBE)
						// "LNK_INTERN"
						if (recipientLnkValue.startsWith("LNK_")) {
							String[] splitStr = recipientLnkValue.split(":");
							Integer numPathItems = splitStr.length;
							BaseEntity lnkBe = coreBE; // seed
							for (int index = 0; index < numPathItems; index++) {
								lnkBe = beUtils.getBaseEntityFromLinkAttribute(lnkBe, splitStr[index]);
							}
							ctxMap.put("RECIPIENT", lnkBe.getCode());
							recipientBECode = lnkBe.getCode();
						}
					}
				} else {
					log.error("NO PRI_RECIPIENT_LNK present");
					continue;
				}

				// Determine the sender
				String senderLnkValue = message.getValueAsString("PRI_SENDER_LNK");
				if (senderLnkValue != null) {
					// check the various formats to get the senderBECode
					if (senderLnkValue.startsWith("USER")) { // The user is the sender
						ctxMap.put("SENDER", userToken.getUserCode());
					} else if (senderLnkValue.startsWith("PER_")) {
						ctxMap.put("SENDER", senderLnkValue);
					} else {
						// check if it is a LNK path (of the coreBE)
						// "LNK_INTERN"
						if (senderLnkValue.startsWith("LNK_")) {
							String[] splitStr = senderLnkValue.split(":");
							Integer numPathItems = splitStr.length;
							BaseEntity lnkBe = coreBE; // seed
							for (int index = 0; index < numPathItems; index++) {
								lnkBe = beUtils.getBaseEntityFromLinkAttribute(lnkBe, splitStr[index]);
							}
							ctxMap.put("SENDER", lnkBe.getCode());
						}
					}
				} else {
					log.error("NO PRI_SENDER_LNK present");
					continue;
				}
				// Now extract all the contexts from the core baseentity LNKs
				List<EntityAttribute> lnkEAs = coreBE.findPrefixEntityAttributes("LNK");
				String contextMapStr = "";
				for (EntityAttribute ea : lnkEAs) {
					String aliasCode = ea.getAttributeCode().substring("LNK_".length());
					String aliasValue = ea.getAsString();
					aliasValue = aliasValue.replace("\"", "").replace("[", "").replace("]", "");
					contextMapStr += aliasCode + "=" + aliasValue + ",";
					ctxMap.put(aliasCode, aliasValue);
				}

				log.info("Sending Message " + message.getCode() + " to " + recipientBECode + " with ctx="
						+ contextMapStr);
				sendMessage(message.getCode(), recipientBECode, ctxMap);

			}
		} else {
			log.warn("No messages found for milestoneCode " + milestoneCode);
		}
	}
}