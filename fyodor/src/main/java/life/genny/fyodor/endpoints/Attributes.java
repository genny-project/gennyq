package life.genny.fyodor.endpoints;

import io.vertx.core.http.HttpServerRequest;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.message.QDataAttributeMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.HttpUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Attributes Endpoints providing database attribute access
 *
 * @author jasper.robison@gada.io
 */
@Path("/attributes")
public class Attributes {

	private static final Logger log = Logger.getLogger(Attributes.class);

	static Jsonb jsonb = JsonbBuilder.create();

	@Context
	HttpServerRequest request;

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	CacheManager cm;

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

		String productCode = userToken.getProductCode();
		Attribute attribute = cm.getAttribute(productCode, code);

		return Response.ok(attribute).build();
	}

	/**
	 * Read an item from the cache.
	 *
	 * @param key The key of the cache item
	 * @return The json item
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/realms/{realm}")
	public Response readAttributes(@PathParam("realm") String realm) {

		if (userToken == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(HttpUtils.error("Not authorized to make this request")).build();
		}

		List<Attribute> attributeList = databaseUtils.findAttributes(realm, 0, 10000, "");

		QDataAttributeMessage attributeMsg = new QDataAttributeMessage(attributeList);
		return Response.ok(attributeMsg).build();
	}

	/**
	 * Read an item from the cache.
	 *
	 * @param key The key of the cache item
	 * @return The json item
	 */
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{realm}/{attributeCode}")
	public Response deleteAttribute(@PathParam("realm") String realm,@PathParam("attributeCode") String attributeCode) {

		if (userToken == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(HttpUtils.error("Not authorized to make this request")).build();
		}

		log.info("Api Attribute delete of "+realm+" : "+attributeCode);

		
		return Response.ok().build();
	}
}
