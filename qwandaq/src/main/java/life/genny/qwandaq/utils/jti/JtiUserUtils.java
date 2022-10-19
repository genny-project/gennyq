package life.genny.qwandaq.utils.jti;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class JtiUserUtils {

    @Inject
    KafkaStreams streams;

    public JtiResult getJtiData(String userCode) {
        JtiAggregation result = getJtiStore().get(userCode);

        if (result != null) {
            return JtiResult.found(JtiEntity.from(result));
        }
        else {
            return JtiResult.notFound();
        }
    }

    private ReadOnlyKeyValueStore<String, JtiAggregation> getJtiStore() {
        while (true) {
            try {
                return streams.store(StoreQueryParameters.fromNameAndType(JtiTopology.JTI_EVENTS,
                        QueryableStoreTypes.keyValueStore()));

            } catch (InvalidStateStoreException e) {
                // ignore, store not ready yet
            }
        }
    }

}
