package life.genny.serviceq.endpoints;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.vertx.core.json.JsonObject;

/**
 * Version --- An endpoint dedicated to the service version 
 * query used in healthchecks.
 *
 * @author    jasper.robison@gada.io
 */
@Path("/api/version")
public class Version {

	@ConfigProperty(name = "genny.version", defaultValue = "Not supported")
	String version;
	
    /**
     * An endpoint to return the service version details.
     *
     * @return JsonObject
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject version() {
        return new JsonObject().put("version", version);
    }
}
