package life.genny.qwandaq.loadtesting;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.message.QSearchMessage;

public class TestJob {

	final String uuid;
	final String code;
	final Instant start;

	final String qSearchMessageJson;
	final String searchEntityJson;
	Instant end = null;

	/**
	 * Creates a new TestJob and adds it to a {@link LoadTestJobs} ConcurrentHashMap
	 * 
	 * @param jobLoader {@link LoadTestJobs} to use
	 * @param searchMessage {@link QSearchMessage} to user
	 */
	public TestJob(LoadTestJobs jobLoader, QSearchMessage searchMessage) {
		this.start = Instant.now();
		this.uuid = UUID.randomUUID().toString().toUpperCase();

		SearchEntity entity = searchMessage.getSearchEntity();

		entity.setCode(entity.getCode() + "_" + this.getUuid());
		this.code = entity.getCode();
		
		this.searchEntityJson = jobLoader.jsonb.toJson(entity);
		this.qSearchMessageJson = jobLoader.jsonb.toJson(searchMessage);
		jobLoader.putJob(entity, this);
	}
	
	/** 
	 * @return String
	 */
	public String getUuid() {
		return uuid;
	}
	
	/** 
	 * @return String
	 */
	public String getCode() {
		return code;
	}
	
	/** 
	 * @return Instant
	 */
	public Instant getStart() {
		return start;
	}

	/** 
	 * @return Instant
	 */
	public Instant getEnd() {
		return end;
	}
	
	/** 
	 * @param end the end to set
	 */
	public void setEnd(Instant end) {
		this.end = end;
	}
	
	/** 
	 * @return Boolean
	 */
	public Boolean isComplete() {
		return end != null;
	}
	
	/** 
	 * @return Long
	 */
	public Long getDuration() {
		return Duration.between(start, end).toMillis();
	}

	/** 
	 * @return String
	 */
	@Override
	public String toString() {
		return toString(false);
	}
	
	/** 
	 * @param qSearchMessageJson the json to stringify
	 * @return String
	 */
	public String toString(boolean qSearchMessageJson) {
		return "TestJob ["
				+ "code=" + code
				+ ", start=" + start
				+ (end != null ? ", end=" + end : "")
				+ (end != null ? ", duration=" + getDuration() : "")
				+ (qSearchMessageJson ? "====QSearchMessage====\n" + getQSearchMessageJSON() : "")
				+ "]";
	}

	/** 
	 * @return String
	 */
	public String getSearchJSON() {
		return searchEntityJson;
	}
	
	/** 
	 * @return String
	 */
	public String getQSearchMessageJSON() {
		return qSearchMessageJson;
	}

}
