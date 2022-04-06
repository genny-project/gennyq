
package org.acme.travels.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;

import org.jboss.logging.Logger;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.message.QCmdMessage;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.KafkaUtils;
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

    public void sendQDataAskMessageJson(final String askMsgJson) {
        KafkaUtils.writeMsg("webcmds", askMsgJson);
    }

    public void sendQDataAskMessage(final QDataAskMessage askMsg) {
        KafkaUtils.writeMsg("webcmds", askMsg);
    }

    public void sendQDataAskMessage2(final QDataAskMessage msg, final String targetCode, final String userTokenStr) {
        GennyToken userToken = new GennyToken(userTokenStr);
        BaseEntity targetBE = entityManager.createQuery(
                "SELECT u from BaseEntity u WHERE u.code = :code  and u.realm = :realm", BaseEntity.class)
                .setParameter("code", targetCode)
                .setParameter("realm", userToken.getRealm())
                .getSingleResult();
        questionUtils.sendQuestions(msg, targetBE, userToken);
    }

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
        log.info("getting QDataAskMessage using Service!  : " + questionGroupCode + ":" + sourceBE.getCode()
                + ":" + targetBE.getCode());
        GennyToken userToken = new GennyToken(userTokenStr);
        BaseEntityUtils beUtils = new BaseEntityUtils(service.getServiceToken(), userToken);

        Question rootQuestion = questionUtils.getQuestion(questionGroupCode, beUtils);

        // test with testuser and testuser

        List<Ask> asks = questionUtils.findAsks2(rootQuestion, sourceBE, targetBE, beUtils);

        QDataAskMessage msg = new QDataAskMessage(asks.toArray(new Ask[0]));
        msg.setToken(beUtils.getGennyToken().getToken());
        log.info("QDataAskMessage--->" + msg);
        return msg;
    }

    public void sendBaseEntity(final String beCode, final QDataAskMessage qDataAskMessage) {

        String userTokenStr = qDataAskMessage.getToken();
        GennyToken userToken = new GennyToken(userTokenStr);
        BaseEntityUtils beUtils = new BaseEntityUtils(service.getServiceToken(),
                userToken);

        // only send the attribute values that are in the questions
        BaseEntity be = beUtils.getBaseEntityByCode(beCode);
        Set<String> allowedAttributeCodes = new HashSet<>();
        for (Ask ask : qDataAskMessage.getItems()) {
            allowedAttributeCodes.add(ask.getAttributeCode());
        }

        Set<EntityAttribute> eas = ConcurrentHashMap.newKeySet(be.getBaseEntityAttributes().size());
        for (EntityAttribute ea : be.getBaseEntityAttributes()) {
            eas.add(ea);
        }
        // Now delete any attribute that is not in the allowed Set
        for (EntityAttribute ea : eas) {
            if (!allowedAttributeCodes.contains(ea.getAttributeCode())) {
                be.removeAttribute(ea.getAttributeCode());
            }
        }

        // // Send to front end
        QDataBaseEntityMessage msg = new QDataBaseEntityMessage(be);
        msg.setToken(qDataAskMessage.getToken());
        msg.setReplace(true);
        KafkaUtils.writeMsg("webcmds", msg);

    }

    public String getAsksJson(final String questionGroupCode, final String sourceCode, final String targetCode,
            final String userTokenStr) {
        QDataAskMessage askMsg = getAsks(questionGroupCode, sourceCode, targetCode, userTokenStr);
        String askMsgJson = jsonb.toJson(askMsg);
        return askMsgJson;
    }

    public QDataAskMessage getAsks(final String questionGroupCode, final String sourceCode, final String targetCode,
            final String userTokenStr) {

        GennyToken userToken = new GennyToken(userTokenStr);
        log.info("Getting Asks using Service! " + userToken.getUsername() + " : " + questionGroupCode + ":" + sourceCode
                + ":" + targetCode);
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

        QDataAskMessage askMsg = getQDataAskMessage(userTokenStr,
                questionGroupCode,
                sourceBE,
                targetBE);

        return askMsg;
    }

    public void sendPCM(final String pcmCode, final String userTokenStr) {
        log.info("Sending PCM -> " + pcmCode);
        if (pcmCode.startsWith("PCM")) {
            QCmdMessage msg = new QCmdMessage("DISPLAY", "FORM");
            msg.setToken(userTokenStr);
            KafkaUtils.writeMsg("webcmds", msg);
        } else { // Old school
            // split by :
            String[] displayParms = pcmCode.split(":");
            QCmdMessage msg = new QCmdMessage(displayParms[0], displayParms[1]);
            msg.setToken(userTokenStr);
            KafkaUtils.writeMsg("webcmds", msg);
        }
    }

    public void sendQuestions(QDataAskMessage askMsg, BaseEntity target, GennyToken userToken) {

        log.info("AskMsg=" + askMsg);

        QCmdMessage msg = new QCmdMessage("DISPLAY", "FORM");
        msg.setToken(userToken.getToken());

        KafkaUtils.writeMsg("webcmds", msg);

        QDataBaseEntityMessage beMsg = new QDataBaseEntityMessage(target);
        beMsg.setToken(userToken.getToken());

        KafkaUtils.writeMsg("webcmds", beMsg); // should be webdata

        askMsg.setToken(userToken.getToken());
        KafkaUtils.writeMsg("webcmds", askMsg);

        QCmdMessage msgend = new QCmdMessage("END_PROCESS", "END_PROCESS");
        msgend.setToken(userToken.getToken());
        msgend.setSend(true);
        KafkaUtils.writeMsg("webcmds", msgend);
    }
}
