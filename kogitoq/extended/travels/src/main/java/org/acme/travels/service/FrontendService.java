
package org.acme.travels.service;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;

import org.jboss.logging.Logger;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.QuestionUtils;
import life.genny.serviceq.Service;

@ApplicationScoped
public class FrontendService {

    private static final Logger log = Logger.getLogger(FrontendService.class);

    Jsonb jsonb = JsonbBuilder.create();
    @Inject
    QuestionUtils questionUtils;

    @Inject
    DatabaseUtils databaseUtils;

    @Inject
    EntityManager entityManager;

    @Inject
    Service service;

    public void sendQuestions(final String questionCode, final String sourceCode,
            final String targetCode, final String userTokenStr) {

        GennyToken userToken = new GennyToken(userTokenStr); // work out how to pass the userToken directly
        log.info("Sending Question using Service! " + userToken.getUsername() + " : " + questionCode + ":" + sourceCode
                + ":" + targetCode);

        QDataAskMessage msg = null;

        log.info("ServiceToken = " + service.getServiceToken().getToken());

        BaseEntityUtils beUtils = new BaseEntityUtils(service.getServiceToken(), userToken);

        BaseEntity source = null;
        BaseEntity target = null;

        // source = beUtils.getBaseEntityByCode(sourceCode);
        // target = beUtils.getBaseEntityByCode(targetCode);

        source = entityManager.createQuery(
                "SELECT u from BaseEntity u WHERE u.code = :code  and u.realm = :realm", BaseEntity.class)
                .setParameter("code", sourceCode)
                .setParameter("realm", userToken.getRealm())
                .getSingleResult();

        target = entityManager.createQuery(
                "SELECT u from BaseEntity u WHERE u.code = :code  and u.realm = :realm", BaseEntity.class)
                .setParameter("code", targetCode)
                .setParameter("realm", userToken.getRealm())
                .getSingleResult();

        if (source == null) {
            log.error("Source BE not found for original " + sourceCode);
            source = beUtils.getBaseEntityByCode(beUtils.getGennyToken().getUserCode());
            if (source == null) {
                log.error("Source BE not found for userToken sourceCode" + beUtils.getGennyToken().getUserCode());
                // return null; // run exception
            }
        }

        log.info("usercode = " + userToken.getUserCode() + " usernamer=[" + userToken.getUsername() + "]");

        // Fetch the Asks

        msg = this.getQDataAskMessage(userTokenStr, questionCode, source, target);

        questionUtils.sendQuestions(msg, target, userToken);

        // return msg;

    }

    public QDataAskMessage getQDataAskMessage(final String userTokenStr,
            String questionGroupCode,
            BaseEntity sourceBE,
            BaseEntity targetBE) {
        log.info("Got to getQuestions API");
        log.info("Sending Question using Service!  : " + questionGroupCode + ":" + sourceBE.getCode()
                + ":" + targetBE.getCode());
        GennyToken userToken = new GennyToken(userTokenStr);
        BaseEntityUtils beUtils = new BaseEntityUtils(service.getServiceToken(), userToken);

        Question rootQuestion = questionUtils.getQuestion(questionGroupCode, beUtils);

        // test with testuser and testuser

        List<Ask> asks = questionUtils.findAsks2(rootQuestion, sourceBE, targetBE, beUtils);

        QDataAskMessage msg = new QDataAskMessage(asks.toArray(new Ask[0]));
        msg.setToken(beUtils.getGennyToken().getToken());

        return msg;
    }

    public void sendBaseEntity(final String beCode, final QDataAskMessage qDataAskMessage) {

        // String userTokenStr = qDataAskMessage.getToken();
        // GennyToken userToken = new GennyToken(userTokenStr);
        // BaseEntityUtils beUtils = new BaseEntityUtils(service.getServiceToken(),
        // userToken);

        // // only send the attribute values that are in the questions
        // BaseEntity be = beUtils.getBaseEntityByCode(beCode);
        // Set<String> allowedAttributeCodes = new HashSet<>();
        // for (Ask ask : qDataAskMessage.getItems()) {
        // allowedAttributeCodes.add(ask.getAttributeCode());
        // }
        // // Now delete any attribute that is not in the allowed Set
        // for (EntityAttribute ea : be.getBaseEntityAttributes()) {
        // if (!allowedAttributeCodes.contains(ea.getAttributeCode())) {
        // be.removeAttribute(ea.getAttributeCode())
        // }
        // }

        // // Send to front end
        // KafkaUtils.writeMsg("webcmds", be);

    }

    public QDataAskMessage getAsks(final String questionGroupCode, final String sourceCode, final String targetCode,
            final String userTokenStr) {
        GennyToken userToken = new GennyToken(userTokenStr);

        BaseEntity sourceBE = null;
        BaseEntity targetBE = null;

        sourceBE = entityManager.createQuery(
                "SELECT u from BaseEntity u WHERE u.code = :code and u.realm = :realm", BaseEntity.class)
                .setParameter("code", sourceCode)
                .setParameter("realm", userToken.getRealm())
                .getSingleResult();

        targetBE = entityManager.createQuery(
                "SELECT u from BaseEntity u WHERE u.code = :code  and u.realm = :realm", BaseEntity.class)
                .setParameter("code", targetCode)
                .setParameter("realm", userToken.getRealm())
                .getSingleResult();

        return getQDataAskMessage(userTokenStr,
                questionGroupCode,
                sourceBE,
                targetBE);
    }
}
