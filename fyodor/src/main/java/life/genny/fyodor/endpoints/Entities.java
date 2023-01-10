package life.genny.fyodor.endpoints;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.transaction.Transactional;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import io.vertx.core.http.HttpServerRequest;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.serialization.baseentity.BaseEntityKey;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.HttpUtils;
import life.genny.serviceq.Service;

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
	CacheManager cm;

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	UserToken userToken;

	@Inject
	Service service;

	@Inject
	BaseEntityUtils beUtils;

	private static final String NOT_AUTHORIZED_TO_MAKE_THIS_REQUEST = "Not authorized to make this request";

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
					.entity(HttpUtils.error(NOT_AUTHORIZED_TO_MAKE_THIS_REQUEST)).build();
		}

		String productCode = userToken.getProductCode();
		BaseEntityKey beKey = new BaseEntityKey(productCode, code);
		log.info("$$$$$$$$$$$$$$$$     reading entity from cache..");
		BaseEntity entity = (BaseEntity) cm.getEntity(CacheManager.CACHE_NAME_BASEENTITY, beKey);
		if (entity != null) {
			log.info("$$$$$$$$$$$$$$$$    entity in cache.. returning...  ");
		} else {
			entity = databaseUtils.findBaseEntityByCode(userToken.getProductCode(), code);
			if (entity != null) {
				// Entity found in DB but not cache. Add it to cache..
				log.info("$$$$$$$$$$$$$$$$    entity not in cache, but in DB.. adding to cache...  ");
				cm.saveEntity(CacheManager.CACHE_NAME_BASEENTITY, beKey, entity);
				log.info("$$$$$$$$$$$$$$$$    entity not in cache, but in DB.. added to cache...  ");
			} else {
				log.error(getBaseEntityNotFoundLog(userToken.getProductCode(), code));
			}
		}

		return Response.ok(entity).build();
	}
	
	/**
	 * Read an item from the cache.
	 *
	 * @param key The key of the cache item
	 * @return The json item
	 */
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{code}")
	public Response delete(@PathParam("code") String code) {

		if (userToken == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(HttpUtils.error(NOT_AUTHORIZED_TO_MAKE_THIS_REQUEST)).build();
		}

		BaseEntity entity = databaseUtils.findBaseEntityByCode(userToken.getProductCode(), code);
		if (entity == null) {
			log.error(getBaseEntityNotFoundLog(userToken.getProductCode(), code));
		}

		return Response.ok(entity).build();
	}
	
	/**
	 * Read an item from the cache.
	 *
	 * @param key The key of the cache item
	 * @return The json item
	 */
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	public Response update(BaseEntity entity) {
		String code = entity.getCode();
		log.info("Put BE " + code);
		if (userToken == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(HttpUtils.error(NOT_AUTHORIZED_TO_MAKE_THIS_REQUEST)).build();
		}

		// NOTE: Forgive me for this, but the rulesservice needs it :(
		String productCode = entity.getRealm();
		userToken.setProductCode(productCode);

		BaseEntityKey beKey = new BaseEntityKey(productCode, code);
		cm.saveEntity(productCode, beKey, entity);
		// beUtils.updateBaseEntity(entity);

		return Response.ok(entity).build();
	}

	/**
	 * Read an item from the cache.
	 *
	 * @param key The key of the cache item
	 * @return The json item
	 */
	@GET
	@Path("/create")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response create() {

		log.info("received request to create entity");

		BaseEntity be = new BaseEntity("TST_ENTITY", "Test Entity");

		log.info("Created BaseEntity " + be.getCode());
		Attribute attr = cm.getAttribute("LNK_AUTHOR");
		EntityAttribute attribute = new EntityAttribute(be, attr, 1.0, "TEST");
		attribute.setBaseEntityCode(be.getCode());
		attribute.setAttribute(attr);

		try {
			be.addAttribute(attribute);
		} catch (Exception e) {
			e.printStackTrace();
		}

		beUtils.updateBaseEntity(be);

		BaseEntity saved = databaseUtils.findBaseEntityByCode(userToken.getProductCode(), be.getCode());
		log.info("SAVED = " + jsonb.toJson(saved));

		return Response.ok(be).build();
	}

	/**
	 * Read an item from the cache.
	 *
	 * @param key The key of the cache item
	 * @return The json item
	 */
	@POST
	@Path("/{productCode}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response create(@PathParam("productCode") String productCode, String baseentityJson) {

		if (userToken == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(HttpUtils.error(NOT_AUTHORIZED_TO_MAKE_THIS_REQUEST)).build();
		}

		BaseEntity be = jsonb.fromJson(baseentityJson, BaseEntity.class);
		be.setRealm(productCode);
		databaseUtils.saveBaseEntity(be);

	BaseEntity entity = databaseUtils.findBaseEntityByCode(productCode, be.getCode());

		if (entity == null) {
			log.error(getBaseEntityNotFoundLog(productCode, be.getCode()));
		}

		return Response.ok(entity.getId()).build();
	}

	private String getBaseEntityNotFoundLog(String productCode, String code) {
		return "productCode=[" + productCode + "] , code=" + code;
	}
}
