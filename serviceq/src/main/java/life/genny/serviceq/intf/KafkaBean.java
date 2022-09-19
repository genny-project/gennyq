package life.genny.serviceq.intf;

import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import io.smallrye.reactive.messaging.kafka.OutgoingKafkaRecordMetadata;
import org.eclipse.microprofile.reactive.messaging.Message;

import life.genny.qwandaq.session.bridge.BridgeSwitch;
import life.genny.qwandaq.kafka.KafkaInterface;
import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.exception.runtime.DebugException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.serviceq.live.data.InternalProducer;

@ApplicationScoped
public class KafkaBean implements KafkaInterface {

	@Inject 
	InternalProducer producer;

	@Inject 
	UserToken userToken;

	static final Logger log = Logger.getLogger(KafkaBean.class);

	static Jsonb jsonb = JsonbBuilder.create();

	/**
	* Write a string payload to a kafka channel.
	*
	* @param topic
	* @param payload
	 */
	public void write(KafkaTopic topic, String payload) { 

		if (topic == null)
			throw new NullParameterException("channel");
		if (payload == null)
			throw new NullParameterException("payload");

		// find GennyToken from payload contents
		JsonObject json = jsonb.fromJson(payload, JsonObject.class);
		if (json == null)
			throw new DebugException("Outgoing message could not be deserialized to json");
		if (!json.containsKey("token"))
			throw new DebugException("Outgoing message must have a token");

		// create metadata for correct bridge if outgoing
		OutgoingKafkaRecordMetadata<String> metadata = OutgoingKafkaRecordMetadata.<String>builder()
					.build();

		if (topic == KafkaTopic.WEBCMDS || topic == KafkaTopic.WEBDATA) {

			String bridgeId = BridgeSwitch.get(userToken);

			if (bridgeId == null) {
				log.warn("No Bridge ID found for " + userToken.getUserCode() + " : " + userToken.getJTI());
				bridgeId = BridgeSwitch.findActiveBridgeId(userToken);
			}

			if (bridgeId != null) {
				log.debug("Sending to " + bridgeId);

				metadata = OutgoingKafkaRecordMetadata.<String>builder()
					.withTopic(bridgeId + "-" + topic.toValidTopicName())
					.build();
			} else {
				log.error("No alternative Bridge ID found!");
			}
		}

		// channel switch
		switch (topic) {
			case EVENTS -> producer.getToEvents().send(payload);
			case VALID_EVENTS -> producer.getToValidEvents().send(payload);
			case GENNY_EVENTS -> producer.getToGennyEvents().send(payload);
			case GENNY_DATA -> producer.getToGennyData().send(payload);
			case SEARCH_EVENTS -> producer.getToSearchEvents().send(payload);
			case DATA -> producer.getToData().send(payload);
			case VALID_DATA -> producer.getToValidData().send(payload);
			case SEARCH_DATA -> producer.getToSearchData().send(payload);
			case MESSAGES -> producer.getToMessages().send(payload);
			case SCHEDULE -> producer.getToSchedule().send(payload);
			case BLACKLIST -> producer.getToBlacklist().send(payload);
			case WEBCMDS -> producer.getToWebCmds().send(Message.of(payload).addMetadata(metadata));
			case WEBDATA -> producer.getToWebData().send(Message.of(payload).addMetadata(metadata));
			default -> log.error("Producer unable to write to channel " + topic);
		}
	}
}
