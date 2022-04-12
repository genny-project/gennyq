package life.genny.bridge.mobile;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import life.genny.bridge.client.RulesserviceClient;

/**
 * MobileResource --- All endpoints related to the mobile app external client
 *
 * @author    hello@gada.io
 *
 */
@Path("/v7/api/service/sync")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MobileResource {

	private static final Logger log = Logger.getLogger(MobileResource.class);

	@Context
	UriInfo info;

	@Context
	HttpServerRequest request;

	@Inject @RestClient RulesserviceClient virtualChannel;

	/**
	 * The mobile app client will sync it database to the schema of interest and the persisted in our
	 * backeds 
	 *
	 * @param body A Json string object
	 *
	 * @return 200 or 500 depending of the request data and the response from wildfly-rulesservice
	 */
	@POST
	@RolesAllowed({"user"})
	public Response sync(String body) {

		log.info("Mobile app from a user in sync with the database");
		final String bodyString = new String(body);
		String cleanedBody= bodyString.toString()
			.replaceAll("[^\\x20-\\x7E]", " ")
			.replaceAll(" +", " ")
			.trim();
		JsonObject rawMessage = null;

		try {
			rawMessage = new JsonObject(cleanedBody);
		} catch (Exception e) {
			log.error("An error occurred when parsing message to JsonObject " + cleanedBody.substring(1,20));
			JsonObject err = new JsonObject().put("status", "error");
			return Response.ok(err).build();
		}

		String token  = request.getHeader("authorization").split("Bearer ")[1];
		if (token != null) { 
			rawMessage.put("token", token);

			try {
				JsonObject res = virtualChannel.sendPayload(rawMessage);
				return Response.ok(res.toString()).build();

			} catch (Exception e) {
				log.error("An error occurred when sending payload to rulesservice datawithreply with the following "+
						"exception ",e);
				return Response.serverError().build();
			}
		} else {
			return Response.ok(new JsonObject().put("status", "no token")).build();
		}
	}

}
