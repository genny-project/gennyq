package life.genny.qwandaq.utils;

import java.io.Serializable;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import life.genny.qwandaq.intf.KafkaInterface;
import org.jboss.logging.Logger;




/*
 * A static utility class used for standard 
 * message routing throgh Kafka.
 * 
 * @author Jasper Robison
 */
public class KafkaUtils implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(KafkaUtils.class);
	private static Jsonb jsonb = JsonbBuilder.create();
	private static KafkaInterface kafkaInterface;

	/**
	* Initialise the kafka interface
	*
	* @param kInterface the kInterface to set
	 */
	public static void init(KafkaInterface kInterface) {
		kafkaInterface = kInterface;
	}

	/**
	* Write an Object to a kafka channel as a payload
	*
	* @param channel the channel to send to
	* @param payload the payload to send
	 */
	public static void writeMsg(String channel, Object payload) {

		// jsonify the payload and write
		String json = jsonb.toJson(payload);
		writeMsg(channel, json);
	}

	/**
	* Write a String to a kafka channel as a payload
	*
	* @param channel the channel to send to
	* @param payload the payload to send
	 */
	public static void writeMsg(String channel, String payload) {
		log.info("WritingMsg1: " + channel + " " + payload.substring(0,100));
		if (!checkInterface()) {
			return;
		}
	log.info("WritingMsg2: " + channel + " " + payload.substring(0,10));
	
		if (channel.isBlank()) {
			log.error("Channel is blank, cannot send payload!");
			return;
		}
	log.info("WritingMsg3: " + channel + " " + payload.substring(0,10));
	
		// write to kafka channel through interface
		kafkaInterface.write(channel, payload);
	}

	private static Boolean checkInterface() {

		if (kafkaInterface == null) {
			log.error("KafkaUtils not initialised! Initialise with: KafkaUtils.init(KafkaInterface)");
			return false;
		}

		return true;
	}

}
