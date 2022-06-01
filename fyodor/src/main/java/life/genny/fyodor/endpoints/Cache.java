package life.genny.fyodor.endpoints;

import io.vertx.core.http.HttpServerRequest;
import java.io.StringReader;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
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
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.serialization.baseentity.BaseEntityKey;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.HttpUtils;
import life.genny.serviceq.Service;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

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

	/**
	 * Read an item from the cache and return in json status format.
	 *
	 * @param productCode The productCode of the cache item
	 * @param key         The key of the cache item
	 * @return The json item
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{productCode}/{key}/json")
	public Response readProductCodeKeyJson(@PathParam("productCode") String productCode, @PathParam("key") String key) {

		Response res = readProductCodeKey(productCode, key);

		JsonObject json = null;
		if (res.getStatus() == 200) {
			String replyString = res.getEntity().toString();

			try {
				JsonReader jsonReader = Json.createReader(new StringReader(replyString));
				JsonObject reply = jsonReader.readObject();

				json = javax.json.Json.createObjectBuilder()
						.add("status", "ok")
						.add("value", reply)
						.build();

			} catch (javax.json.stream.JsonParsingException je) {

				json = javax.json.Json.createObjectBuilder()
						.add("status", "ok")
						.add("value", replyString)
						.build();
			}

		} else {
			json = javax.json.Json.createObjectBuilder()
					.add("status", "error")
					.add("value", res.getStatusInfo().toString())
					.build();
		}

		return Response.ok().entity(json).build();
	}

	/**
	 * Read an item from the cache.
	 *
	 * @param productCode The productCode of the cache item
	 * @param key         The key of the cache item
	 * @return The json item
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{productCode}/{key}")
	public Response readProductCodeKey(@PathParam("productCode") String productCode, @PathParam("key") String key) {

		//log.info("[!] read(" + productCode + ":" + key + ")");

		if (userToken == null) {
			// TODO - using serviceToken
			log.warn("userToken is NULL - FIX!");
			// return Response.status(Response.Status.BAD_REQUEST)
			// .entity(HttpUtils.error("Not authorized to make this request")).build();
		}

		if (!userToken.hasRole("test", "service") && false) {  // TODO : make work for non service users.
			log.warn("User [" + userToken.userCode + "] does not have valid role:" + userToken.getUserRoles());
			// TODO -> Do not permit access from externally
			if ("jenny".equals(productCode) && (key.startsWith("TOKEN"))) {
				log.warn("jenny and TOKEN returning " + serviceToken.getToken().substring(0, 10));
				return Response.ok(serviceToken.getToken()).build();
			}
			if ("JENNY".equals(productCode) && (key.startsWith("TOKEN"))) {
				log.warn("JENNY and TOKEN returning " + serviceToken.getToken().substring(0, 10));
				return Response.ok(serviceToken.getToken()).build();
			}
			if ("JENNY".equals(productCode) && (key.startsWith("SKIP"))) {
				log.warn("JENNY and SKIP returning " + serviceToken.getToken().substring(0, 10));
				return Response.ok("false").build();
			}

			if ("CACHE:SERVICE_TOKEN".equals(key)) {
				log.warn("JENNY and TOKEN returning " + serviceToken.getToken().substring(0, 10));
				return Response.ok(serviceToken.getToken()).build();
			}

			log.warn("No token,  returning BAD-REQUEST " + serviceToken.getToken().substring(0, 10));
		}

		if ((key.contains(":")) || ("attributes".equals(key))) {
			// It's a token
			String json = (String) CacheUtils.readCache(productCode, key);

			if (StringUtils.isBlank(json)) {
				log.info("Could not find in cache: " + key);
				return Response.status(Response.Status.NO_CONTENT).build();
			}

			return Response.ok(json).build();

		} else if (key.charAt(3) == '_' 
				&& !key.startsWith("SBE")
				&& !key.startsWith("QUE")
				&& !key.startsWith("FRM")
				&& !key.startsWith("ADD")) {

			// It's a baseentity
			BaseEntityKey baseEntityKey = new BaseEntityKey(productCode, key);
			try {
				log.info("Getting BE with code " + key);
				BaseEntity baseEntity = (BaseEntity) CacheUtils.getEntity(GennyConstants.CACHE_NAME_BASEENTITY,
						baseEntityKey);

				if (baseEntity == null) {
					throw new Exception("Not found in cache");
				}
				return Response.ok(jsonb.toJson(baseEntity)).build();

			} catch (Exception e) {
				// TODO: just to get something going..
				log.warn("BaseEntity not found in cache, fetching from database");
				BaseEntity be = databaseUtils.findBaseEntityByCode(productCode, key);

				return Response.ok(jsonb.toJson(be)).build();
			}

		} else {

			if ("CAPABILITIES".equals(key)) {
				log.warn("productCode: [" + productCode + "] ; key: [" + key + "] " + serviceToken.getToken());
				String json = (String) CacheUtils.readCache(productCode, key);
				return Response.ok(json).build();
			}
			if ("ACTIVE_BRIDGE_IDS".equals(key)) {
				log.warn("productCode: [" + productCode + "] ; key: [" + key + "] " + StringUtils.abbreviate(serviceToken.getToken(),20));
				String json = (String) CacheUtils.readCache(productCode, key);
				return Response.ok(json).build();
			}
			if ("JENNY".equals(productCode) && "SKIP".equals(key)) {
				log.warn("productCode: [" + productCode + "] ; key: [" + key + "] " + serviceToken.getToken());
				return Response.ok("TRUE").build(); // always return true
			}
			if ("jenny".equals(productCode)) {
				log.warn("productCode: [" + productCode + "] ; key: [" + key + "] " + serviceToken.getToken());
				return Response.ok(serviceToken.getToken()).build();
			}
			if (key.startsWith("TOKEN")) {
				log.warn("productCode: [" + productCode + "] ; key: [" + key + "]");
				return Response.ok(serviceToken.getToken()).build();
			}

			String json = (String) CacheUtils.readCache(productCode, key);

			return Response.ok(json).build();
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{productCode}/{key}")
	public Response write(@PathParam("productCode") String productCode, @PathParam("key") String key, String value) {

		if (value != null) {
			log.info("[!] write(" + productCode + ":" + key + ":" + StringUtils.abbreviate(value, 20) + ")");
		} else {
			log.info("[!] write(" + productCode + ":" + key + ":null)");
		}

		if (userToken == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(HttpUtils.error("Not authorized to make this request")).build();
		}

		if (key.charAt(3) == '_'
				&& !key.startsWith("SBE")
				&& !key.startsWith("QUE")
				&& !key.startsWith("FRM")
				&& !key.startsWith("ADD")) {
			log.info("Writing to baseentity cache " + productCode + ":" + key);
			// It's a baseentity
			BaseEntityKey baseEntityKey = new BaseEntityKey(productCode, key);
			BaseEntity entity = jsonb.fromJson(value, BaseEntity.class);
			CacheUtils.saveEntity(GennyConstants.CACHE_NAME_BASEENTITY,
					baseEntityKey, entity);
		} else {
			CacheUtils.writeCache(productCode, key, value);
		}
		log.info("Wrote json of length " + value.length() + " for " + key);

		return Response.ok().build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{productCode}/{key}/savenull")
	public Response writeNull(@PathParam("productCode") String productCode, @PathParam("key") String key) {

	
			log.info("[!] saveNull(" + productCode + ":" + key + ")");


		if (userToken == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(HttpUtils.error("Not authorized to make this request")).build();
		}


			CacheUtils.writeCache(productCode, key, null);

		log.info("Wrote null for " + key);

		return Response.ok().build();
	}


	/**
	 * Read an item from the cache.
	 *
	 * @param key The key of the cache item
	 * @return The json item
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{key}")
	public Response read(@PathParam("key") String key) {

		log.info("[!] read(" + key + ")");

		if (userToken == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(HttpUtils.error("Not authorized to make this request")).build();
		}

		log.info("User: " + userToken.getUserCode());
		log.info("Product Code/Cache: " + userToken.getProductCode());

		String productCode = userToken.getProductCode();
		String json = (String) CacheUtils.readCache(productCode, key);

		if (json == null) {
			log.info("Could not find in cache: " + key);
			return Response.ok("null").build();
		}

		log.info("Found json of length " + json.length() + " for " + key);

		return Response.ok(json).build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{key}")
	public Response write(@PathParam("key") String key, String value) {

		log.info("Writing to cache " + userToken.getProductCode() + ": [" + key + ":" + value + "]");

		if (userToken == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(HttpUtils.error("Not authorized to make this request")).build();
		}

		String productCode = userToken.getProductCode();
		if (key.charAt(3) == '_') {
			log.info("Writing to baseentity cache " + productCode + ":" + key);
			// It's a baseentity
			BaseEntityKey baseEntityKey = new BaseEntityKey(productCode, key);
			BaseEntity entity = jsonb.fromJson(value, BaseEntity.class);
			CacheUtils.saveEntity(GennyConstants.CACHE_NAME_BASEENTITY,
					baseEntityKey, entity);
		} else {
			CacheUtils.writeCache(productCode, key, value);
		}

		log.info("Wrote json of length " + value.length() + " for " + key);

		return Response.ok().build();
	}

	
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{key}")
	public Response remove(@PathParam("key") String key) {

		if (userToken == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(HttpUtils.error("Not authorized to make this request")).build();
		}

		String productCode = userToken.getProductCode();
		CacheUtils.removeEntry(productCode, key);

		log.info("Removed Item for " + key);

		return Response.ok().build();
	}

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{productCode}/{key}")
	public Response delete(@PathParam("productCode") String productCode, @PathParam("key") String key) {

		if (userToken == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(HttpUtils.error("Not authorized to make this request")).build();
		}

		CacheUtils.removeEntry(productCode, key);

		log.info("Removed Item for " + key);

		return Response.ok().build();
	}

}
