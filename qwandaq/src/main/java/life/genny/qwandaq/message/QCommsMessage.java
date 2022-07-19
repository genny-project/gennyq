package life.genny.qwandaq.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.NullParameterException;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.utils.KafkaUtils;

@RegisterForReflection
public class QCommsMessage extends QMessage {

	private static final Logger log = Logger.getLogger(QCommsMessage.class);
	private static final String MESSAGE_TYPE = "COM_MSG";
	private String templateCode;
	private List<CommunicationType> messageTypes = new ArrayList<>();
	private List<String> recipients = new ArrayList<>();
	private Map<String, String> contextMap = new HashMap<>();
	
	public QCommsMessage() {
		super(MESSAGE_TYPE);
	}

	public static Logger getLog() {
        return log;
    }

    public static String getMessageType() {
        return MESSAGE_TYPE;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public List<CommunicationType> getMessageTypes() {
        return messageTypes;
    }

    public void setMessageTypes(List<CommunicationType> messageTypes) {
        this.messageTypes = messageTypes;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }

    public Map<String, String> getContextMap() {
        return contextMap;
    }

    public void setContextMap(Map<String, String> contextMap) {
        this.contextMap = contextMap;
    }

	public void addMessageType(CommunicationType type) {
		this.messageTypes.add(type);
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
		if (recipient == null)
			throw new NullParameterException("recipient");

		recipients.add(recipient);
	}
	
	/** 
	 * @param key the key of the context to add
	 * @param value the value of the context to add
	 */
	public void addContext(String key, Object value) {
		if (value == null)
			throw new NullParameterException("recipient");

		if (value.getClass().equals(BaseEntity.class)) {
			this.contextMap.put(key, ((BaseEntity) value).getCode());
		} else {
			this.contextMap.put(key, value.toString());
		}
	}

	/* 
	 * Thought it unnecessary to rewrite all of these methods, 
	 * so the builder re-uses them instead.
	 */
	public QCommsMessage(Builder builder) {
		super(MESSAGE_TYPE);
		this.templateCode = builder.msg.templateCode;
		this.messageTypes = builder.msg.messageTypes;
		this.recipients = builder.msg.recipients;
		this.contextMap = builder.msg.contextMap;
	}

	public static class Builder {

		public QCommsMessage msg;

		public Builder(final String templateCode) {
			this.msg = new QCommsMessage();
			this.msg.setTemplateCode(templateCode);
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
			this.msg.setContextMap(ctxMap);
			return this;
		}

		public Builder addContext(String key, Object value) {
			this.msg.addContext(key, value);
			return this;
		}

		public Builder addMessageType(CommunicationType messageType) {
			this.msg.addMessageType(messageType);
			return this;
		}

		public Builder setToken(GennyToken token) {
			this.msg.setToken(token.getToken());
			return this;
		}
		
		@Deprecated
		public Builder setToken(String token) {
			this.msg.setToken(token);
			return this;
		}

		public QCommsMessage send() {

			if (this.msg.getToken() == null) {
				log.error("No token set for message. Cannot send!!!");
				return this.msg;
			}

			// Check if template code is present
			if (this.msg.getTemplateCode() == null) {
				log.warn("Message does not contain a Template Code!!");
			}

			// Set Msg Type to DEFAULT if none set already
			if (this.msg.getMessageTypes().size() == 0) {
				this.msg.addMessageType(CommunicationType.DEFAULT);
			}

			KafkaUtils.writeMsg("messages", this.msg);
			return this.msg;
		}
	}

}
