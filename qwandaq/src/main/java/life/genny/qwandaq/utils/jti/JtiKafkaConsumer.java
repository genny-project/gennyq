package life.genny.qwandaq.utils.jti;

import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.utils.KafkaUtils;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.lang.invoke.MethodHandles;

@ApplicationScoped
public class JtiKafkaConsumer {
    private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass());
    static Jsonb jsonb = JsonbBuilder.create();

    @Incoming("jti_events")
    public String produce(String event)
    {
        JsonObject json = jsonb.fromJson(event, JsonObject.class);
        LOGGER.info(event);

        String userCode = json.getString("userUUID");;
        String jti = json.getString( "JTI");
        String productCode = json.getString( "productCode");
        String realm = json.getString( "realm");
//        String authDateTime = json.getString("authDateTime");

        JtiEntity entity = new JtiEntity();
        entity.userCode = userCode;
        entity.productCode = productCode;
        entity.jti = jti;
        entity.realm = realm;

        KafkaUtils.writeMsg(KafkaTopic.JTI_EVENTS, entity);

        return event;
    }
}
