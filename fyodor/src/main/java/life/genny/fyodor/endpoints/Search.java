package life.genny.fyodor.endpoints;

import java.util.List;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.smallrye.mutiny.tuples.Tuple2;
import io.vertx.core.http.HttpServerRequest;
import life.genny.fyodor.utils.FyodorUltra;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.message.QSearchBeResult;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.HttpUtils;

/**
 * Search - Endpoints providing classic Genny Search functionality
 */
@Path("/")
public class Search {

	private static final Logger log = Logger.getLogger(Search.class);

	@ConfigProperty(name = "genny.keycloak.url", defaultValue = "https://keycloak.gada.io")
	String baseKeycloakUrl;

	@Context
	HttpServerRequest request;

	@Inject
	EntityManager entityManager;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	FyodorUltra fyodor;

	@Inject
	UserToken userToken;

	@Inject
	ServiceToken serviceToken;

	Jsonb jsonb = JsonbBuilder.create();

	/**
	 * A POST request for search results based on a 
	 * {@link SearchEntity}. Will only fetch codes.
	 *
	 * @return Success
	 */
	@POST
	@Path("/api/search")
	@Produces(MediaType.APPLICATION_JSON)
	public Response search(SearchEntity searchEntity) {

		log.info("Search POST received..");

		if (userToken == null) {
			log.error("Bad or no header token in Search POST provided");
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		// Process search
		Tuple2<List<String>, Long> tpl = null;
		try {
			tpl = fyodor.search26(searchEntity);
		} catch (ItemNotFoundException e) {
			return Response.serverError().entity(HttpUtils.error(e.getMessage())).build();
		}
		QSearchBeResult results = new QSearchBeResult(tpl.getItem1(), tpl.getItem2());
		log.info("Found " + results.getTotal() + " results!");

		String json = jsonb.toJson(results);
		return Response.ok().entity(json).build();
	}

	/**
	 * 
	 * A POST request for search results based on a 
	 * {@link SearchEntity}. Will fetch complete entities.
	 *
	 * @return Success
	 */
	@POST
	@Path("/api/search/fetch")
	@Produces(MediaType.APPLICATION_JSON)
	public Response fetch(SearchEntity searchEntity) {

		log.info("Fetch POST received..");

		if (userToken == null) {
			log.error("Bad or no header token in Search POST provided");
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		// Process search
		Tuple2<List<BaseEntity>, Long> tpl = null;
		try {
			tpl = fyodor.fetch26(searchEntity);
		} catch (ItemNotFoundException e) {
			return Response.serverError().entity(HttpUtils.error(e.getMessage())).build();
		}
		QSearchBeResult results = new QSearchBeResult(tpl.getItem1().toArray(BaseEntity[]::new), tpl.getItem2());
		log.info("Found " + results.getTotal() + " results!");

		String json = jsonb.toJson(results);
		return Response.ok().entity(json).build();
	}

	@POST
	@Path("/count25")
	@Produces(MediaType.APPLICATION_JSON)
	public String count25(SearchEntity searchEntity) {
		// TODO: Remove this endpoint
		return count(searchEntity);
	}

	@POST
	@Path("/api/search/count")
	@Produces(MediaType.APPLICATION_JSON)
	public String count(SearchEntity searchEntity) {

		if (userToken == null) {
			log.error("Bad or no header token in Search POST provided");
			return "0";
		}

		Tuple2<List<String>, Long> tpl = null;
		try {
			tpl = fyodor.search26(searchEntity);
		} catch (ItemNotFoundException e) {
			return HttpUtils.error(e.getMessage());
		}

		Long count = tpl.getItem2();
		log.infof("Found %s entities", count);

		return ""+count;
	}

	@POST
	@Path("/api/wildcard/{wildcard}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response fetch(@PathParam("wildcard") String wildcard) {

		log.info("Fetch POST received..");

		if (userToken == null) {
			log.error("Bad or no header token in Search POST provided");
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		SearchEntity searchEntity = new SearchEntity("SBE_WILDCARD", "Wildcard")
			.add(new Filter("PRI_CODE", Operator.LIKE, "PER_%"))
			.setWildcard(wildcard)
			.setPageSize(100)
			.setRealm(userToken.getProductCode());

		// Process search
		Tuple2<List<BaseEntity>, Long> tpl = null;
		try {
			tpl = fyodor.fetch26(searchEntity);
		} catch (ItemNotFoundException e) {
			return Response.serverError().entity(HttpUtils.error(e.getMessage())).build();
		}
		log.info("Found " + tpl.getItem2() + " results!");

		String json = jsonb.toJson(tpl);
		return Response.ok().entity(json).build();
	}

}
