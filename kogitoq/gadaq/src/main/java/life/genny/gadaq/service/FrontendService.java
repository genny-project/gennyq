package life.genny.gadaq.service;

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
import life.genny.qwandaq.attribute.Attribute;
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
import life.genny.qwandaq.utils.QwandaUtils;
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
    QwandaUtils qwandaUtils;

    @Inject
    EntityManager entityManager;

    @Inject
    Service service;

    public void sendQDataAskMessageJson(final String askMsgJson) {
        log.info("Entering sendQDataAskMessageJson");
        KafkaUtils.writeMsg("webcmds", askMsgJson);
        log.info("Exiting sendQDataAskMessageJson");
    }

    public void sendQDataAskMessage(final QDataAskMessage askMsg) {
        log.info("Entering sendQDataAskMessage");
        KafkaUtils.writeMsg("webcmds", askMsg);
        log.info("Leaving sendQDataAskMessage");
    }

    // public void sendQDataAskMessage2(final QDataAskMessage msg, final String
    // targetCode, final String userTokenStr) {
    // GennyToken userToken = new GennyToken(userTokenStr);
    // BaseEntity targetBE = entityManager.createQuery(
    // "SELECT u from BaseEntity u WHERE u.code = :code and u.realm = :realm",
    // BaseEntity.class)
    // .setParameter("code", targetCode)
    // .setParameter("realm", userToken.getRealm())
    // .getSingleResult();
    // questionUtils.sendQuestions(msg, targetBE, userToken);
    // }

    public void sendQuestions(final String questionCode, final String sourceCode,
            final String targetCode, final String userTokenStr) {
        log.info("Entering sendQuestions");
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
        log.info("Leaving sendQuestions");
        // return msg;

    }

    public String setupProcessBEJson(final String targetCode, BaseEntity processBE,
            final String qDataAskMessageJson) {
        log.info("Updating processBEJson with latest Target");
        // BaseEntity processBE = jsonb.fromJson(processBEJson, BaseEntity.class);

        processBE = setupProcessBE(targetCode, processBE, qDataAskMessageJson);
        String processBEJson = jsonb.toJson(processBE);
        return processBEJson;
    }

    public BaseEntity setupProcessBE(final String targetCode, final BaseEntity processBE,
            final String qDataAskMessageJson) {
        log.info("Updating processBE with latest Target");

        BaseEntityUtils beUtils = new BaseEntityUtils(service.getServiceToken());

        // Force the realm
        processBE.setRealm(service.getServiceToken().getRealm());

        // only copy the entityAttributes used in the Asks
        BaseEntity target = beUtils.getBaseEntityByCode(targetCode);
        Set<String> allowedAttributeCodes = new HashSet<>();
        QDataAskMessage qDataAskMessage = jsonb.fromJson(qDataAskMessageJson, QDataAskMessage.class);
        // FIX TODO

        for (Ask ask : qDataAskMessage.getItems()) {
            allowedAttributeCodes.add(ask.getAttributeCode());
            if ((ask.getChildAsks() != null) && (ask.getChildAsks().length > 0)) {
                // dumb single level
                for (Ask childAsk : ask.getChildAsks()) {
                    if ((childAsk.getChildAsks() != null) && (childAsk.getChildAsks().length > 0)) {
                        for (Ask grandChildAsk : childAsk.getChildAsks()) {
                            allowedAttributeCodes.add(grandChildAsk.getAttributeCode());
                        }
                    } else {
                        allowedAttributeCodes.add(childAsk.getAttributeCode());
                    }
                }
            }
        }
        log.info("Number of Asks = " + allowedAttributeCodes.size());
        for (String attributeCode : allowedAttributeCodes) {
            EntityAttribute ea = target.findEntityAttribute(attributeCode).orElse(null);
            if (ea == null) {
                Attribute attribute = qwandaUtils.getAttribute(attributeCode);
                ea = new EntityAttribute(processBE, attribute, 1.0, null);
            }
            processBE.getBaseEntityAttributes().add(ea);
        }
        log.info("Number of BE eas = " + processBE.getBaseEntityAttributes().size());

        log.info("Leaving updateProcessBE");
        return processBE;
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

        List<Ask> asks = questionUtils.findAsks(rootQuestion, sourceBE, targetBE, beUtils);

        QDataAskMessage msg = new QDataAskMessage(asks.toArray(new Ask[0]));
        msg.setToken(beUtils.getGennyToken().getToken());
        // log.info("QDataAskMessage--->" + msg);
        return msg;
    }

    public void sendBaseEntityJson(final String beCode, final String qDataAskMessageJson) {
        QDataAskMessage askMsg = jsonb.fromJson(qDataAskMessageJson, QDataAskMessage.class);
        sendBaseEntity(beCode, askMsg);
    }

    public void sendBaseEntity(final String beCode, final QDataAskMessage qDataAskMessage) {
        log.info("Entering sendBaseEntity");
        String userTokenStr = qDataAskMessage.getToken();
        GennyToken userToken = new GennyToken(userTokenStr);
        BaseEntityUtils beUtils = new BaseEntityUtils(service.getServiceToken(),
                userToken);

        // only send the attribute values that are in the questions
        BaseEntity be = beUtils.getBaseEntityByCode(beCode);
        Set<String> allowedAttributeCodes = new HashSet<>();
        // FIX TODO
        for (Ask ask : qDataAskMessage.getItems()) {
            allowedAttributeCodes.add(ask.getAttributeCode());
            if ((ask.getChildAsks() != null) && (ask.getChildAsks().length > 0)) {
                // dumb single level
                for (Ask childAsk : ask.getChildAsks()) {
                    if ((childAsk.getChildAsks() != null) && (childAsk.getChildAsks().length > 0)) {
                        for (Ask grandChildAsk : childAsk.getChildAsks()) {
                            allowedAttributeCodes.add(grandChildAsk.getAttributeCode());
                        }
                    } else {
                        allowedAttributeCodes.add(childAsk.getAttributeCode());
                    }
                }
            }
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
        log.info("Leaving sendBaseEntity");
    }

    public String getAsksJson(final String questionGroupCode, final String sourceCode, final String targetCode,
            final String userTokenStr, final String processId) {
        log.info("Entering getAsksJson");
        QDataAskMessage askMsg = getAsks(questionGroupCode, sourceCode, targetCode, userTokenStr, processId);
        log.info("About to json QDataAskMessage");

        String askMsgJson = jsonb.toJson(askMsg);
        askMsg = jsonb.fromJson(askMsgJson, QDataAskMessage.class);
        askMsg.setToken(userTokenStr);
        log.info("Leaving getAsksJson");
        return askMsgJson;
    }

    public QDataAskMessage getAsks(final String questionGroupCode, final String sourceCode, final String targetCode,
            final String userTokenStr, final String processId) {
        log.info("Entering getAsks with ");
        log.info("questionGroupCode :" + questionGroupCode);
        log.info("sourceCode :" + sourceCode);
        log.info("targetCode :" + targetCode);
        log.info("userTokenStr :" + userTokenStr);
        log.info("processId :" + processId);
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

        for (Ask ask : askMsg.getItems()) {
            ask.setProcessId(processId);
        }

        log.info("Leaving getAsks");
        return askMsg;
    }

    public void sendPCM(final String pcmCode, final String userTokenStr) {
        log.info("Entering sendPCM  PCM -> " + pcmCode);
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
        log.info("Leaving sendPCM");
    }

    public void sendQuestions(QDataAskMessage askMsg, BaseEntity target, GennyToken userToken) {

        log.info("Entering sendQuestions - AskMsg=" + askMsg);

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
        log.info("LEaving sendQuestions ");
    }
}
