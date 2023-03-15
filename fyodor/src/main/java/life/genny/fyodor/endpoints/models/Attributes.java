package life.genny.fyodor.endpoints.models;

import io.vertx.core.http.HttpServerRequest;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.annotations.UpdateTimestamp;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.serialization.attribute.AttributeKey;
import life.genny.qwandaq.utils.AttributeUtils;

/**
 * Attributes Endpoints providing database attribute access
 *
 * @author jasper.robison@gada.io
 */
@Path("/attributes")
public class Attributes {

	@Inject
	Logger log;

	static Jsonb jsonb = JsonbBuilder.create();

	@Context
	HttpServerRequest request;

	@Inject
	CacheManager cm;

	@Inject
	UserToken userToken;

	@Inject
	AttributeUtils attributeUtils;

	/**
	 * Read an item from the cache.
	 *
	 * @param key The key of the cache item
	 * @return The json item
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{realm}/{code}")
	public Response read(@PathParam("realm") final String realm, @PathParam("code") String code) {
		log.debug("[!] call to GET /attributes/" + realm + "/" + code);

		if (userToken == null) {
			return Response.status(Response.Status.FORBIDDEN)
					.entity("Not authorized to make this request").build();
		}

		try {
			Attribute attribute = attributeUtils.getAttribute(realm, code, true, true);
			return Response.ok(attribute).build();
		} catch(ItemNotFoundException e) {
			return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
		}
	}

	// @PUT
	// @Consumes(MediaType.APPLICATION_JSON)
	// @Path("/{realm}")
	// public Response edit(@PathParam("realm") final String realm, @Valid Attribute attribute) {
	// 	log.debug("[!] call to GET /attributes/" + realm + "/");
	// 	log.debug("Editing Attribute: " + attribute.getCode());

	// }

	/**
	 * Read an item from the cache.
	 *
	 * @param key The key of the cache item
	 * @return The json item
	 */
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{realm}/{code}")
	public Response deleteAttribute(@PathParam("realm") String realm, @PathParam("code") String code) {
		log.debug("[!] call to DELETE /attributes/" + realm + "/" + code);
		if (userToken == null) {
			return Response.status(Response.Status.FORBIDDEN)
					.entity("Not authorized to make this request").build();
		}
		int numAffected = cm.removeAttribute(realm, code);
		if(numAffected > 0)
			return Response.ok().entity("Num Entities affected: " + numAffected).build();
		else
			return Response.status(Response.Status.NOT_FOUND).entity("No entities affected by delete of " + realm + ":" + code).build();
	}
}
