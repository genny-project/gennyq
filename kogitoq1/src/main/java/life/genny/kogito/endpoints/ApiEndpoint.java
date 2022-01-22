package life.genny.kogito.endpoints;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.http.HttpServerRequest;

/**
 * ApiEndpoint - Endpoints providing Rules API
 */

@Path("/api/kogitoq")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ApiEndpoint {

    private static final Logger log = Logger.getLogger(ApiEndpoint.class);

    @ConfigProperty(name = "genny.show.values", defaultValue = "false")
    Boolean showValues;

    @ConfigProperty(name = "genny.keycloak.url", defaultValue = "https://keycloak.gada.io")
    String baseKeycloakUrl;

    @ConfigProperty(name = "quarkus.infinispan-client.server-list", defaultValue = "infinispan")
    String infinispanUrl;

    @ConfigProperty(name = "kogito.jobs-service.url", defaultValue = "jobs-service")
    String jobsserviceUrl;

    @ConfigProperty(name = "kogito.service.url", defaultValue = "jobs-service")
    String serviceUrl;

    @ConfigProperty(name = "kogito.dataindex.http.url", defaultValue = "data-index")
    String dataIndexHttpUrl;

    @ConfigProperty(name = "project.version", defaultValue = "unknown")
    String version;

    @ConfigProperty(name = "kafka.bootstrap.servers", defaultValue = "kafka:9092")
    String gennyKafkaUrl;

    @Context
    HttpServerRequest request;

    /**
     * A GET request for the running version
     *
     * @return version data
     */
    @GET
    @Path("/version")
    public Response version() {
        return Response.ok().entity("{\"version\": \"" + version + "\"}").build();
    }

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        log.info("Api Endpoint starting");
        if (showValues) {
            log.info("Version=" + version);
            log.info("GENNY_KAFKA_URL=" + gennyKafkaUrl);
            log.info("GENNY_KEYCLOAK_URL=" + baseKeycloakUrl);
            log.info("GENNY_INFINISPAN_URL=" + infinispanUrl);
            log.info("GENNY_KOGITO_JOBSSERVICE_URL=" + jobsserviceUrl);
            log.info("GENNY_KOGITO_SERVICE_URL=" + serviceUrl);
            log.info("GENNY_KOGITO_DATAINDEX_HTTP_URL=" + dataIndexHttpUrl);
        }
    }

    @Transactional
    void onShutdown(@Observes ShutdownEvent ev) {
        log.info("Api Endpoint Shutting down");
    }
}
