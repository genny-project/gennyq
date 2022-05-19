package life.genny.fyodor.endpoints;

import io.vertx.core.http.HttpServerRequest;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.HttpUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.jaxrs.PathParam;



/**
 * Entities --- Endpoints providing database entity access
 *
 * @author jasper.robison@gada.io
 *
 */
@Path("/entity")
public class Entities {

	private static final Logger log = Logger.getLogger(Entities.class);

	static Jsonb jsonb = JsonbBuilder.create();

	@Context
	HttpServerRequest request;

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	UserToken userToken;

	/**
	 * Read an item from the cache.
	 *
	 * @param key The key of the cache item
	 * @return The json item
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{code}")
	public Response read(@PathParam("code") String code) {

		if (userToken == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(HttpUtils.error("Not authorized to make this request")).build();
		}

		BaseEntity entity = databaseUtils.findBaseEntityByCode(userToken.getProductCode(), code);
		if (entity == null) {
			log.error("productCode=["+userToken.getProductCode()+"] , code="+code);
		}

		return Response.ok(entity).build();
	}
}
