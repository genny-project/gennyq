package life.genny.fyodor.endpoints.models;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.Consumes;
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

import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.utils.*;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import io.vertx.core.http.HttpServerRequest;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.serialization.baseentity.BaseEntityKey;

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
	UserToken userToken;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	EntityAttributeUtils beaUtils;

	@Inject
	AttributeUtils attributeUtils;

	private static final String NOT_AUTHORIZED_TO_MAKE_THIS_REQUEST = "Not authorized to make this request";

	/**
	 * Read an item from the cache.
	 *
	 * @param code The code of the base entity to be read
	 * @return OK response containing the read base entity
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
		BaseEntity entity = (BaseEntity) cm.getEntity(GennyConstants.CACHE_NAME_BASEENTITY, beKey);
		if (entity != null) {
			log.debug("Entity in cache.. returning...  ");
		} else {
			entity = beUtils.getBaseEntity(userToken.getProductCode(), code);
			if (entity != null) {
				// Entity found in DB but not cache. Add it to cache..
				log.debug("Entity not in cache, but in DB.. adding to cache...  ");
				cm.saveEntity(GennyConstants.CACHE_NAME_BASEENTITY, beKey, entity);
			} else {
				log.error(getBaseEntityNotFoundLog(userToken.getProductCode(), code));
			}
		}

		return Response.ok(entity).build();
	}
	
	/**
	 * Read an item from the cache.
	 *
	 * @param code The code of the base entity to be deleted
	 * @return OK response containing the deleted base entity
	 */
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{code}")
	public Response delete(@PathParam("code") String code) {

		if (userToken == null) {
			return Response.status(Response.Status.FORBIDDEN)
					.entity(HttpUtils.error(NOT_AUTHORIZED_TO_MAKE_THIS_REQUEST)).build();
		}

		BaseEntity entity = beUtils.removeBaseEntity(userToken.getProductCode(), code);
		if (entity == null) {
			log.error(getBaseEntityNotFoundLog(userToken.getProductCode(), code));
		}

		return Response.ok(entity).build();
	}
	
	/**
	 * Update a base entity the cache.
	 *
	 * @param entity The base entity to be updated
	 * @return OK response containing the updated base entity
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
	 * Create an empty test base entity
	 *
	 * @return OK response containing the created base entity
	 */
	@GET
	@Path("/create")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response create() {

		log.info("received request to create entity");

		BaseEntity be = new BaseEntity("TST_ENTITY", "Test Entity");

		log.info("Created BaseEntity " + be.getCode());
		Attribute attr = attributeUtils.getAttribute("LNK_AUTHOR", true);
		EntityAttribute attribute = new EntityAttribute(be, attr, 1.0, "TEST");
		try {
			be.addAttribute(attribute);
			attribute.setBaseEntityCode(be.getCode());
			attribute.setAttribute(attr);
			beaUtils.updateEntityAttribute(attribute);
		} catch (Exception e) {
			e.printStackTrace();
		}

		BaseEntity saved = beUtils.updateBaseEntity(be);

		log.debug("SAVED = " + jsonb.toJson(saved));

		return Response.ok(be).build();
	}

	/**
	 * Create a base entity from json for a given product
	 *
	 * @param productCode The product code
	 * @param baseentityJson The json representation of the base entity to be created
	 * @return OK response containing the id of the created base entity
	 */
	@POST
	@Path("/{productCode}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response create(@PathParam("productCode") String productCode, String baseentityJson) {
		if (userToken == null) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(HttpUtils.error(NOT_AUTHORIZED_TO_MAKE_THIS_REQUEST)).build();
		}
		BaseEntity be = jsonb.fromJson(baseentityJson, BaseEntity.class);
		be.setRealm(productCode);
		BaseEntity entity = beUtils.updateBaseEntity(be);
		if (entity == null) {
			String msg = getBaseEntityNotFoundLog(productCode, be.getCode());
			log.error(msg);
			return Response.status(Response.Status.NOT_FOUND).entity(msg).build();
		}
		return Response.ok(entity.getId()).build();
	}

	private String getBaseEntityNotFoundLog(String productCode, String code) {
		return "productCode=[" + productCode + "] , code=" + code;
	}
}
