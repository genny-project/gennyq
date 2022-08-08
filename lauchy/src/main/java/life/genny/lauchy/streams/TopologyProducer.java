package life.genny.lauchy.streams;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.jboss.logging.Logger;

import life.genny.lauchy.Validator;
import life.genny.serviceq.intf.GennyScopeInit;

@ApplicationScoped
public class TopologyProducer {
	static Logger log = Logger.getLogger(TopologyProducer.class);

	@Inject
	Validator validator;

	@Inject
	GennyScopeInit scope;
	
	Jsonb jsonb = JsonbBuilder.create();

	@Produces
	public Topology buildTopology() {

		// Read the input Kafka topic into a KStream instance.
		StreamsBuilder builder = new StreamsBuilder();
		builder
				.stream("data", Consumed.with(Serdes.String(), Serdes.String()))
				.peek((k, v) -> scope.init(v))
				.peek((k, v) -> log.info("Received message: " + stripToken(v)))
				.filter((k, v) -> (v != null))
				.mapValues((k, v) -> validator.tidy(v))
				.filter((k, v) -> validator.validateData(v))
				.peek((k, v) -> log.info("Forwarding valid message"))
				.peek((k,v ) -> scope.destroy())
				.to("valid_data", Produced.with(Serdes.String(), Serdes.String()));

		return builder.build();
	}

	/**
	 * Helper function to show the data without a token
	 * @param data
	 * @return
	 */
	public String stripToken(String data) {
		JsonObject dataJson = jsonb.fromJson(data, JsonObject.class);
		return javax.json.Json.createObjectBuilder(dataJson).remove("token").build().toString();
	}

}
