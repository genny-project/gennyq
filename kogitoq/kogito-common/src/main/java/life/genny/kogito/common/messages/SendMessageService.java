package life.genny.kogito.common.messages;

import static life.genny.qwandaq.attribute.Attribute.LNK_MESSAGE_TYPE;
import life.genny.qwandaq.entity.BaseEntity;
import static life.genny.qwandaq.entity.BaseEntity.PRI_NAME;
import life.genny.qwandaq.message.QBaseMSGMessageType;
import life.genny.qwandaq.models.UserToken;

import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CommonUtils;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.util.Map;
import java.util.HashMap;

@ApplicationScoped
public class SendMessageService {

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	BaseEntityUtils baseEntityUtils;

	@Inject
	UserToken userToken;

	@Inject
	Logger log;

	public static final String RECIPIENT = "RECIPIENT";
	public static final String SOURCE = "SOURCE";
	public static final String TARGET = "TARGET";

	/**
	 * Send a genny message.
	 *
	 * @param messageCode    BaseEntity code for message
	 * @param recipientCode  BaseEntity code for Person
	 */
	public void sendMessage(String messageCode, String recipientCode) {
		new SendMessage(messageCode, recipientCode).sendMessage();
	}

	/**
	 * Send a genny message.
	 *
	 * @param messageCode    BaseEntity code for message
	 * @param recipientCode  BaseEntity code for Person
	 * @param ctxMap          The map of contexts to use
	 */
	public void sendMessage(String messageCode, String recipientCode, Map<String, String> ctxMap) {
		new SendMessage(messageCode, recipientCode, ctxMap).sendMessage();
	}

	/**
	 * Send a genny message.
	 *
	 * @param messageCode    BaseEntity code for message
	 * @param recipientCode  BaseEntity code for Person
	 * @param ctxMap          The map of contexts to use
	 */
	public void sendMessage(String messageCode, BaseEntity recipientBE, Map<String, String> ctxMap) {
		new SendMessage(messageCode, recipientBE, ctxMap).sendMessage();
	}

	public void send(String messageCode, String recipientCode){
		BaseEntity baseEntity = baseEntityUtils.getBaseEntity(messageCode);
		if(baseEntity != null){
			BaseEntity lnkMessageType = baseEntityUtils.getBaseEntityFromLinkAttribute(baseEntity, LNK_MESSAGE_TYPE, true);
			log.debug("lnkMessageType: "+lnkMessageType.getCode());
			log.debug("lnkMessageType.name: "+lnkMessageType.getName());
			String msgType = lnkMessageType.getName().toUpperCase();
			log.info("Triggering message!");
			sendMessage(messageCode,recipientCode, msgType);
		}else{
			log.error("Message messageCode not found!");
		}
	}

	public void send(String messageCode, String recepientCode, Map<String, String> ctxMap){
		BaseEntity baseEntity = baseEntityUtils.getBaseEntity(messageCode);
		if(baseEntity != null){
			BaseEntity lnkMessageType = baseEntityUtils.getBaseEntityFromLinkAttribute(baseEntity, LNK_MESSAGE_TYPE, true);
			log.debug("lnkMessageType: "+lnkMessageType.getCode());
			log.debug("lnkMessageType.name: "+lnkMessageType.getName());
			String msgType = lnkMessageType.getName().toUpperCase();
			log.info("Triggering message!");
			sendMessage(messageCode,recepientCode, msgType, ctxMap);
		}else{
			log.error("Message messageCode not found!");
		}
	}

	/**
	 * Send a toast message and cannot use message type as a parameter via kogito
	 *
	 * @param messageCode    BaseEntity code for message
	 * @param recipientCode  BaseEntity code for Person
	 * @param msgType The message type might be TOAST,SLACK,SMS,EMAIL,SENDGRID,VOICE
	 * @param ctxMap The context map
	 */
	public void sendMessage(String messageCode,String recipientCode,String msgType) {
		Map<String, String> ctxMap = new HashMap<>();
		sendMessage(messageCode, recipientCode, msgType, ctxMap);
	}

	public void sendMessage(String messageCode,String recipientCode,String msgType, Map<String, String> ctxMap) {
		if(StringUtils.isBlank(msgType)){
			log.error("templateCode: "+ messageCode);
			log.error("recipientCode: "+ recipientCode);
			log.error("msgType: "+ msgType);
			CommonUtils.printMap(ctxMap);
			return;
		}
		if(msgType.contains(QBaseMSGMessageType.TOAST.name())) {
			ctxMap.put(TARGET,recipientCode);
			new SendMessage(messageCode, recipientCode, QBaseMSGMessageType.TOAST, ctxMap).sendMessage();
		}
		if(msgType.contains(QBaseMSGMessageType.SLACK.name())) {
			ctxMap.put(RECIPIENT,messageCode);
			new SendMessage(messageCode, recipientCode, QBaseMSGMessageType.SLACK, ctxMap).sendMessage();
		}
		if(msgType.contains(QBaseMSGMessageType.SMS.name())) {
			new SendMessage(messageCode, recipientCode, QBaseMSGMessageType.SMS, ctxMap).sendMessage();
		}
		if(msgType.contains(QBaseMSGMessageType.EMAIL.name())) {
			new SendMessage(messageCode, recipientCode, QBaseMSGMessageType.EMAIL, ctxMap).sendMessage();
		}
		if(msgType.contains(QBaseMSGMessageType.SENDGRID.name())) {
			new SendMessage(messageCode, recipientCode, QBaseMSGMessageType.SENDGRID, ctxMap).sendMessage();
		}
		if(msgType.contains(QBaseMSGMessageType.VOICE.name())) {
			new SendMessage(messageCode, recipientCode, QBaseMSGMessageType.VOICE, ctxMap).sendMessage();
		}
	}
}