package life.genny.bridge.live.data;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.vertx.core.json.JsonObject;
import life.genny.bridge.model.grpc.Empty;
import life.genny.bridge.model.grpc.Item;
import life.genny.bridge.model.grpc.Stream;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.security.keycloak.KeycloakTokenPayload;
import life.genny.qwandaq.session.bridge.BridgeSwitch;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Implementation of {@link Stream} that handles GRPC communication
 * between bridge and the frontend. Needs refining to use infispan cache rather
 * than a local hashmap
 * 
 * @author Dan
 */
@GrpcService
@Default
@Singleton
public class BridgeGrpcService implements Stream {

    @ConfigProperty(name = "bridge.id", defaultValue = "false")
	String bridgeId;

    private static final Logger LOG = Logger.getLogger(BridgeGrpcService.class);

    @Inject
    ExternalConsumer handler;

    /**
     * Duration to wait before a timeout is fired
     */
    private final Duration timeout = Duration.ofSeconds(15);

    /**
     * Stores a map between jti and
     * a broadcast processor
     */
    private static final Map<String, BroadcastProcessor<Item>> processors = new HashMap<>();


    /**
     * Called when a connection errors out in some way
     * 
     * @param jti
     */
    private static void onError(String jti) {
        LOG.warn("Session with jti " + jti + " just errored out!");
        processors.get(jti).onComplete();
        processors.remove(jti);
    }

    /**
     * Is called by the Frontend to create a new connection.
     * Should create an {@link BroadcastProcessor} and {@link Multi} for the
     * connection
     */
    @Override
    public Multi<Item> connect(Item request) {

        KeycloakTokenPayload payload = getPayload(request);

        if (processors.containsKey(payload.jti)) {
            LOG.error("2 sessions with the same token tried to connect!");
            return Multi.createFrom().failure(io.grpc.Status.ALREADY_EXISTS.withDescription("Client already connected!").asRuntimeException());
        }
        BroadcastProcessor<Item> processor = BroadcastProcessor.create();
        Multi<Item> multi = processor
                // .onItem().invoke() // - Called when an item is being sent
                .ifNoItem().after(timeout).failWith(io.grpc.Status.ABORTED.withDescription("Client timed out!").asRuntimeException())
                .onFailure().invoke(() -> {
                    onError(payload.jti);
                });

        processors.put(payload.jti, processor);

        LOG.info("New session with jti " + payload.jti + " just connected!");

        return multi;
    }

    /**
     * Is called by the Frontend when they want to send data
     * 
     * @param request - A multi of Items containing the data
     */
    @Override
    public Uni<Empty> sink(Item request) {
        LOG.info("Got data from " + getPayload(request).jti);

        routeMessage(request);

        // Return an Empty Uni
        return Uni.createFrom().nothing();
    }

    /**
     * Call this to send data to the frontend based on a jti
     * 
     * @param jti - User to send to
     * @param data  - Data to send. Can possibly be a Multi if we want to send a few
     *              things
     */
    public void send(String jti, Item data) {

        if (processors.containsKey(jti)) {
            processors.get(jti).onNext(data);
        }

        // Throw an exception or something?

    }

    /**
     * Broadcast data to all connected clients
     * 
     * @param data
     */
    public void broadcast(Item data) {

        for (Entry<String, BroadcastProcessor<Item>> entry : processors.entrySet()) {
            send(entry.getKey(), data);
        }

    }

    /**
     * Heartbeat to keep connections alive. Should be called by the frontend on a
     * timer
     */
    @Override
    public Uni<Empty> heartbeat(Item request) {

        KeycloakTokenPayload payload = getPayload(request);

        send(payload.jti, request);

        return Uni.createFrom().nothing();
    }

    /**
     * Takes an item sent through {@link BridgeGrpcService#sink} and sends it through to kafka
     * @param request
     */
    public void routeMessage(Item request) {
        KeycloakTokenPayload payload = getPayload(request);
        LOG.info("JTI " + payload.jti + " " + payload.sid);
        JsonObject object = new JsonObject(request.getBody());
        // put bridgeId into users cached info
		BridgeSwitch.put(new GennyToken(request.getToken()), bridgeId);
		BridgeSwitch.addActiveBridgeId(new GennyToken(request.getToken()), bridgeId);
        handler.routeDataByMessageType(object, new GennyToken(request.getToken()));
    }

    /**
     * Creates a keycloakTokenPayload out of an Item, based on its token
     * @param request
     * @return
     */
    private KeycloakTokenPayload getPayload(Item request) {
        return KeycloakTokenPayload.decodeToken(request.getToken());
    }

}
