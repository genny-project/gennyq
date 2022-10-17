package life.genny.qwandaq.message;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.KafkaUtils;

@RegisterForReflection
public class QCommunication extends QMessage {

	private static final Logger log = Logger.getLogger(QCommunication.class);
	private static final long serialVersionUID = 1L;

	private static final String MSG_TYPE = "COMMUNICATION";

	private String templateCode;
	private List<String> recipients;
	private List<QCommunicationType> communicationTypes;
	private Map<String, String> contexts;

	@Inject
	BaseEntityUtils beUtils;
	
	/* 
	 * Thought it unnecessary to rewrite all of these methods, 
	 * so the builder re-uses them instead.
	 */
	public QCommunication(Builder builder) {
		super(MSG_TYPE);
		this.templateCode = builder.templateCode;
		this.communicationTypes = builder.communicationTypes;
		this.recipients = builder.recipients;
		this.contexts = builder.contexts;
	}

	public static class Builder {

		private String templateCode;
		private List<String> recipients;
		private List<QCommunicationType> communicationTypes;
		private Map<String, String> contexts;

		public Builder(final String templateCode) {
			this.templateCode = templateCode;
		}

		public Builder addRecipient(BaseEntity recipient) {
			this.recipients.add(recipient.getCode());
			return this;
		}

		public Builder addRecipient(String recipient) {
			this.recipients.add(recipient);
			return this;
		}

		public Builder setMessageContextMap(Map<String, String> contexts) {
			this.contexts = contexts;
			return this;
		}

		public Builder addContext(String key, Object value) {
			this.contexts.put(key, value.toString());
			return this;
		}

		public Builder addCommunicationType(QCommunicationType communicationType) {
			this.communicationTypes.add(communicationType);
			return this;
		}

		public QCommunication send() {

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
			if (this.msg.communicationTypes.length == 0) {
				this.msg.addCommunicationType(QCommunicationType.DEFAULT);
			}

			KafkaUtils.writeMsg(KafkaTopic.MESSAGES, this.msg);
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
		return "QMessageGennyMSG [templateCode=" + templateCode + ", messageTypeArr=" + communicationTypes
				+ ", recipientArr=" + Arrays.toString(recipients) + ", messageContextMap=" + contexts + "]";
	}

}
