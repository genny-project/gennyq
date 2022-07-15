package life.genny.qwandaq.utils;

import java.io.Serializable;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.exception.NotInitializedException;
import life.genny.qwandaq.exception.NullParameterException;
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

		JsonObject jsonObject = jsonb.fromJson(jsonb.toJson(payload), JsonObject.class);
		if (jsonObject.containsKey("tag") && StringUtils.isBlank(jsonObject.getString("tag"))) {
			jsonObject.remove("tag");
			JsonObjectBuilder builder = Json.createObjectBuilder();
			jsonObject.forEach(builder::add);
			jsonObject = builder.add("tag", getCallerMethodName()).build();
		}

		writeMsg(channel, jsonb.toJson(jsonObject));
	}

	/**
	 * Write a String to a kafka channel as a payload.
	 * @param channel the channel to send to
	 * @param payload the payload to send
	 */
	public static void writeMsg(String channel, String payload) {

		if (channel == null)
			throw new NullParameterException("channel");
		if (payload == null)
			throw new NullParameterException("payload");

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
