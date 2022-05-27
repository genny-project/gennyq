package life.genny.qwandaq.message;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.util.concurrent.CopyOnWriteArrayList;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.KafkaUtils;

@RegisterForReflection
public class QMessageGennyMSG extends QMessage {

	private static final Logger log = Logger.getLogger(QMessageGennyMSG.class);
	private static final long serialVersionUID = 1L;
	private static final String MESSAGE_TYPE = "MSG_MESSAGE";
	private String templateCode;
	private QBaseMSGMessageType[] messageTypeArr;
	private String[] recipientArr;
	private Map<String, String> messageContextMap;
	
	/**
	 * @return the templateCode
	 */
	public String getTemplateCode() {
		return templateCode;
	}

	/**
	 * @param templateCode the templateCode to set
	 */
	public void setTemplateCode(String templateCode) {
		this.templateCode = templateCode;
	}

	/**
	 * @return QBaseMSGMessageType the messageTypeArr
	 */
	public QBaseMSGMessageType[] getMessageTypeArr() {
		return messageTypeArr;
	}

	/**
	 * @param messageTypeArr the messageTypeArr to set
	 */
	public void setMessageTypeArr(QBaseMSGMessageType[] messageTypeArr) {
		this.messageTypeArr = messageTypeArr;
	}

	/**
	 * @return String the recipientArr
	 */
	public String[] getRecipientArr() {
		return recipientArr;
	}

	/**
	 * @param recipientArr the recipientArr to set
	 */
	public void setRecipientArr(String[] recipientArr) {
		this.recipientArr = recipientArr;
	}

	/**
	 * @return Map the messageContextMap
	 */
	public Map<String, String> getMessageContextMap() {
		return messageContextMap;
	}

	/**
	 * @param messageContextMap the messageContextMap to set
	 */
	public void setMessageContextMap(Map<String, String> messageContextMap) {
		this.messageContextMap = messageContextMap;
	}
	
	public QMessageGennyMSG() {
		super("COM_MSG");
		this.messageTypeArr = new QBaseMSGMessageType[0];
		this.messageContextMap = new HashMap<String, String>();
		this.recipientArr = new String[0];
	}

	public QMessageGennyMSG(String templateCode) {
		super("COM_MSG");
		this.templateCode = templateCode;
		this.messageTypeArr = new QBaseMSGMessageType[0];
		this.messageContextMap = new HashMap<String, String>();
		this.recipientArr = new String[0];
	}

	public QMessageGennyMSG(QBaseMSGMessageType messageType) {
		super("COM_MSG");
		this.messageTypeArr = new QBaseMSGMessageType[]{ messageType };
		this.messageContextMap = new HashMap<String, String>();
		this.recipientArr = new String[0];
	}

	public QMessageGennyMSG(String msg_type, QBaseMSGMessageType[] messageType, String templateCode, Map<String, String> contextMap, String[] recipientArr) {
		super(msg_type);
		this.templateCode = templateCode;
		this.messageTypeArr = messageType;
		this.messageContextMap = contextMap;
		this.recipientArr = recipientArr;
	}
	
	/** 
	 * @param messageType the type of message to set
	 */
	public void addMessageType(QBaseMSGMessageType messageType) {
		
		List<QBaseMSGMessageType> list = this.getMessageTypeArr() != null ? new CopyOnWriteArrayList<>(Arrays.asList(this.getMessageTypeArr())) : new CopyOnWriteArrayList<>();
		list.add(messageType);
		this.setMessageTypeArr(list.toArray(new QBaseMSGMessageType[0]));
	}

	/** 
	 * @param recipient the entity of the recipient to set
	 */
	public void setRecipient(String recipient) {
		this.recipientArr = new String[0];
		addRecipient(recipient);
	}

	/** 
	 * @param recipient the code or email of the recipient to set
	 */
	public void setRecipient(BaseEntity recipient) {
		this.recipientArr = new String[0];
		addRecipient(recipient);
	}

	/** 
	 * @param recipient the entity of the recipient to add
	 */
	public void addRecipient(BaseEntity recipient) {
		if (recipient == null) {
			log.warn("RECIPIENT BE passed is NULL");
		} else {
			addRecipient("[\""+recipient.getCode()+"\"]");
		}
	}

	/** 
	 * @param recipient the code or email of the recipient to add
	 */
	public void addRecipient(String recipient) {
		
		if (recipient == null) {
			log.warn("RECIPIENT passed is NULL");
		} else {
			List<String> list = this.getRecipientArr() != null ? new CopyOnWriteArrayList<>(Arrays.asList(this.getRecipientArr())) : new CopyOnWriteArrayList<>();
			list.add(recipient);
			this.setRecipientArr(list.toArray(new String[0]));
		}
	}
	
	/** 
	 * @param key the key of the context to add
	 * @param value the value of the context to add
	 */
	public void addContext(String key, Object value) {
		if (value == null) {
			log.warn(key + " passed is NULL");
		} else {
			if (value.getClass().equals(BaseEntity.class)) {
				this.messageContextMap.put(key, ((BaseEntity) value).getCode());
			} else {
				this.messageContextMap.put(key, value.toString());
			}
		}
	}

	/* 
	 * Thought it unnecessary to rewrite all of these methods, 
	 * so the builder re-uses them instead.
	 */
	public QMessageGennyMSG(Builder builder) {
		super("COM_MSG");
		this.templateCode = builder.msg.templateCode;
		this.messageTypeArr = builder.msg.messageTypeArr;
		this.recipientArr = builder.msg.recipientArr;
		this.messageContextMap = builder.msg.messageContextMap;
	}

	public static class Builder {

		public QMessageGennyMSG msg;
		public BaseEntityUtils beUtils;

		public Builder(final String templateCode) {
			this.msg = new QMessageGennyMSG(templateCode);
		}

		public Builder(final String templateCode, BaseEntityUtils beUtils) {
			this.msg = new QMessageGennyMSG(templateCode);
			this.beUtils = beUtils;
		}

		public Builder addRecipient(BaseEntity recipient) {
			this.msg.addRecipient(recipient);
			return this;
		}

		public Builder addRecipient(String recipient) {
			this.msg.addRecipient(recipient);
			return this;
		}

		public Builder setMessageContextMap(Map<String, String> ctxMap) {
			this.msg.setMessageContextMap(ctxMap);
			return this;
		}

		public Builder addContext(String key, Object value) {
			this.msg.addContext(key, value);
			return this;
		}

		public Builder addMessageType(QBaseMSGMessageType messageType) {
			this.msg.addMessageType(messageType);
			return this;
		}

		public Builder setToken(String token) {
			this.msg.setToken(token);
			return this;
		}

		public Builder setUtils(BaseEntityUtils beUtils) {
			this.beUtils = beUtils;
			return this;
		}

		public QMessageGennyMSG send() {

			if (this.msg.getToken() == null) {
				log.error("No token set for message. Cannot send!!!");
				return this.msg;
			}

			if (this.beUtils == null) {
				log.error("No beUtils set for message. Cannot send!!!");
				return this.msg;
			}
			// Check if template code is present
			if (this.msg.getTemplateCode() == null) {
				log.warn("Message does not contain a Template Code!!");
			} else {
				// Make sure template exists
				BaseEntity templateBE = beUtils.getBaseEntityByCode(this.msg.getTemplateCode());

				if (templateBE == null) {
					log.error("Message template " + this.msg.getTemplateCode() + " does not exist!!");
					return this.msg;
				}

				// // Find any required contexts for template
				// String contextListString = templateBE.getValue("PRI_CONTEXT_LIST", "[]");
				// String[] contextArray = contextListString.replaceAll("[", "").replaceAll("]", "").replaceAll("\"", "").split(",");

				// if (!contextListString.equals("[]") && contextArray != null && contextArray.length > 0) {
				// 	// Check that all required contexts are present
				// 	boolean containsAllContexts = Arrays.stream(contextArray).allMatch(item -> this.msg.getMessageContextMap().containsKey(item));

				// 	if (!containsAllContexts) {
				// 		log.error(ANSIColour.RED+"Msg does not contain all required contexts : " + contextArray.toString() + ANSIColour.RESET);
				// 		return this.msg;
				// 	}
				// }
			}

			// Set Msg Type to DEFAULT if none set already
			if (this.msg.messageTypeArr.length == 0) {
				this.msg.addMessageType(QBaseMSGMessageType.DEFAULT);
			}

			KafkaUtils.writeMsg("messages", this.msg);
			return this.msg;
		}

	}

	
	/** 
	 * @return String
	 */
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "QMessageGennyMSG [templateCode=" + templateCode + ", messageTypeArr=" + messageTypeArr
				+ ", recipientArr=" + Arrays.toString(recipientArr) + ", messageContextMap=" + messageContextMap + "]";
	}

}
