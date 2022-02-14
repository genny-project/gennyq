package life.genny.kogito.intf;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import life.genny.kogito.live.data.InternalProducer;
import life.genny.qwandaq.intf.KafkaInterface;

@ApplicationScoped
public class KafkaBean implements KafkaInterface {

    @Inject
    InternalProducer producer;

    private static final Logger log = Logger.getLogger(KafkaBean.class);

    /**
     * Write a string payload to a kafka channel.
     *
     * @param channel
     * @param payload
     */
    public void write(String channel, String payload) {

        if ("webcmds".equals(channel)) {
            producer.getToWebCmds().send(payload);

        } else if ("search_data".equals(channel)) {
            producer.getToSearchData().send(payload);
        } else if ("messages".equals(channel)) {
            producer.getToMessages().send(payload);
        } else {
            log.error("Producer unable to write to channel " + channel);
        }
    }

}
