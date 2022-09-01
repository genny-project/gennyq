package life.genny.qwandaq.kafka;

import org.jboss.logging.Logger;

/**
 * A Kafka interface to write to kafka channels.
 *
 * This interface should be implemented seperately in any project that requires sending through kafka.
 * */
public interface KafkaInterface {

	public static final Logger log = Logger.getLogger(KafkaInterface.class);

	/**
	* A Dummy write method.
	*
	* @param topic the kafka topic to write to
	* @param payload the payload to write
	 */
	public default void write(KafkaTopic topic, String payload) {
		log.error("No KafkaInterface set up... not writing Message!!!");
	}
}
