package life.genny.qwandaq.utils;

import java.io.Serializable;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.exception.runtime.NotInitializedException;
import life.genny.qwandaq.intf.KafkaInterface;

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

		writeMsg(channel, jsonb.toJson(payload));
	}

	/**
	 * Write a String to a kafka channel as a payload.
	 * @param channel the channel to send to
	 * @param payload the payload to send
	 */
	public static void writeMsg(String channel, String payload) {

		// write to kafka channel through interface
		checkInterface();
		kafkaInterface.write(channel, payload);
	}

	/**
	 * Check if the kafkaInterface is initialized.
	 */
	private static void checkInterface() {

		if (kafkaInterface == null) {
			throw new NotInitializedException("KafkaUtils not initialised!");
		}
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
