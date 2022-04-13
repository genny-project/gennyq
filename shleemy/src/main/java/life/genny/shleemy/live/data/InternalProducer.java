package life.genny.shleemy.live.data;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * InternalProducer --- Kafka smalltye producer objects to send to internal consumers backends
 * such as wildfly-rulesservice.
 *
 */
@ApplicationScoped
public class InternalProducer {

    @Inject @Channel("eventsout") Emitter<String> events;
    public Emitter<String> getToEvents() {
        return events;
    }
}
