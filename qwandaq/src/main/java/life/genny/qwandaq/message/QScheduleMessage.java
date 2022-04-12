package life.genny.qwandaq.message;

import java.time.LocalDateTime;
import java.time.ZoneId;
import javax.json.bind.annotation.JsonbTypeAdapter;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotEmpty;

import org.jboss.logging.Logger;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.datatype.PanacheLocalDateTimeAdapter;

import com.querydsl.core.annotations.QueryExclude;

@Entity
@Cacheable
@Table(name = "schedulemessage")
@RegisterForReflection
@QueryExclude
public class QScheduleMessage extends PanacheEntity {

	 private static final Logger log = Logger.getLogger(QScheduleMessage.class);	
	 private static final String DEFAULT_TAG = "default";

	@JsonbTypeAdapter(PanacheLocalDateTimeAdapter.class)
	public LocalDateTime created = LocalDateTime.now(ZoneId.of("UTC"));
	@JsonbTypeAdapter(PanacheLocalDateTimeAdapter.class)
	public LocalDateTime updated;

	public String cron;

	public LocalDateTime triggertime;
		
	@NotEmpty
	public String realm;

	@NotEmpty
	@Column(name = "jsonMessage", columnDefinition = "LONGTEXT")
	public String jsonMessage;
	
	@NotEmpty
	public String sourceCode;

	@NotEmpty
	public String channel;
	
	@Column(name = "token", columnDefinition = "MEDIUMTEXT")
	public String token;
	
	public String code;


	public QScheduleMessage() {}
	
	public QScheduleMessage(final String code,final String jsonMessage, final String sourceCode, final String channel, final String cron, final String realm) {

		this.code = code;
		this.cron = cron;
		this.jsonMessage = jsonMessage;
		this.channel = channel;
		this.sourceCode = sourceCode;
	}
	
	public QScheduleMessage(final String code,final String jsonMessage, final String sourceCode, final String channel, final LocalDateTime triggertime, final String realm) {

		this.code = code;
		this.triggertime = triggertime;
		this.jsonMessage = jsonMessage;
		this.channel = channel;
		this.sourceCode = sourceCode;
	}

	
	/** 
	 * @param code the code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}

	
	/** 
	 * @return String
	 */
	public String getCode() {
		return code;
	}

	
	/** 
	 * @param token the token to set
	 */
	public void setToken(String token) {
		this.token = token;
	}

	
	/** 
	 * @return String
	 */
	public String getToken() {
		return token;
	}
	
	
	/**
	 * @param id the id to delete by
	 * @return QScheduleMessage
	 */
	public static QScheduleMessage findById(Long id) {
		return find("id", id).firstResult();
	}

	
	/** 
	 * @param code the code to delete by
	 * @return QScheduleMessage
	 */
	public static QScheduleMessage findByCode(String code) {
		return find("code", code).firstResult();
	}

	
	/** 
	 * @param id the id to delete by
	 * @return long
	 */
	public static long deleteById(final Long id) {
		return delete("id", id);
	}

	
	/** 
	 * @param code the code to delete by
	 * @return long
	 */
	public static long deleteByCode(final String code) {
		return delete("code", code);
	}
}
