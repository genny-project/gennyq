package life.genny.qwandaq.message;

import java.time.LocalDateTime;
import java.time.ZoneId;
import javax.json.bind.annotation.JsonbTypeAdapter;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotEmpty;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.datatype.PanacheLocalDateTimeAdapter;

import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.models.GennyToken;

import com.querydsl.core.annotations.QueryExclude;

@Entity
@Cacheable
@Table(name = "schedulemessage")
@RegisterForReflection
@QueryExclude
public class QScheduleMessage extends PanacheEntity {

	private static final Logger log = Logger.getLogger(QScheduleMessage.class);	
	private static Jsonb jsonb = JsonbBuilder.create();

	@JsonbTypeAdapter(PanacheLocalDateTimeAdapter.class)
	public LocalDateTime created = LocalDateTime.now(ZoneId.of("UTC"));

	@JsonbTypeAdapter(PanacheLocalDateTimeAdapter.class)
	public LocalDateTime updated;

	@NotEmpty
	public String realm;

	@NotEmpty
	public String sourceCode;

	@NotEmpty
	public String channel;

	public String code;

	@Column(name = "token", columnDefinition = "MEDIUMTEXT")
	public String token;

	public String cron;

	public LocalDateTime triggerTime;

	@NotEmpty
	@Column(name = "jsonMessage", columnDefinition = "LONGTEXT")
	public String jsonMessage;

	public QScheduleMessage() {}

	public QScheduleMessage(String code) {
		this.code = code;
	}

	public QScheduleMessage(final String code, final String jsonMessage, final String sourceCode, final String channel, final String cron, final String realm) {

		this.code = code;
		this.cron = cron;
		this.jsonMessage = jsonMessage;
		this.channel = channel;
		this.sourceCode = sourceCode;
	}

	public QScheduleMessage(final String code,final String jsonMessage, final String sourceCode, final String channel, final LocalDateTime triggerTime, final String realm) {

		this.code = code;
		this.triggerTime = triggerTime;
		this.jsonMessage = jsonMessage;
		this.channel = channel;
		this.sourceCode = sourceCode;
	}

	/**
	* Set the realm
	*
	* @param realm The realm to set
	 */
	public void setRealm(String realm) {
		this.realm = realm;
	}

	/**
	* Get the realm
	*
	* @return The realm
	 */
	public String getRealm() {
		return realm;
	}

	/**
	* Set the source code
	*
	* @param sourceCode The source code to set
	 */
	public void setSourceCode(String sourceCode) {
		this.sourceCode = sourceCode;
	}

	/**
	* Get the source code of the schedule message
	*
	* @return The source code
	 */
	public String getSourceCode() {
		return sourceCode;
	}

	/**
	* Set the targeted channel
	*
	* @param channel The channel to set
	 */
	public void setChannel(String channel) {
		this.channel = channel;
	}

	/**
	* Get the targeted channel
	*
	* @return The channel 
	 */
	public String getChannel() {
		return channel;
	}

	/** 
	 * Set the code
	 *
	 * @param code the code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/** 
	 * Get the code
	 *
	 * @return The code
	 */
	public String getCode() {
		return code;
	}

	/** 
	 * Set the token
	 *
	 * @param token the token to set
	 */
	public void setToken(String token) {
		this.token = token;
	}

	/** 
	 * Get the token
	 *
	 * @return String
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Set the cron string
	 *
	 * @param cron the cron to set
	 */
	public void setCron(String cron) {
		this.cron = cron;
	}

	/**
	 * Get the cron string
	 *
	 * @return the cron string
	 */
	public String getCron() {
		return cron;
	}

	/**
	 * Set the triggerTime datetime
	 *
	 * @param triggerTime the datetime to set
	 */
	public void setTriggerTime(LocalDateTime triggerTime) {
		this.triggerTime = triggerTime;
	}

	/**
	 * Get the triggerTime datetime
	 *
	 * @return the triggerTime datetime
	 */
	public LocalDateTime getTriggerTime() {
		return triggerTime;
	}

	/**
	 * Set the Json Message to send at scheduled time/cron
	 *
	 * @param jsonMessage The json msg to send
	 */
	public void setJsonMessage(String jsonMessage) {
		this.jsonMessage = jsonMessage;
	}

	/**
	 * Get the Json Message
	 *
	 * @return The json message
	 */
	public String getJsonMessage() {
		return jsonMessage;
	}

	/**
	 * The Schedule message bulder pattern constructor
	 *
	 * @param builder The builder pattern
	 */
	public QScheduleMessage(Builder builder) {

		this.realm = builder.msg.realm;
		this.sourceCode = builder.msg.sourceCode;
		this.channel = builder.msg.channel;
		this.code = builder.msg.code;
		this.token = builder.msg.token;
		this.cron = builder.msg.cron;
		this.triggerTime = builder.msg.triggerTime;
		this.jsonMessage = builder.msg.jsonMessage;
	}

	public static class Builder {

		public QScheduleMessage msg;
		public GennyToken gennyToken;

		public QEventMessage eventMessage;
		public QMessageGennyMSG commsMessage;

		public Builder(String scheduleCode) {

			this.msg = new QScheduleMessage(scheduleCode);
		}

		/**
		 * Set the source code
		 *
		 * @param sourceCode The source code to set
		 * @return The builder
		 */
		public Builder setSourceCode(String sourceCode) {

			this.msg.setCron(sourceCode);
			return this;
		}

		/**
		 * Set the scheduled channel
		 *
		 * @param channel The channel to set
		 * @return The builder
		 */
		public Builder setChannel(String channel) {

			this.msg.setCron(channel);
			return this;
		}

		/**
		 * Set the GennyToken to use in scheduling
		 *
		 * @param gennyToken The GennyToken to set
		 * @return The builder
		 */
		public Builder setGennyToken(GennyToken gennyToken) {

			this.gennyToken = gennyToken;
			return this;
		}

		/**
		 * Set the Cron Job trigger string
		 *
		 * @param cron The cron string to set
		 * @return The builder
		 */
		public Builder setCron(String cron) {

			this.msg.setCron(cron);
			return this;
		}

		/**
		 * Set the scheduled triggerTime datetime
		 *
		 * @param triggerTime The datetime to triggerTime at
		 * @return The builder
		 */
		public Builder setTriggerTime(LocalDateTime triggerTime) {

			this.msg.setTriggerTime(triggerTime);
			return this;
		}

		/**
		 * Set the Event Message to send at trigger time
		 *
		 * @param code The code of the event message
		 * @param targetCode The target code of the event message
		 * @return The builder
		 */
		public Builder setEventMessage(String code, String targetCode) {

			QEventMessage eventMessage = new QEventMessage("SCHEDULE_EVT", code);

			eventMessage.setToken(this.msg.getToken());
			eventMessage.getData().setTargetCode(targetCode);

			setEventMessage(eventMessage);
			return this;
		}

		/**
		 * Set the Event Message to send at trigger time
		 *
		 * @param eventMessage The event message to send
		 * @return The builder
		 */
		public Builder setEventMessage(QEventMessage eventMessage) {

			this.eventMessage = eventMessage;
			return this;
		}

		/**
		 * Set the Communications GennyMSG to send at trigger time
		 *
		 * @param commsMessage The communications message
		 * @return The builder
		 */
		public Builder setCommsMessage(QMessageGennyMSG commsMessage) {

			this.commsMessage = commsMessage;
			return this;
		}

		/**
		 * Finish building the schedule message and send to shleemy.
		 *
		 * @return The complete schedule message.
		 */
		public QScheduleMessage schedule() {

			// null check the gennyToken
			if (this.gennyToken == null) {
				log.error("A valid GennyToken must be set before scheduling can occur!");
				return null;
			}

			this.msg.setToken(gennyToken.getToken());

			// handle event message
			if (this.eventMessage != null) {

				String[] rxList = new String[]{ "SUPERUSER", gennyToken.getUserCode() };
				this.eventMessage.setRecipientCodeArray(rxList);

				this.msg.setJsonMessage(jsonb.toJson(this.eventMessage));

				// set channel to send to events of not otherwise set
				if (this.msg.getChannel() == null) {
					this.msg.setChannel("events");
				}
			}

			// handle comms message
			if (this.commsMessage != null) {
				this.msg.setJsonMessage(jsonb.toJson(this.commsMessage));

				// set channel to send to messages of not otherwise set
				if (this.msg.getChannel() == null) {
					this.msg.setChannel("messages");
				}
			}

			// null check the json body
			if (this.msg.getJsonMessage() == null ) {
				log.error("No json has been set for shcedule msg. Please check that a valid event or comm has been provided!");
				return null;
			}

			// send to shleemy
			KafkaUtils.writeMsg("schedule", this.msg);
			return this.msg;
		}

	}

	/**
	 * Find a message using the id.
	 *
	 * @param id The id to delete by
	 * @return QScheduleMessage
	 */
	public static QScheduleMessage findById(Long id) {
		return find("id", id).firstResult();
	}

	/** 
	 * Find a message using the code.
	 *
	 * @param code The code to delete by
	 * @return QScheduleMessage
	 */
	public static QScheduleMessage findByCode(String code) {
		return find("code", code).firstResult();
	}

	/** 
	 * Delete a message using the id.
	 *
	 * @param id The id to delete by
	 * @return long
	 */
	public static long deleteById(final Long id) {
		return delete("id", id);
	}

	/** 
	 * Delete a message using the code.
	 *
	 * @param code The code to delete by
	 * @return long
	 */
	public static long deleteByCode(final String code) {
		return delete("code", code);
	}
}
