package life.genny.fyodor.endpoints.models;

import java.util.List;
import java.util.Set;

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

import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.QuestionUtils;


@Path("questionquestion")
public class QuestionQuestions {
    
    @Inject
    UserToken userToken;

    @Inject
    QuestionUtils questionUtils;

    @Inject
    CacheManager cm;

    @Inject
    Logger log;

    /**
     * Fetch all QuestionQuestions for a group from a given product using a parentCode
     * @param product - product to fetch from
     * @param parentCode - parentCode of the QuestionQuestion group
     * @return <ul>
     *             <li>200 OK with the QuestionQuestions if found.</li>
     *             <li>404 with an associated error message if no QuestionQuestions cannot be found under the parentCode/li>
     *          </ul>
     * 
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/all/{product}/{parentCode}/{childCode}")
    public Response fetchAll(
            @PathParam("product") String product, 
            @PathParam("parentCode") String parentCode) {
                log.debug("[!] call to GET /all/questionquestion/" + product + "/" + parentCode);

                if (userToken == null) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("Not authorized to make this request").build();
                }

                Set<QuestionQuestion> questionQuestions = questionUtils.findQuestionQuestionBySource(product, parentCode);
                if(questionQuestions.isEmpty()) {
                    log.info("Could not find any question questions for: " + product + ":" + parentCode);
                    return Response.status(Response.Status.NOT_FOUND)
                                    .entity("Could not find any question questions with parentCode: " + parentCode + " in product: " + product)
                                    .build();
                }
                log.info("Found " + questionQuestions.size() + " child questions in the group for: " + product + ":" + parentCode);
                return Response.ok(questionQuestions).build();
            }

    /**
     * Fetch a QuestionQuestion from a given product using a parentCode, childCode
     * @param product - product to fetch from
     * @param parentCode - parentCode of the QuestionQuestion
     * @param childCode - childCode of the QuestionQuestion
     * @return <ul>
     *             <li>200 OK with the QuestionQuestions if found.</li>
     *             <li>404 with an associated error message if the QuestionQuestions cannot be found/li>
     *          </ul>
     * 
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{product}/{parentCode}/{childCode}")
    public Response fetch(
            @PathParam("product") String product, 
            @PathParam("parentCode") String parentCode,
            @PathParam("childCode") String childCode) {
                log.debug("[!] call to GET /questionquestion/" + product + "/" + parentCode + "/" + childCode);

                if (userToken == null) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("Not authorized to make this request").build();
                }

                QuestionQuestion question = questionUtils.findQuestionQuestionBySourceAndTarget(product, parentCode, childCode);
                if(question == null) {
                    return Response.status(Response.Status.NOT_FOUND).entity("Could not find question question: " + parentCode + ":" + childCode + " in product " + product).build();
                }
                log.info("Found QQ: " + (question.getParentCode() != null ? question.getParentCode() : "null") + ":" + (question.getChildCode() != null ? question.getChildCode() : "null"));
                return Response.ok(question).build();
            }

    /**
     * Delete a QuestionQuestion from a given product using a parentCode and childCode
     * @param product - product to fetch from
     * @param parentCode - parentCode of the QuestionQuestion
     * @param childCode - childCode of the QuestionQuestion
     * @return 200 OK with the Question if found. 404 with an associated error message
     */
    @DELETE
    @Transactional
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{product}/{parentCode}/{childCode}")
    public Response delete(
        @PathParam("product") String product, 
        @PathParam("parentCode") String parentCode,
        @PathParam("childCode") String childCode) {
            log.debug("[!] call to DELETE /questionquestion/" + product + "/" + parentCode + "/" + childCode);
            // TODO: Need to make a proper authentication check here
            if (userToken == null) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Not authorized to make this request").build();
            }

            int numChanged = cm.removeQuestionQuestion(product, parentCode, childCode);
            if(numChanged > 0) {
                return Response.status(Status.OK).entity("Num affected: " + numChanged).build();
            } else {
                return Response.status(Status.NOT_FOUND).entity("No entities affected").build();
            }
        }
    
    /**
     * Delete a QuestionQuestion from a given product using a parentCode and childCode
     * @param product - product to fetch from
     * @param parentCode - parentCode of the QuestionQuestion
     * @param childCode - childCode of the QuestionQuestion
     * @return 200 OK with the Question if found. 404 with an associated error message
     */
    @DELETE
    @Transactional
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/all/{product}/{parentCode}/")
    public Response deleteAll(
        @PathParam("product") String product, 
        @PathParam("parentCode") String parentCode,
        @PathParam("childCode") String childCode) {
            log.debug("[!] call to DELETE /all/questionquestion/" + product + "/" + parentCode);
            // TODO: Need to make a proper authentication check here
            if (userToken == null) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Not authorized to make this request").build();
            }

            int numChanged = questionUtils.removeAllQuestionQuestionsForSource(product, parentCode);
            if(numChanged > 0) {
                return Response.status(Status.OK).entity("Num affected: " + numChanged).build();
            } else {
                return Response.status(Status.NOT_FOUND).entity("No entities affected").build();
            }
        }
}
