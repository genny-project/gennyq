package life.genny.fyodor.endpoints;

import java.util.List;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import io.vertx.core.http.HttpServerRequest;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Link;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.ItemNotFoundException;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.serviceq.Service;

/**
 * QwandaEndpoint --- A temporary replacement for the api service
 *
 * @author adam@gada.io
 * @author jasper.robison@gada.io
 *
 */
@Path("/qwanda")
public class QwandaEndpoint {

	private static final Logger log = Logger.getLogger(QwandaEndpoint.class);

	static Jsonb jsonb = JsonbBuilder.create();

	@Context
	HttpServerRequest request;

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	Service service;

	@Inject
	UserToken userToken;

	@Inject
	ServiceToken serviceToken;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	EntityManager entityManager;

	@GET
	@Path("/baseentitys/{sourceCode}/asks2/{questionCode}/{targetCode}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createAsks3(
			@PathParam("sourceCode") final String sourceCode,
			@PathParam("questionCode") final String questionCode, 
			@PathParam("targetCode") final String targetCode,
			@Context final UriInfo uriInfo
			) {

		if (userToken == null) {
			log.error("Bad or no header token in request");
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		userToken.setProductCode("internmatch");

		BaseEntity source = null;
		BaseEntity target = null;

		if ("PER_SOURCE".equals(sourceCode) && "PER_TARGET".equals(targetCode)) {
			source = new BaseEntity(sourceCode, "SourceCode");
			target = new BaseEntity(targetCode, "TargetCode");
		} else {
			source = beUtils.getBaseEntityByCode(sourceCode);
			target = beUtils.getBaseEntityByCode(targetCode);
		}

		if (source == null) {
			log.error("No Source entity found!");
			return null;
		}

		if (target == null) {
			log.error("No Target entity found!");
			return null;
		}

		log.info("Fetching asks -> " + questionCode + ":" + source.getCode() + ":" + target.getCode());

		// fetch question from DB
		Ask ask = qwandaUtils.generateAskFromQuestionCode(questionCode, source, target);

		if (ask == null) {
			log.error("No ask returned for " + questionCode);
			return null;
		}

		// create ask msg from asks
		log.info("Creating ask Message...");
		QDataAskMessage msg = new QDataAskMessage(ask);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);

		String json = jsonb.toJson(msg);

		return Response.ok().entity(json).build();
	}

	@GET
	@Path("/entityentitys/{targetCode}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEntityEntitys(@PathParam("targetCode") String targetCode) {

		log.info("Request for EntityEntitys " + targetCode);

		if (userToken == null) {
			log.error("Bad or no header token in entityentity GET provided");
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		log.info("GENNY_TOKEN = " + userToken);
		log.info("SERVICE_TOKEN = " + serviceToken);

		return Response.ok().build();
	}

	@GET
	@Path("/entityentitys/{targetCode}/parents")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEntityEntitysParents(@PathParam("targetCode") String targetCode) {

		log.info("Request for EntityEntitys Parents " + targetCode);

		log.info("GENNY_TOKEN = " + userToken);
		log.info("SERVICE_TOKEN = " + serviceToken);

		if (userToken == null) {
			log.error("Bad or no header token in entityentityParents GET provided");
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		List<Link> items = databaseUtils.findParentLinks(userToken.getProductCode(), targetCode);
		Link[] array = items.toArray(new Link[0]);
		String json = jsonb.toJson(array);

		return Response.ok().entity(json).build();
	}

	@POST
	@Path("/baseentitys")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response saveBaseEntity(BaseEntity baseEntity) {

		log.info("Request to SAVE BaseEntity");

		log.info("GENNY_TOKEN = " + userToken);
		log.info("SERVICE_TOKEN = " + serviceToken);

		if (userToken == null) {
			log.error("Bad or no header token in entityentityParents GET provided");
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		beUtils.updateBaseEntity(baseEntity);

		return Response.ok().entity(jsonb.toJson(baseEntity)).build();
	}

	@GET
	@Path("/questioncodes/{code}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getQuestion(@PathParam("code") String code) {

		log.info("Request for Question " + code);

		log.info("GENNY_TOKEN = " + userToken);
		log.info("SERVICE_TOKEN = " + serviceToken);

		if (userToken == null) {
			log.error("Bad or no header token in entityentityParents GET provided");
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		String productCode = userToken.getProductCode();
		Question question;
		try {
			question = databaseUtils.findQuestionByCode(productCode, code);
		} catch (NoResultException e) {
			throw new ItemNotFoundException(code, e);
		}

		return Response.ok().entity(jsonb.toJson(question)).build();
	}
}
