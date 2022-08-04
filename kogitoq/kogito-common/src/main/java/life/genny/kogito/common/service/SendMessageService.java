package life.genny.kogito.common.service;

import java.util.Map;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

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
	DefUtils defUtils;


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
		BaseEntity recipientBe = beUtils.getBaseEntityByCode(recipientBECode);
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
	* Send all genny messages for a given milestone code.
	*
	* @param milestoneCode The workflow location to send messages for
	*
	* @param coreBE The core BaseEntity json for which all Contexts can be derived.
	 */
	public void sendAllMessages(String productCode, String milestoneCode, BaseEntity coreBE) {
		SearchEntity searchEntity = new SearchEntity("SBE_MILESTONE_MESSAGES", "Fetch All Messages associated with milestone Code")
			.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "MSG_%")
			.addFilter("PRI_MILESTONE", SearchEntity.StringFilter.LIKE, "%\""+milestoneCode.toUpperCase()+"\"%")
			.setPageStart(0)
			.setPageSize(100);

		

		searchEntity.setRealm(productCode);

		List<BaseEntity> messages = searchUtils.searchBaseEntitys(searchEntity);

		log.info("messages : " + messages.size());
		for (BaseEntity message : messages) {
			log.info("message : " + message.getCode());
			//sendMessage(message.getCode(), coreBE);
		}
	}


}
