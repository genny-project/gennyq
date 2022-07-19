package life.genny.fyodor.endpoints;

import java.util.List;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import io.vertx.core.http.HttpServerRequest;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.serviceq.Service;

/**
 * ServiceEndpoint --- A temporary replacement for the service endpoint
 *
 * @author jasper.robison@gada.io
 */
@Path("/service")
public class ServiceEndpoint {

	private static final Logger log = Logger.getLogger(ServiceEndpoint.class);

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
	@Path("/forms")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getForms() {

		// TODO: we should be getting rid of this file soon anyways
		String productCode = "internmatch";
		try {
			Query q = entityManager.createNativeQuery(
					"select distinct(q.code) from question q LEFT JOIN question_question qq ON q.code = qq.sourceCode WHERE qq.sourceCode IS NOT NULL and q.realm='"
					+ productCode + "' and q.code like 'QUE_%_GRP'");

			List<Object[]> questionCodes = q.getResultList();
			String json = jsonb.toJson(questionCodes);

			return Response.status(200).entity(json).build();
		} catch (Exception e) {
			log.error("Error in fetching forms for productCode " + productCode + " " + e.getLocalizedMessage());
		}

		return Response.status(401).entity("Bad Request Made!").build();

	}
}
