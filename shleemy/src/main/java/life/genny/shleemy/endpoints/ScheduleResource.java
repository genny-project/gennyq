package life.genny.shleemy.endpoints;

import java.net.URI;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.quartz.SchedulerException;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.security.identity.SecurityIdentity;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.message.QScheduleMessage;
import life.genny.shleemy.quartz.TaskBean;

@Path("/api/schedule")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ScheduleResource {

	private static final Logger log = Logger.getLogger(ScheduleResource.class);

	@ConfigProperty(name = "default.realm", defaultValue = "genny")
	String defaultRealm;

	@Inject
	JsonWebToken accessToken;

	@Inject
	TaskBean taskBean;

	void onStart(@Observes StartupEvent ev) {
		log.info("ScheduleResource Endpoint starting");
	}

	void onShutdown(@Observes ShutdownEvent ev) {
		log.info("ScheduleResource Endpoint Shutting down");
	}

	@POST
	@Transactional
	public Response scheduleMessage(@Context UriInfo uriInfo, @Valid QScheduleMessage scheduleMessage) {

		GennyToken userToken = new GennyToken(accessToken.getRawToken());
		log.info("User is " + userToken.getEmail());

		try {
			taskBean.addSchedule(scheduleMessage, userToken);

			URI uri = uriInfo.getAbsolutePathBuilder().path(ScheduleResource.class, "findById")
					.build(scheduleMessage.id);

			return Response.created(uri).entity(scheduleMessage.code).build();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.status(Status.BAD_REQUEST).entity("ScheduleMessage did not schedule").build();
	}

	@GET
	@Path("/code/{code}")
	public Response findByCode(@PathParam("code") final String code) {

		GennyToken userToken = new GennyToken(accessToken.getRawToken());
		log.info("User is " + userToken.getEmail());

		QScheduleMessage scheduleMessage = QScheduleMessage.findByCode(code);
		if (scheduleMessage == null) {
			return Response.status(Status.NOT_FOUND).entity("ScheduleMessage with code of " + code + " does not exist.")
					.build();
		}
		if (scheduleMessage.realm != userToken.getRealm()) {
			return Response.status(Status.NOT_FOUND).entity("ScheduleMessage with bad realm")
					.build();
		}

		return Response.status(Status.OK).entity(scheduleMessage).build();
	}
	
	@GET
	@Path("/id/{id}")
	public Response findById(@PathParam("id") final Long id) {

		GennyToken userToken = new GennyToken(accessToken.getRawToken());
		log.info("User is " + userToken.getEmail());

		QScheduleMessage scheduleMessage = QScheduleMessage.findById(id);
		if (scheduleMessage == null) {
			return Response.status(Status.NOT_FOUND).entity("ScheduleMessage with id of " + id + " does not exist.")
					.build();
		}
		if (scheduleMessage.realm != userToken.getRealm()) {
			return Response.status(Status.NOT_FOUND).entity("ScheduleMessage with id of " + id + " does not exist.")
					.build();
		}

		return Response.status(Status.OK).entity(scheduleMessage).build();
	}

	@DELETE
	@Path("/{id}")
	@Transactional
	public Response deleteSchedule(@PathParam("id") final Long id) {

		GennyToken userToken = new GennyToken(accessToken.getRawToken());
		log.info("User is " + userToken.getEmail());

		QScheduleMessage scheduleMessage = QScheduleMessage.findById(id);
		if (scheduleMessage == null) {
			return Response.status(Status.NOT_FOUND).entity("ScheduleMessage with id of " + id + " does not exist.")
					.build();
		}

		if (scheduleMessage.realm != userToken.getRealm()) {
			return Response.status(Status.NOT_FOUND).entity("ScheduleMessage with id of " + id + " does not exist.")
					.build();
		}

		if (!(userToken.hasRole("admin")) && !(userToken.getUserCode().equals(scheduleMessage.sourceCode))) {
			return Response.status(Status.FORBIDDEN)
					.entity("ScheduleMessage with id of " + id + " cannot be deleted by this user.").build();
		}

		// find msg using id
		QScheduleMessage msg = QScheduleMessage.findById(id);
		if (msg == null) {
			return Response.status(Status.NOT_FOUND).build();
		}

		// attempt to abort schedule
		try {
			taskBean.abortSchedule(msg.code, userToken);

		} catch (org.quartz.SchedulerException e) {
			log.error(e.getMessage());
			e.printStackTrace();

			return Response.status(Status.NOT_FOUND).build();
		}

		// delete schedule and return ok status
		QScheduleMessage.deleteByCode(msg.code);
		return Response.status(Status.OK).build();
	}

	@Transactional
	@DELETE
	@Path("/code/{code}")
	public Response deleteSchedule(@PathParam("code") final String code) {

		GennyToken userToken = new GennyToken(accessToken.getRawToken());
		log.info("User is " + userToken.getEmail());

		QScheduleMessage scheduleMessage = QScheduleMessage.findByCode(code);
		if (scheduleMessage == null) {
			return Response.status(Status.NOT_FOUND).entity("ScheduleMessage with code of " + code + " does not exist.")
					.build();
		}

		if (scheduleMessage.realm != userToken.getRealm()) {
			return Response.status(Status.NOT_FOUND).entity("ScheduleMessage has a different realm to user.").build();
		}

		if (!(userToken.hasRole("admin")) && !(userToken.getUserCode().equals(scheduleMessage.sourceCode))) {
			return Response.status(Status.FORBIDDEN)
					.entity("ScheduleMessage with code of " + code + " cannot be deleted by this user.").build();
		}

		// attempt to abort schedule
		try {
			taskBean.abortSchedule(code, userToken);

		} catch (org.quartz.SchedulerException e) {
			log.error(e.getMessage());
			e.printStackTrace();

			return Response.status(Status.NOT_FOUND).build();
		}

		// delete schedule and return ok status
		QScheduleMessage.deleteByCode(code);
		return Response.status(Status.OK).build();
	}

}
