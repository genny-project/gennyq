package life.genny.kogito.endpoints;

import javax.enterprise.event.Observes;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@Path("/kogito/api/init")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ApiEndpoint {

    private static final Logger log = Logger.getLogger(ApiEndpoint.class);

    @OPTIONS
    public Response opt() {
        return Response.ok().build();
    }

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        log.info("Kogito Endpoint starting");

    }

    @Transactional
    void onShutdown(@Observes ShutdownEvent ev) {
        log.info("Kogito Endpoint Shutting down");
    }
}