package life.genny.kogito.endpoints;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
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
import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.KeycloakUtils;
import life.genny.qwandaq.utils.QuestionUtils;

@Path("/workitemhelper")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkItemHelperEndpoint {

    private static final Logger log = Logger.getLogger(WorkItemHelperEndpoint.class);

    @Inject
    DatabaseUtils databaseUtils;

    @Inject
    EntityManager entityManager;

    @Inject
    QuestionUtils questionUtils;

    Jsonb jsonb = JsonbBuilder.create();

    // @Inject
    // JsonWebToken jwtUserToken;

    @OPTIONS
    public Response opt() {
        return Response.ok().build();
    }

    Question getQuestion(final String realm, final String questionCode) {
        Question question = null;
        try {

            question = entityManager
                    .createQuery(
                            "FROM Question WHERE realm=:realmStr AND code = :code",
                            Question.class)
                    .setParameter("realmStr", realm)
                    .setParameter("code", questionCode)
                    .getSingleResult();

        } catch (NoResultException e) {
            log.error("No Question found in DB for " + questionCode);
        }

        return question;
    }

    /**
     * A GET request to get the processId for the given internCode and SourceCode
     * for legacy genny
     *
     * @return Kogito processId
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/questions/{realm}/{questioncode}/{sourcecode}/{targetcode}")
    public Response getQuestions(@PathParam("realm") String realm,
            @PathParam("questioncode") String questionGroupCode,
            @PathParam("sourcecode") String sourceCode,
            @PathParam("targetcode") String targetCode) {
        log.info("Got to getQuestions API");

        Question rootQuestion = getQuestion(realm, questionGroupCode);

        // test with testuser and testuser

        BaseEntity source = null;
        BaseEntity target = null;
        BaseEntityUtils beUtils = init();

        source = beUtils.getBaseEntityByCode(beUtils.getGennyToken().getUserCode());
        target = source;
        List<Ask> asks = questionUtils.findAsks2(rootQuestion, source, target, beUtils);

        QDataAskMessage msg = new QDataAskMessage(asks.toArray(new Ask[0]));
        // Convert to json

        String ret = null;
        try {
            ret = jsonb.toJson(msg);
            // log.info("ret=" + ret);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
        }
        return Response.ok(msg).build();
    }

    // @Transactional
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

        // log.info("qq:" + qqs);
        List<QuestionQuestion> retQQ = new ArrayList<QuestionQuestion>();
        for (QuestionQuestion qq : qqs) {
            log.info("####" + qq.getPk().getSourceCode() + ":" + qq.getPk().getTargetCode());
        }

        for (QuestionQuestion qq : qqs) {
            if (qq.getPk().getTargetCode().endsWith("_GRP")) {
                // This is a directory
                List<QuestionQuestion> childQQs = getQuestionQuestions(realm, qq.getPk().getTargetCode());
                if (childQQs != null) {
                    // Update their sorting
                    childQQs.stream().forEach(u -> u.setWeight(qq.getWeight() + (u.getWeight() / 10)));
                    if (!childQQs.isEmpty()) {
                        retQQ.addAll(childQQs);
                    }
                }

            } else {
                retQQ.add(qq);
            }
        }

        return retQQ;
    }

    BaseEntityUtils init() {
        GennyToken serviceToken = KeycloakUtils.getToken("https://keycloak.gada.io", "internmatch", "admin-cli",
                null, "service", System.getenv("GENNY_SERVICE_PASSWORD"));
        log.info("ServiceToken = " + serviceToken.getToken());
        GennyToken userToken = null;
        BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken, serviceToken);

        SearchEntity searchBE = new SearchEntity("SBE_TESTUSER", "test user Search")
                .addSort("PRI_CREATED", "Created", SearchEntity.Sort.DESC)
                .addFilter("PRI_CODE", SearchEntity.StringFilter.EQUAL, "PER_086CDF1F-A98F-4E73-9825-0A4CFE2BB943") // testuser
                .addColumn("PRI_CODE", "Code");

        searchBE.setRealm("internmatch");

        searchBE.setPageStart(0);
        Integer pageSize = 1;
        searchBE.setPageSize(pageSize);

        List<BaseEntity> recipients = beUtils.getBaseEntitys(searchBE); // load 100 at a time
        if (recipients != null) {
            BaseEntity recipient = null;
            if (recipients.size() > 0) {

                recipient = recipients.get(0);
                log.info("Recipient = " + recipient.getCode());

            }

            Jsonb jsonb = JsonbBuilder.create();

            String userCode = recipient.getCode();
            String username = "testuser@gada.io";
            log.info("usercode = " + userCode + " usernamer=[" + username + "]");
            String userTokenStr = (String) CacheUtils.readCache("internmatch", "TOKEN:" + userCode);
            log.info("fetching from internmatch cache [" + "TOKEN:" + userCode + "]");
            if (StringUtils.isBlank(userTokenStr)) {
                log.error("Make sure you log into your target genny system as testuser@gada.io ");
            } else {

                log.info("usercode = " + userCode + " CacheJsonStr=[" + userTokenStr + "]");
                userToken = new GennyToken("userToken", userTokenStr);
                log.info(
                        "User " + username + " is logged in! " + userToken.getAdecodedTokenMap().get("session_state"));

            }

        } else {
            log.error("No recipients matched search");
        }

        beUtils = new BaseEntityUtils(serviceToken, userToken);
        return beUtils;
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
