package life.genny.qwandaq.utils;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.exception.runtime.NotInitializedException;
import life.genny.qwandaq.kafka.KafkaInterface;
import life.genny.qwandaq.kafka.KafkaTopic;

/*
 * A static utility class used for standard 
 * message routing throgh Kafka.
 * 
 * @author Jasper Robison
 */
public class KafkaUtils implements Serializable {

	private static final long serialVersionUID = 1L;
	private static Jsonb jsonb = JsonbBuilder.create();
	private static KafkaInterface kafkaInterface;

	@Inject
	private static Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * Initialise the kafka interface
	 *
	 * @param kInterface the kInterface to set
	 */
	public static void init(KafkaInterface kInterface) {
		kafkaInterface = kInterface;
	}

	/**
	 * Write an Object to a kafka topic as a payload
	 *
	 * @param topic the topic to send to
	 * @param payload the payload to send
	 */
	public static void writeMsg(KafkaTopic topic, Object payload) {

		writeMsg(topic, jsonb.toJson(payload));
	}

	/**
	 * Write a String to a kafka topic as a payload.
	 * @param topic the topic to send to
	 * @param payload the payload to send
	 */
	public static void writeMsg(KafkaTopic topic, String payload) {

		// write to kafka channel through interface
		checkInterface();
		kafkaInterface.write(topic, payload);
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
