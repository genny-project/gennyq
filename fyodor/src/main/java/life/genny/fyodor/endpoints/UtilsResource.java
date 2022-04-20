package life.genny.fyodor.endpoints;

import io.vertx.core.http.HttpServerRequest;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.HttpUtils;
import life.genny.qwandaq.utils.SecurityUtils;
import life.genny.serviceq.Service;
import org.jboss.logging.Logger;



/**
 * Attribute --- Endpoints providing database attribute access
 *
 * @author jasper.robison@gada.io
 *
 */
@Path("/utils")
public class UtilsResource {

	private static final Logger log = Logger.getLogger(UtilsResource.class);

	static Jsonb jsonb = JsonbBuilder.create();

	@Context
	HttpServerRequest request;

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	Service service;

	/**
	 * Read a list of Realms.
	 *
	 *
	 * @return The realm String items
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/realms")
	public Response read(@HeaderParam("Authorization") String token) {
		log.info("Called api realms");
		Boolean authorized = SecurityUtils.isAuthorizedToken(token);
		if (!authorized) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(HttpUtils.error("Not authorized to make this request")).build();
		}

		Set<String> realmSet = new HashSet<>();
		realmSet.add("internmatch");
		realmSet.add("mentormatch");
		realmSet.add("credmatch");
		realmSet.add("lojing");


		return Response.ok(realmSet).build();
	}
}
