package life.genny.fyodor.endpoints;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.persistence.EntityManager;

import org.jboss.logging.Logger;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.vertx.core.http.HttpServerRequest;

import life.genny.fyodor.utils.SearchUtility;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.message.QScheduleMessage;
import life.genny.qwandaq.message.QSearchBeResult;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;

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
	SearchUtility search;

	@Inject
	UserToken userToken;

	@Inject
	ServiceToken serviceToken;

	Jsonb jsonb = JsonbBuilder.create();

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/api/schedule")
	public Response schedule() {

		String uuid = UUID.randomUUID().toString().toUpperCase();

		log.info("Scheduling test event for " + uuid);

		 if (userToken == null) {
		 	log.error("Bad or no header token in Search POST provided");
		 	return Response.status(Response.Status.BAD_REQUEST).build();
		 }


		log.info("GENNY_TOKEN = " + userToken);
		log.info("SERVICE_TOKEN = " + serviceToken);

		BaseEntity user = beUtils.getUserBaseEntity();

		new QScheduleMessage.Builder("SCHEDULE_TEST")
			.setEventMessage("TEST_EVENT", uuid)
			.setTriggerTime(LocalDateTime.now(ZoneId.of("UTC")).plusSeconds(5))
			.setGennyToken(userToken)
			.schedule();

		log.info("Done!");

		return Response.ok().build();
	}

	/**
	 * A POST request for search results based on a 
	 * {@link SearchEntity}. Will only fetch codes.
	 *
	 * @return Success
	 */
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/api/search")
	public Response search(SearchEntity searchEntity) {

		log.info("Search POST received..");

		if (userToken == null) {
			log.error("Bad or no header token in Search POST provided");
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		// Process search
		QSearchBeResult results = search.findBySearch25(searchEntity, false, false);
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
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/api/search/fetch")
	public Response fetch(SearchEntity searchEntity) {

		log.info("Fetch POST received..");

		if (userToken == null) {
			log.error("Bad or no header token in Search POST provided");
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		// Process search
		QSearchBeResult results = search.findBySearch25(searchEntity, false, true);
		log.info("Found " + results.getTotal() + " results!");

		String json = jsonb.toJson(results);
		return Response.ok().entity(json).build();
	}
}
