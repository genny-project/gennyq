package life.genny.fyodor.endpoints;

import io.vertx.core.http.HttpServerRequest;
import java.util.List;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.QuestionUtils;
import life.genny.serviceq.Service;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

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
	Service service;

	@Inject
	QuestionUtils questionUtils;

	@Inject
	UserToken userToken;

	@GET
	@Consumes("application/json")
	@Path("/baseentitys/{sourceCode}/asks2" + "/{questionCode}/{targetCode}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createAsks3(@PathParam("sourceCode") final String sourceCode,
			@PathParam("questionCode") final String questionCode, @PathParam("targetCode") final String targetCode,
			@Context final UriInfo uriInfo) {

			if (userToken == null) {
				log.error("Bad or no header token in Search POST provided");
				return Response.status(Response.Status.BAD_REQUEST).build();
			}

			List<Ask> asks = questionUtils.createAsksByQuestionCode(questionCode, sourceCode, targetCode);

			log.debug("Number of asks=" + asks.size());
			log.debug("Number of asks=" + asks);

			final QDataAskMessage askMsgs = new QDataAskMessage(asks.toArray(new Ask[0]));
			askMsgs.setToken(userToken.getToken());
			String json = jsonb.toJson(askMsgs);

			return Response.ok().entity(json).build();

	}

}
