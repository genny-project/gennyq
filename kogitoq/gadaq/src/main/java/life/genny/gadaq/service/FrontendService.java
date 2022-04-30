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
import life.genny.qwandaq.models.TokenCollection;
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
    BaseEntityUtils beUtils;

	@Inject
	TokenCollection tokens;

    @Inject
    EntityManager entityManager;

    @Inject
    Service service;

    public void sendQDataAskMessage(final QDataAskMessage askMsg) {
        KafkaUtils.writeMsg("webcmds", askMsg);
    }

    public void sendQuestions(final String questionCode, final String sourceCode, final String targetCode) {

        log.info("Sending Question using Service! : " + questionCode + ":" + sourceCode + ":" + targetCode);

        QDataAskMessage msg = null;

        BaseEntity source = null;
        BaseEntity target = null;

        source = beUtils.getBaseEntityByCode(sourceCode);
        target = beUtils.getBaseEntityByCode(targetCode);

        if (source == null) {
            log.error("Source BE not found for original " + sourceCode);
            source = beUtils.getUserBaseEntity();
        }

        // Fetch the Asks
        msg = getQDataAskMessage(questionCode, source, target);

        questionUtils.sendQuestions(msg, target);
    }

    public BaseEntity setupProcessBE(final String targetCode, final BaseEntity processBE, QDataAskMessage qDataAskMessage) {

        // Force the realm
        processBE.setRealm(tokens.getGennyToken().getProductCode());

        // only copy the entityAttributes used in the Asks
        BaseEntity target = beUtils.getBaseEntityByCode(targetCode);
        Set<String> allowedAttributeCodes = new HashSet<>();

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

    public QDataAskMessage getQDataAskMessage(String questionGroupCode, BaseEntity sourceBE, BaseEntity targetBE) {

        log.info("getting QDataAskMessage using Service!  : " + questionGroupCode + ":" + sourceBE.getCode()
                + ":" + targetBE.getCode());

        Question rootQuestion = questionUtils.getQuestion(questionGroupCode);

        List<Ask> asks = questionUtils.findAsks(rootQuestion, sourceBE, targetBE);

        QDataAskMessage msg = new QDataAskMessage(asks.toArray(new Ask[0]));
        msg.setToken(tokens.getGennyToken().getToken());

        return msg;
    }

    public void sendBaseEntityJson(final String beCode, final String qDataAskMessageJson) {

        QDataAskMessage askMsg = jsonb.fromJson(qDataAskMessageJson, QDataAskMessage.class);
        sendBaseEntity(beCode, askMsg);
    }

    public void sendBaseEntity(final String beCode, final QDataAskMessage qDataAskMessage) {

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
        msg.setToken(tokens.getGennyToken().getToken());
        msg.setReplace(true);
        KafkaUtils.writeMsg("webcmds", msg);
    }

    public QDataAskMessage getAsks(final String questionGroupCode, final String sourceCode, final String targetCode,
            final String processId) {

        log.info("questionGroupCode :" + questionGroupCode);
        log.info("sourceCode :" + sourceCode);
        log.info("targetCode :" + targetCode);
        log.info("processId :" + processId);

        log.info("Getting Asks using Service! : " + questionGroupCode + ":" + sourceCode
                + ":" + targetCode);

        BaseEntity source = null;
        BaseEntity target = null;

        source = beUtils.getBaseEntityByCode(sourceCode);
        target = beUtils.getBaseEntityByCode(targetCode);

        QDataAskMessage askMsg = getQDataAskMessage(questionGroupCode, source, target);

        for (Ask ask : askMsg.getItems()) {
            ask.setProcessId(processId);
        }

        return askMsg;
    }

    public void sendPCM(final String pcmCode) {

		QCmdMessage msg = null;
        if (pcmCode.startsWith("PCM")) {
            msg = new QCmdMessage("DISPLAY", "FORM");
        } else {
            String[] displayParms = pcmCode.split(":");
            msg = new QCmdMessage(displayParms[0], displayParms[1]);
        }

		msg.setToken(tokens.getGennyToken().getToken());
		KafkaUtils.writeMsg("webcmds", msg);
    }

    public void sendQuestions(QDataAskMessage askMsg, BaseEntity target) {

		String token = tokens.getGennyToken().getToken();

        QCmdMessage msg = new QCmdMessage("DISPLAY", "FORM");
        msg.setToken(token);
        KafkaUtils.writeMsg("webcmds", msg);

        QDataBaseEntityMessage beMsg = new QDataBaseEntityMessage(target);
        beMsg.setToken(token);
        KafkaUtils.writeMsg("webdata", beMsg);

        askMsg.setToken(token);
        KafkaUtils.writeMsg("webdata", askMsg);
    }
}
