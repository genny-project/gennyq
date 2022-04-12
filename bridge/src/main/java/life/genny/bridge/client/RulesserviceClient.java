package life.genny.bridge.client;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.vertx.core.json.JsonObject;
import life.genny.qwandaq.exception.ResponseException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

/**
 * RulesserviceClient --- Make call to wildfly-rulesservice endpoints
 * This class is mostly used to send information to rulesservice which 
 * has to be synchronous so kafka or any asyn implementation cannot be 
 * used. 
 *
 * @author    hello@gada.io
 */
@Path("/eventbus")
@RegisterRestClient
@RegisterProvider(value = ResponseException.class, priority = 50)
public interface RulesserviceClient {

    @POST
    @Path("/datawithreply")
    JsonObject sendPayload(Object name);
}
