package life.genny.qwandaq.utils.jti;

import io.smallrye.reactive.messaging.annotations.Blocking;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.Stores;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.lang.invoke.MethodHandles;

@ApplicationScoped
public class JtiTopology {
    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

    public static final String JTI_STORE = "jti-events";

//    @Produces
    @Incoming("jti_events")
    @Blocking
    public void consume(String event) {
        log.info("Test -- " + event);
    }

//    @Produces
//    public Topology buildTopology() {
//        StreamsBuilder builder = new StreamsBuilder();
//
//        KeyValueBytesStoreSupplier storeSupplier = Stores.persistentKeyValueStore(JTI_STORE);
//
//        return builder.build();
//    }

}
