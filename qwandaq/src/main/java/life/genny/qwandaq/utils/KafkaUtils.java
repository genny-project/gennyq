package life.genny.qwandaq.utils;

import java.io.Serializable;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import life.genny.qwandaq.intf.KafkaInterface;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
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

		String json = null;

		if (payload instanceof QDataBaseEntityMessage) {
			QDataBaseEntityMessage msg = (QDataBaseEntityMessage) payload;
			msg.setTag(getCallerMethodName());
			// jsonify the payload and write
			json = jsonb.toJson(msg);
		} else {
			// jsonify the payload and write
			json = jsonb.toJson(payload);
		}

		writeMsg(channel, json);
	}

	/**
	 * Write a String to a kafka channel as a payload
	 *
	 * @param channel the channel to send to
	 * @param payload the payload to send
	 */
	public static void writeMsg(String channel, String payload) {
		if (payload == null) {
			log.error("Payload is null");
			return;
		}

		log.info("WritingMsg1: " + channel + " " + payload.substring(0, 100));
		if (!checkInterface()) {
			return;
		}
		log.info("WritingMsg2: " + channel + " " + payload.substring(0, 100));

		if (channel.isBlank()) {
			log.error("Channel is blank, cannot send payload!");
			return;
		}
		log.info("WritingMsg3: " + channel + " " + payload.substring(0, 100));

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

	public static String getCurrentMethodName() {
		return StackWalker.getInstance()
				.walk(s -> s.skip(1).findFirst())
				.get()
				.getMethodName();
	}

	public static String getCallerMethodName() {
		return StackWalker.getInstance()
				.walk(s -> s.skip(2).findFirst())
				.get()
				.getMethodName();
	}
}
