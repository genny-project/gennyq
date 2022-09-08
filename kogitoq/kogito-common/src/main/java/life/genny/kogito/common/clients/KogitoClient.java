package life.genny.kogito.common.clients;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.vertx.core.json.JsonObject;
import life.genny.qwandaq.exception.ResponseException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

/**
 * KogitoClient --- Make call to kogito workflow endpoints
 * This class is mostly used to send information to perform workflow functions which
 * has to be synchronous so kafka or any asyn implementation cannot be
 * used.
 *
 * @author hello@gada.io
 */
@Path("/")
@RegisterRestClient
@RegisterProvider(value = ResponseException.class, priority = 50)
public interface KogitoClient {

    @POST
    @Path("/datawithreply")
    JsonObject delete(String processId);
}
