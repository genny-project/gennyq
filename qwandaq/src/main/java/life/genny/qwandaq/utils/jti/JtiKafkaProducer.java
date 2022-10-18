package life.genny.qwandaq.utils.jti;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class JtiKafkaProducer {

//    @Outgoing("jti-events")
    public String produce() {
        return "event";
    }
}
