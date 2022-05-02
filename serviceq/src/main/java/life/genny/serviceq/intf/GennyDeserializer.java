package life.genny.serviceq.intf;

import java.util.Map;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.kafka.common.serialization.Deserializer;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.models.TokenCollection;

public class GennyDeserializer implements Deserializer<String> {

	static final Logger log = Logger.getLogger(GennyDeserializer.class);

	Jsonb jsonb = JsonbBuilder.create();

	TokenCollection tokens;

	public GennyDeserializer() {
		tokens = Arc.container().instance(TokenCollection.class).get();
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

			GennyToken gennyToken = new GennyToken(token);
			tokens.setGennyToken(gennyToken);

			log.info("Token Initialized: " + gennyToken);

			return deser;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
    }

    @Override
    public void close() { }
}
