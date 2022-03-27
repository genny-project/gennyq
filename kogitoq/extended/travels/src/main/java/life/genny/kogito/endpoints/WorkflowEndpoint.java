package life.genny.kogito.endpoints;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import life.genny.kogito.utils.KogitoUtils;
import life.genny.qwandaq.models.GennyToken;

@Path("/workflows")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkflowEndpoint {

    private static final Logger log = Logger.getLogger(WorkflowEndpoint.class);

    @Inject
    KogitoUtils kogitoUtils;

    // @Inject
    // JsonWebToken jwtUserToken;

    @OPTIONS
    public Response opt() {
        return Response.ok().build();
    }

    /**
     * A GET request to get the processId for the given internCode and SourceCode
     * for legacy genny
     *
     * @return Kogito processId
     */
    @GET
    // @RolesAllowed({ "service,test" })
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/legacy/processids/{sourcecode}/{interncode}")
    public String getProcessId(@PathParam("sourcecode") String sourceCode, @PathParam("interncode") String internCode) {
        log.info("Getting kogito ProcessId for legacy");

        String processId = null;
        // String token = jwtUserToken.getRawToken();
        // GennyToken userToken = new GennyToken(token);

        String graphQL = "query {  Application (where: {      internCode: { like: \"" + internCode
                + "\" }, sourceCode: { like: \"" + sourceCode + "\" }, newApplication: { equal: true}}) {   id  }}";
        log.warn("Getting kogito ProcessId for legacy - " + graphQL);

        Boolean done = false;
        int count = 5;
        while ((!done) && (count > 0)) {

            try {
                processId = kogitoUtils.fetchProcessId("Application", graphQL, null);
                if (!StringUtils.isBlank(processId)) {
                    done = true;
                } else {
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } // Send null token for now
            if (count <= 0) {
                done = true;
            }
        }
        log.info("Returned processId from WorkflowEndpoint is " + processId);
        return processId;
    }

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        log.info("Workflow Endpoint starting");

    }

    @Transactional
    void onShutdown(@Observes ShutdownEvent ev) {
        log.info("Workflow Endpoint Shutting down");
    }
}
