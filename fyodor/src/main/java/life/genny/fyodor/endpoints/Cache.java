package life.genny.fyodor.endpoints;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import io.vertx.core.http.HttpServerRequest;
import life.genny.qwandaq.constants.CacheName;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.serialization.common.key.cache.CacheKey;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.HttpUtils;
import life.genny.serviceq.Service;

/**
 * Cache --- Endpoints providing cache access
 *
 * @author jasper.robison@gada.io
 */
@Path("/cache")
public class Cache {

	private static final Logger log = Logger.getLogger(Cache.class);

	static Jsonb jsonb = JsonbBuilder.create();

	@Context
	HttpServerRequest request;

	@Inject
	Service service;

	@Inject
	ServiceToken serviceToken;

	@Inject
	UserToken userToken;

	@Inject
	DatabaseUtils databaseUtils;

	// New endpoints:
	// Update (notify on update)
	// Read
	// Delete

	// Add to this as necessary
	private boolean isAuthorized() {
		return userToken != null;
	}

	@DELETE
	@Path("/{cacheName}/{key}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteKeyToken(@PathParam("cacheName") String cacheNameStr, @PathParam("key") String key) {
		if(!isAuthorized()) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(HttpUtils.error("Not authorized to make this request")).build();
		}

		return deleteKey(cacheNameStr, userToken.getProductCode(), key);
	}

	@DELETE
	@Path("/{cacheName}/{productCode}/{key}")
	public Response deleteKey(@PathParam("cacheName") String cacheNameStr, @PathParam("productCode") String productCode, @PathParam("key") String key) {
		if(!isAuthorized()) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(HttpUtils.error("Not authorized to make this request")).build();
		}
		
		CacheName cacheName = CacheName.getCacheName(cacheNameStr);

		if(cacheName == null) {
			return Response.status(Status.NOT_FOUND).entity("Could not find cache " + cacheNameStr.toUpperCase()).build();
		}

		CacheKey cacheKey = new CacheKey(productCode, key);
		CacheUtils.removeEntry(cacheName, cacheKey);

		return Response.ok().build();
	}

	@POST
	@Path("/{cacheName}/{key}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateKeyToken(@PathParam("cacheName") String cacheNameStr, @PathParam("key") String key, String value) {
		if(!isAuthorized()) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(HttpUtils.error("Not authorized to make this request")).build();
		}

		return updateKey(cacheNameStr, userToken.getProductCode(), key, value);
	}

	@POST
	@Path("/{cacheName}/{productCode}/{key}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateKey(@PathParam("cacheName") String cacheNameStr, @PathParam("productCode") String productCode, @PathParam("key") String key, String value) {
		if(!isAuthorized()) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(HttpUtils.error("Not authorized to make this request")).build();
		}
		CacheName cacheName = CacheName.getCacheName(cacheNameStr);

		// TODO: Add JSON Validation

		JsonObjectBuilder responseJsonBuilder = Json.createObjectBuilder();

		if(cacheName == null) {
			return Response.status(Status.NOT_FOUND).entity( "Could not find cache " + cacheNameStr.toUpperCase()).build();
		}

		CacheKey cacheKey = new CacheKey(productCode, key);
		String previouslyCachedItem = CacheUtils.getObject(cacheName, cacheKey, String.class);
		
		responseJsonBuilder.add("updated", !StringUtils.isBlank(previouslyCachedItem));
		JsonObject responseJson = responseJsonBuilder.add("lastValue", previouslyCachedItem).build();
		CacheUtils.putObject(cacheName, cacheKey, value);
		
		return Response.ok().entity(responseJson).build();
	}



	@GET
	@Path("/{cacheName}/{productCode}/{key}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response readKeyToken(@PathParam("cacheName") String cacheNameStr, @PathParam("key") String key) {
		if(!isAuthorized()) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(HttpUtils.error("Not authorized to make this request")).build();
		}

		return readKey(cacheNameStr, userToken.getProductCode(), key);
	}

	@GET
	@Path("/{cacheName}/{productCode}/{key}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response readKey(@PathParam("cacheName") String cacheNameStr, @PathParam("productCode") String productCode, @PathParam("key") String key) {
		if(!isAuthorized()) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(HttpUtils.error("Not authorized to make this request")).build();
		}
		CacheName cacheName = CacheName.getCacheName(cacheNameStr);

		JsonObject responseJson;

		if(cacheName == null) {
			responseJson = Json.createObjectBuilder()
				.add("status", "error")
				.add("details", "Could not find cache " + cacheNameStr.toUpperCase())
				.build();
			return Response.status(Status.NOT_FOUND).entity(responseJson).build();
		}
		CacheKey cacheKey = new CacheKey(productCode, key);
		String cachedItem = CacheUtils.getObject(cacheName, cacheKey, String.class);

		if(cachedItem == null) {
			responseJson = Json.createObjectBuilder()
				.add("status", "error")
				.add("details", Json.createObjectBuilder()
						.add("message", "could not find item in cache")
						.add("cache", cacheNameStr.toUpperCase())
						.add("key", cacheKey.getFullKeyString())
						.build()
				)
				.build();
			return Response.status(Status.NOT_FOUND).entity(responseJson).build();
		}
		return Response.ok().entity(cachedItem).build();
	}
}
