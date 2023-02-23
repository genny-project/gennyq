package life.genny.qwandaq.kafka;

/**
 * KafkaTopic
 */
public enum KafkaTopic {

	EVENTS,
	VALID_EVENTS,
	GENNY_EVENTS,
	SEARCH_EVENTS,
	DATA,
	VALID_DATA,
	GENNY_DATA,
	SEARCH_DATA,
	WEBCMDS,
	WEBDATA,
	MESSAGES,
	SCHEDULE,
	BLACKLIST,
	DEAD_LETTER_QUEUE;

	public String toValidTopicName() {
		return this.name().toLowerCase();
	}
}
