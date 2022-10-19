package life.genny.qwandaq.utils.jti;

import io.quarkus.kafka.client.serialization.ObjectMapperSerde;
import io.smallrye.reactive.messaging.annotations.Blocking;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.GlobalKTable;
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

    public static final String JTI_AGGRERATION = "jti_aggregation";
    public static final String JTI_EVENTS = "jti_events";

    @Produces
    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();
        KeyValueBytesStoreSupplier storeSupplier = Stores.persistentKeyValueStore(JTI_AGGRERATION);

        ObjectMapperSerde<JtiEntity> entitySerde = new ObjectMapperSerde<>(JtiEntity.class);
        ObjectMapperSerde<JtiAggregation> mainSerde = new ObjectMapperSerde<>(JtiAggregation.class);


        GlobalKTable<Integer, JtiEntity> jtiEvents = builder.globalTable(JTI_AGGRERATION,
                                Consumed.with(Serdes.Integer(), entitySerde));

        log.info("============================buildTopology()============================");

//        builder.stream(JTI_EVENTS, Consumed.with(Serdes.Integer(), Serdes.String()))
//                .join(jtiEvents, )

        return builder.build();
    }

}
