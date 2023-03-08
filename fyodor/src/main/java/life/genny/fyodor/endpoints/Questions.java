package life.genny.fyodor.endpoints;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;

import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.EntityAttributeUtils;
import life.genny.qwandaq.utils.QuestionUtils;

@Path("question")
public class Questions {
    
    @Inject
    UserToken userToken;

    @Inject
    CacheManager cm;

    @Inject
    Logger log;
    /**
     * Fetch a Question from a given product using a code
     * @param product - product to fetch from
     * @param code - code of the Question
     * @return <ul>
     *             <li>200 OK with the QuestionQuestions if found.</li>
     *             <li>404 with an associated error message if the QuestionQuestions cannot be found/li>
     *          </ul>
     * 
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{product}/{code}")
    public Response fetch(
            @PathParam("product") String product,
            @PathParam("code") String code) {
                log.debug("[!] call to GET /question/" + product + "/" + code);

                if (userToken == null) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("Not authorized to make this request").build();
                }

                try {
                    Question question = cm.getQuestion(product, code);
                    log.info("Found Question: " + (question.getCode() != null ? question.getCode() : "null"));
                    return Response.ok(question).build();
                } catch(ItemNotFoundException e) {
                    return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
                }
            }

    /**
     * Delete a Question from a given product using a code
     * @param product - product to fetch from
     * @param code - code of the Question
     * @return 200 OK with the Question if found. 404 with an associated error message
     */
    @DELETE
    @Transactional
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{product}/{code}")
    public Response delete(
        @PathParam("product") String product, 
        @PathParam("code") String code) {
            log.debug("[!] call to DELETE /question/" + product + "/" + code);
            // TODO: Need to make a proper authentication check here
            if (userToken == null) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Not authorized to make this request").build();
            }

            int numChanged = cm.removeQuestion(product, code);
            if(numChanged > 0) {
                return Response.status(Status.OK).entity("Num affected: " + numChanged).build();
            } else {
                return Response.status(Status.NOT_FOUND).entity("No entities affected").build();
            }
        }
    
}
