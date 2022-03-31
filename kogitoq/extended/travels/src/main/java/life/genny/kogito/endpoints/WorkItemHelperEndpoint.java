package life.genny.kogito.endpoints;

import java.util.List;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.utils.DatabaseUtils;

@Path("/workitemhelper")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkItemHelperEndpoint {

    private static final Logger log = Logger.getLogger(WorkItemHelperEndpoint.class);

    @Inject
    DatabaseUtils databaseUtils;

    @Inject
    EntityManager entityManager;

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
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/questions")
    public QDataAskMessage getQuestions() {
        log.info("Got to getQuestions API");

        List<QuestionQuestion> qqs = getQuestionQuestions("internmatch", "QUE_ADMIN_GRP");

        log.info("qq:" + qqs);

        for (QuestionQuestion qq : qqs) {
            log.info("QQ:" + qq + " --> " + qq.getWeight());
        }

        // Now we need to build the ask

        return null;
    }

    List<QuestionQuestion> getQuestionQuestions(final String realm, final String sourceCode) {
        List<QuestionQuestion> qqs = null;
        try {

            qqs = entityManager
                    .createQuery(
                            "FROM QuestionQuestion WHERE realm=:realmStr AND sourceCode = :sourceCode order by weight ASC",
                            QuestionQuestion.class)
                    .setParameter("realmStr", realm)
                    .setParameter("sourceCode", sourceCode)
                    .getResultList();

        } catch (NoResultException e) {
            log.error("No QuestionQuestion found in DB for " + sourceCode);
        }
        // List<QuestionQuestion> qqs =
        // databaseUtils.findQuestionQuestionsBySourceCode(realm, sourceCode);

        log.info("qq:" + qqs);

        for (QuestionQuestion qq : qqs) {
            log.info("QQ:" + qq + " --> " + qq.getWeight());
            if (qq.getPk().getTargetCode().endsWith("GRP")) {
                // This is a directory
                List<QuestionQuestion> childQQs = getQuestionQuestions(realm, qq.getPk().getTargetCode());
                // Update their sorting
                childQQs.stream().forEach(u -> u.setWeight(qq.getWeight() + (u.getWeight() / 10)));
                qqs.addAll(childQQs);
            }
        }

        return qqs;
    }

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        log.info("WorkItemHelper Endpoint starting");

    }

    @Transactional
    void onShutdown(@Observes ShutdownEvent ev) {
        log.info("WorkItemHelper Endpoint Shutting down");
    }
}
