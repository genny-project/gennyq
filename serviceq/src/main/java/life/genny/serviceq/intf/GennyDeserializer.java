package life.genny.serviceq.intf;

import io.quarkus.arc.Arc;

import java.util.Map;

import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.kafka.common.serialization.Deserializer;

import org.jboss.logging.Logger;

import life.genny.qwandaq.models.UserToken;

/**
 * Custom Deserializer class for initializing the UserToken through Genny Kafka consumers.
 **/
public class GennyDeserializer implements Deserializer<String> {

	static final Logger log = Logger.getLogger(GennyDeserializer.class);

	Jsonb jsonb = JsonbBuilder.create();

	UserToken userToken;

	/**
	 * Constructor. Used to activate request scope and fetch token bean.
	 **/
	public GennyDeserializer() {
		// activate our request scope
		Arc.container().requestContext().activate();
		// find beans in container
		userToken = Arc.container().instance(UserToken.class).get();
	}

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) { }

    @Override
    public String deserialize(String topic, byte[] data) {

		log.info("[*] Calling deserialize for GennyDeserializer");

		if (data == null) {
			log.error("Null received at deserializing");
			return null;
		}

		try {
			String deser = new String(data, "UTF-8");

			JsonObject json = jsonb.fromJson(deser, JsonObject.class);
			String token = json.getString("token");

			// init GennyToken from token string
			userToken.init(token);
			log.info("Token Initialized: " + userToken);

			return deser;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
    }

    @Override
    public void close() { }
}
